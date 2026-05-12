package cairo.transport.algorithms;

import cairo.transport.model.*;

import java.util.*;

/**
 * Dynamic Programming Solutions for Cairo Public Transportation.
 *
 * Two DP problems:
 *
 * 1. VEHICLE SCHEDULING (Unbounded Knapsack variant):
 *    Problem: Allocate a total fleet of N buses/trains across routes/lines
 *             to maximize total daily passenger coverage.
 *    State:   dp[i][j] = max passengers served using first i routes with j vehicles
 *    Transition: dp[i][j] = max(dp[i-1][j], dp[i][j-1] + marginalPassengers(i))
 *    Time:  O(R × V) where R = number of routes, V = total vehicles
 *    Space: O(R × V) — can optimize to O(V) with rolling array
 *
 * 2. ROAD MAINTENANCE (0/1 Knapsack):
 *    Problem: Select roads for maintenance given a budget B to maximize
 *             total improvement (capacity × condition_improvement).
 *    State:   dp[i][b] = max improvement using first i roads with budget b
 *    Transition: dp[i][b] = max(dp[i-1][b], dp[i-1][b-cost[i]] + value[i])
 *    Time:  O(E × B)
 *    Space: O(E × B) — optimized to O(B) with 1D array
 *
 * 3. MEMOIZED ROUTE PLANNING (top-down DP):
 *    Wraps Dijkstra results with LRU-style memoization.
 */
public class DynamicProgramming {

    private TransportGraph graph;
    // Memoization table for route planning: (source,target,period) -> (distance, path)
    private Map<String, double[]> routeMemo = new HashMap<>();

    public DynamicProgramming(TransportGraph graph) {
        this.graph = graph;
    }

    // =====================================================================
    // 1. VEHICLE SCHEDULING - Maximize passenger coverage
    // =====================================================================

    public static class SchedulingResult {
        public int[] vehiclesPerRoute;    // allocated vehicles for each route
        public double totalPassengers;    // total passengers served
        public List<String> routeIds;
        public List<Double> frequencies; // departures per hour per route

        public SchedulingResult(int routes) {
            vehiclesPerRoute = new int[routes];
            frequencies = new ArrayList<>();
        }
    }

    /**
     * Allocate totalVehicles buses across bus routes to maximize daily passengers.
     * Uses 2D DP (unbounded knapsack variant - each route can get multiple vehicles).
     *
     * @param routes       list of bus routes
     * @param totalVehicles total available buses
     * @return allocation result
     */
    public SchedulingResult optimizeBusScheduling(List<BusRoute> routes, int totalVehicles) {
        int R = routes.size();
        int V = totalVehicles;

        // Marginal passenger gain per extra bus on route i (diminishing returns model)
        // We model: passengers(buses) = dailyPass * (1 - e^(-buses/10))
        // Marginal gain for going from k to k+1 buses ≈ dailyPass * 0.1 * e^(-k/10)

        // dp[j] = max passengers if we allocate exactly j vehicles to first i routes (1D rolling)
        double[] dp = new double[V + 1];
        int[][] alloc = new int[R][V + 1]; // backtracking

        for (int i = 0; i < R; i++) {
            BusRoute route = routes.get(i);
            double[] newDp = new double[V + 1];
            for (int j = 0; j <= V; j++) {
                newDp[j] = dp[j]; // don't assign any extra to this route
                alloc[i][j] = 0;
            }
            for (int extra = 1; extra <= V; extra++) {
                double marginal = marginalPassengerGain(route, route.getBusesAssigned(), extra);
                for (int j = extra; j <= V; j++) {
                    double candidate = dp[j - extra] + marginal;
                    if (candidate > newDp[j]) {
                        newDp[j] = candidate;
                        alloc[i][j] = extra;
                    }
                }
            }
            dp = newDp;
        }

        // Backtrack to find allocations
        SchedulingResult result = new SchedulingResult(R);
        result.routeIds = new ArrayList<>();
        int remaining = V;
        for (int i = R - 1; i >= 0 && remaining >= 0; i--) {
            int extra = alloc[i][remaining];
            result.vehiclesPerRoute[i] = routes.get(i).getBusesAssigned() + extra;
            remaining -= extra;
            result.routeIds.add(routes.get(i).getId());
        }
        result.totalPassengers = dp[V];

        // Compute frequencies (buses per hour assuming 16hr operational day)
        for (int i = 0; i < R; i++) {
            double busesPerHour = result.vehiclesPerRoute[i] / 16.0;
            double freqMin = busesPerHour > 0 ? 60.0 / busesPerHour : 60;
            result.frequencies.add(freqMin);
            routes.get(i).setOptimizedBuses(result.vehiclesPerRoute[i]);
        }

        return result;
    }

    /** Marginal passenger gain: extra buses reduce wait time, attracting more riders */
    private double marginalPassengerGain(BusRoute route, int currentBuses, int extraBuses) {
        double base = route.getDailyPassengers();
        // Capacity model: adding buses improves frequency and thus ridership
        // Gain = base * (sqrt(current+extra) - sqrt(current)) / sqrt(current)
        double before = Math.sqrt(currentBuses);
        double after  = Math.sqrt(currentBuses + extraBuses);
        return base * (after - before) / Math.max(1, before);
    }

    /**
     * Optimize metro train schedules across all lines.
     */
    public SchedulingResult optimizeMetroScheduling(List<MetroLine> lines, int totalTrains) {
        int R = lines.size();
        double[] dp = new double[totalTrains + 1];
        int[][] alloc = new int[R][totalTrains + 1];

        for (int i = 0; i < R; i++) {
            MetroLine line = lines.get(i);
            double[] newDp = Arrays.copyOf(dp, totalTrains + 1);

            for (int extra = 1; extra <= totalTrains; extra++) {
                double marginal = line.getDailyPassengers() * 0.05 * extra; // ~5% gain per train
                for (int j = extra; j <= totalTrains; j++) {
                    double candidate = dp[j - extra] + marginal;
                    if (candidate > newDp[j]) {
                        newDp[j] = candidate;
                        alloc[i][j] = extra;
                    }
                }
            }
            dp = newDp;
        }

        SchedulingResult result = new SchedulingResult(R);
        result.routeIds = new ArrayList<>();
        int remaining = totalTrains;
        for (int i = R - 1; i >= 0 && remaining >= 0; i--) {
            int extra = alloc[i][remaining];
            int baseTrains = 10; // baseline trains
            result.vehiclesPerRoute[i] = baseTrains + extra;
            remaining -= extra;
            result.routeIds.add(lines.get(i).getId());
            double freqMin = result.vehiclesPerRoute[i] > 0 ? 60.0 / result.vehiclesPerRoute[i] : 10;
            result.frequencies.add(freqMin);
            lines.get(i).setAssignedTrains(result.vehiclesPerRoute[i]);
            lines.get(i).setOptimizedFrequencyMin(freqMin);
        }
        result.totalPassengers = dp[totalTrains];
        return result;
    }

    // =====================================================================
    // 2. ROAD MAINTENANCE - 0/1 Knapsack budget optimization
    // =====================================================================

    public static class MaintenanceResult {
        public List<Edge> selectedRoads = new ArrayList<>();
        public double totalCost;
        public double totalImprovement; // weighted score
        public List<String> explanation = new ArrayList<>();
    }

    /**
     * Select roads for maintenance given a budget to maximize network improvement.
     * Improvement = capacity_gain * current_traffic_volume / condition_factor
     *
     * @param budgetMillionEGP  total maintenance budget
     * @return selected roads for maintenance
     */
    public MaintenanceResult optimizeRoadMaintenance(double budgetMillionEGP) {
        List<Edge> candidates = graph.getAllEdges().stream()
                .filter(e -> e.getCondition() < 8) // only roads that need maintenance
                .collect(java.util.stream.Collectors.toList());

        int N = candidates.size();
        int B = (int)(budgetMillionEGP * 10); // scale to integer (0.1M units)

        // Cost and value for each road
        int[] cost  = new int[N];
        double[] value = new double[N];
        for (int i = 0; i < N; i++) {
            Edge e = candidates.get(i);
            // Maintenance cost estimate: proportional to distance and inverse of condition
            double maintenanceCost = e.getDistance() * (10 - e.getCondition()) * 2.0; // M EGP
            cost[i] = Math.max(1, (int)(maintenanceCost * 10));
            // Value = improvement in effective capacity after maintenance
            double condImprovement = (10 - e.getCondition()) / 10.0;
            value[i] = e.getCapacity() * condImprovement * e.getDistance() / 100.0;
        }

        // 0/1 Knapsack DP (1D array, space optimized)
        double[] dp = new double[B + 1];
        for (int i = 0; i < N; i++) {
            // Traverse right to left to avoid using same item twice
            for (int b = B; b >= cost[i]; b--) {
                dp[b] = Math.max(dp[b], dp[b - cost[i]] + value[i]);
            }
        }

        // Backtrack to find selected roads
        MaintenanceResult result = new MaintenanceResult();
        result.totalImprovement = dp[B];
        int b = B;
        for (int i = N - 1; i >= 0 && b >= 0; i--) {
            if (b >= cost[i] && dp[b] == dp[b - cost[i]] + value[i]) {
                result.selectedRoads.add(candidates.get(i));
                result.totalCost += (double)cost[i] / 10.0;
                b -= cost[i];
                Edge e = candidates.get(i);
                Node from = graph.getNode(e.getFromId());
                Node to   = graph.getNode(e.getToId());
                result.explanation.add(String.format(
                        "%s → %s (condition %d/10, est. cost %.1fM EGP, value %.1f)",
                        from != null ? from.getName() : e.getFromId(),
                        to   != null ? to.getName()   : e.getToId(),
                        e.getCondition(), (double)cost[i]/10.0, value[i]));
            }
        }
        return result;
    }

    // =====================================================================
    // 3. MEMOIZED ROUTE LOOKUP
    // =====================================================================

    /**
     * Memoized shortest distance lookup - avoids re-running Dijkstra for known queries.
     * Returns cached result if available, else computes via Dijkstra.
     */
    public double getMemoizedDistance(String sourceId, String targetId, int period,
                                       DijkstraShortestPath dijkstra) {
        String key = sourceId + ":" + targetId + ":" + period;
        if (routeMemo.containsKey(key)) {
            return routeMemo.get(key)[0]; // cache hit
        }
        Map<String, Double> dist = dijkstra.run(sourceId, period);
        double d = dist.getOrDefault(targetId, Double.MAX_VALUE);
        routeMemo.put(key, new double[]{d});
        return d;
    }

    public int getMemoSize() { return routeMemo.size(); }

    // =====================================================================
    // PRINT RESULTS
    // =====================================================================

    public void printSchedulingResults(SchedulingResult result, List<BusRoute> routes) {
        System.out.println("\n========== DP BUS SCHEDULING RESULTS ==========");
        System.out.printf("Optimized total passengers/day: %,.0f%n", result.totalPassengers);
        System.out.println("Route allocations:");
        for (int i = 0; i < routes.size(); i++) {
            BusRoute r = routes.get(i);
            System.out.printf("  %-4s | Before: %2d buses | After: %2d buses | Freq: %.1f min%n",
                    r.getId(), r.getBusesAssigned(),
                    result.vehiclesPerRoute[i], result.frequencies.get(i));
        }
    }

    public void printMaintenanceResults(MaintenanceResult result) {
        System.out.println("\n========== DP ROAD MAINTENANCE RESULTS ==========");
        System.out.printf("Total maintenance cost: %.1f Million EGP%n", result.totalCost);
        System.out.printf("Total improvement score: %.1f%n", result.totalImprovement);
        System.out.println("Selected roads:");
        result.explanation.forEach(s -> System.out.println("  - " + s));
    }
}
