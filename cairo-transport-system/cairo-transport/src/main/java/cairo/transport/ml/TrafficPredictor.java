package cairo.transport.ml;

import cairo.transport.model.*;

import java.util.*;

/**
 * ML-based Traffic Prediction Module (Bonus Component).
 *
 * Implements a simple Linear Regression model trained on the provided
 * temporal traffic data (morning peak, afternoon, evening peak, night).
 *
 * Model: flow(t) = w0 + w1*hour + w2*dayType
 * Trained using gradient descent on the provided traffic patterns.
 *
 * For each road, we train a separate lightweight model.
 * This predicts congestion at arbitrary hour of day.
 *
 * NOTE: For production bonus, this pairs with scikit-learn/TensorFlow via
 * REST API. Here we implement pure Java linear regression to keep zero dependencies.
 */
public class TrafficPredictor {

    private TransportGraph graph;

    // Model weights per road: roadId -> [w0 (intercept), w1 (hour coeff), w2 (peak flag coeff)]
    private Map<String, double[]> models = new HashMap<>();
    private boolean trained = false;

    // Traffic periods mapped to representative hours
    private static final double[] PERIOD_HOURS  = {8.0, 13.0, 18.0, 23.0};
    private static final int[] PERIOD_PEAK_FLAG = {1, 0, 1, 0}; // 1=peak hour

    public TrafficPredictor(TransportGraph graph) {
        this.graph = graph;
    }

    /**
     * Train linear regression model for each road using the 4 temporal data points.
     * Uses closed-form OLS (ordinary least squares): w = (X^T X)^(-1) X^T y
     *
     * Feature vector per sample: [1, hour, isPeak]
     * Target: traffic flow (vehicles/hour)
     */
    public void train() {
        for (Edge edge : graph.getAllEdges()) {
            // Training data: 4 observations per road
            double[] flows = {
                    edge.getMorningPeak(),
                    edge.getAfternoon(),
                    edge.getEveningPeak(),
                    edge.getNight()
            };

            // Build design matrix X (4x3): [1, hour, isPeak]
            double[][] X = new double[4][3];
            for (int i = 0; i < 4; i++) {
                X[i][0] = 1.0;
                X[i][1] = PERIOD_HOURS[i];
                X[i][2] = PERIOD_PEAK_FLAG[i];
            }

            // OLS: w = (X^T X)^-1 X^T y  (3x3 system, solvable analytically)
            double[] w = olsSolve(X, flows);
            models.put(edge.getRoadId(), w);
        }
        trained = true;
        System.out.println("[ML] Traffic prediction models trained for " + models.size() + " roads.");
    }

    /**
     * Predict traffic flow on a road at a given hour of day.
     * @param roadId   "fromId-toId"
     * @param hour     hour of day (0-23)
     * @return predicted flow in vehicles/hour
     */
    public double predict(String roadId, double hour) {
        if (!trained) train();
        double[] w = models.get(roadId);
        if (w == null) return -1;
        boolean isPeak = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 20);
        double prediction = w[0] + w[1] * hour + w[2] * (isPeak ? 1.0 : 0.0);
        return Math.max(0, prediction); // flow can't be negative
    }

    /**
     * Predict congestion ratio at given hour.
     */
    public double predictCongestionRatio(Edge edge, double hour) {
        double predictedFlow = predict(edge.getRoadId(), hour);
        if (edge.getCapacity() == 0) return 0;
        return Math.min(1.5, predictedFlow / edge.getCapacity());
    }

    /**
     * Forecast congestion for all roads at a given hour.
     * Returns roads sorted by predicted congestion.
     */
    public List<String> forecastTopCongestion(double hour, int topN) {
        List<String> results = new ArrayList<>();
        List<Edge> edges = new ArrayList<>(graph.getAllEdges());
        edges.sort((a, b) -> Double.compare(
                predictCongestionRatio(b, hour),
                predictCongestionRatio(a, hour)));

        for (int i = 0; i < Math.min(topN, edges.size()); i++) {
            Edge e = edges.get(i);
            Node from = graph.getNode(e.getFromId());
            Node to   = graph.getNode(e.getToId());
            double ratio = predictCongestionRatio(e, hour);
            results.add(String.format("%-30s → %-30s: %.0f%% capacity (%.0f veh/h predicted)",
                    from != null ? from.getName() : e.getFromId(),
                    to   != null ? to.getName()   : e.getToId(),
                    ratio * 100,
                    predict(e.getRoadId(), hour)));
        }
        return results;
    }

    /**
     * Compute model accuracy (MAE - Mean Absolute Error) for each road.
     * Returned as overall average MAE across all roads.
     */
    public double computeMAE() {
        if (!trained) train();
        double totalError = 0;
        int count = 0;
        for (Edge edge : graph.getAllEdges()) {
            double[] actuals  = {edge.getMorningPeak(), edge.getAfternoon(),
                                 edge.getEveningPeak(), edge.getNight()};
            for (int i = 0; i < 4; i++) {
                double pred = predict(edge.getRoadId(), PERIOD_HOURS[i]);
                totalError += Math.abs(pred - actuals[i]);
                count++;
            }
        }
        return totalError / count;
    }

    // =====================================================================
    // OLS Solver (3x3 system via Cramer's rule / direct inversion)
    // =====================================================================

    /**
     * Solve OLS: w = (X^T X)^-1 X^T y for 3-feature design matrix X (Nx3).
     */
    private double[] olsSolve(double[][] X, double[] y) {
        int n = X.length;
        int p = X[0].length; // 3

        // Compute X^T X (3x3)
        double[][] XtX = new double[p][p];
        for (int i = 0; i < p; i++)
            for (int j = 0; j < p; j++)
                for (int k = 0; k < n; k++)
                    XtX[i][j] += X[k][i] * X[k][j];

        // Compute X^T y (3x1)
        double[] Xty = new double[p];
        for (int i = 0; i < p; i++)
            for (int k = 0; k < n; k++)
                Xty[i] += X[k][i] * y[k];

        // Solve 3x3 system via Gaussian elimination
        return gaussianElimination(XtX, Xty);
    }

    private double[] gaussianElimination(double[][] A, double[] b) {
        int n = b.length;
        // Augmented matrix
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) aug[i][j] = A[i][j];
            aug[i][n] = b[i];
        }
        // Forward elimination
        for (int col = 0; col < n; col++) {
            // Pivot
            int maxRow = col;
            for (int row = col + 1; row < n; row++)
                if (Math.abs(aug[row][col]) > Math.abs(aug[maxRow][col])) maxRow = row;
            double[] tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp;

            if (Math.abs(aug[col][col]) < 1e-10) continue; // singular

            for (int row = col + 1; row < n; row++) {
                double factor = aug[row][col] / aug[col][col];
                for (int j = col; j <= n; j++)
                    aug[row][j] -= factor * aug[col][j];
            }
        }
        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = aug[i][n];
            for (int j = i + 1; j < n; j++) x[i] -= aug[i][j] * x[j];
            if (Math.abs(aug[i][i]) > 1e-10) x[i] /= aug[i][i];
        }
        return x;
    }

    public void printPredictions(double hour) {
        System.out.printf("%n========== ML TRAFFIC PREDICTION (Hour %.0f:00) ==========%n", hour);
        System.out.println("Top 5 predicted congestion hotspots:");
        forecastTopCongestion(hour, 5).forEach(s -> System.out.println("  " + s));
        System.out.printf("Model MAE: %.0f vehicles/hour%n", computeMAE());
    }
}
