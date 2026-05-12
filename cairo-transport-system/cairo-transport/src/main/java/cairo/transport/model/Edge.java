package cairo.transport.model;

/**
 * Represents a road (edge) between two nodes in Cairo's transportation network.
 */
public class Edge implements Comparable<Edge> {
    private String fromId;
    private String toId;
    private double distance;       // km
    private int capacity;          // vehicles/hour
    private int condition;         // 1-10 (road quality)
    private boolean isExisting;
    private double constructionCost; // Million EGP (0 for existing roads)

    // Traffic flow by period (vehicles/hour)
    private int morningPeak;
    private int afternoon;
    private int eveningPeak;
    private int night;

    // Constructor for existing roads
    public Edge(String fromId, String toId, double distance, int capacity, int condition) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.capacity = capacity;
        this.condition = condition;
        this.isExisting = true;
        this.constructionCost = 0;
    }

    // Constructor for potential new roads
    public Edge(String fromId, String toId, double distance, int capacity, double constructionCost) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.capacity = capacity;
        this.condition = 10; // new road assumed perfect
        this.isExisting = false;
        this.constructionCost = constructionCost;
    }

    /**
     * Returns a time-dependent weight based on traffic period.
     * Period: 0=morning peak, 1=afternoon, 2=evening peak, 3=night
     * Weight = distance * congestion_factor / condition_factor
     */
    public double getWeight(int period) {
        int flow = getFlowForPeriod(period);
        double congestion = (capacity > 0) ? Math.max(1.0, (double) flow / capacity) : 1.0;
        double conditionFactor = Math.max(0.5, condition / 10.0);
        return distance * congestion / conditionFactor;
    }

    public double getWeight() {
        return distance; // default: pure distance
    }

    public int getFlowForPeriod(int period) {
        return switch (period) {
            case 0 -> morningPeak;
            case 1 -> afternoon;
            case 2 -> eveningPeak;
            case 3 -> night;
            default -> afternoon;
        };
    }

    /** Congestion ratio: flow/capacity at given period */
    public double getCongestionRatio(int period) {
        if (capacity == 0) return 0;
        return (double) getFlowForPeriod(period) / capacity;
    }

    public void setTrafficFlow(int morning, int afternoon, int evening, int night) {
        this.morningPeak = morning;
        this.afternoon = afternoon;
        this.eveningPeak = evening;
        this.night = night;
    }

    /** For Kruskal's MST - sort by distance */
    @Override
    public int compareTo(Edge other) {
        return Double.compare(this.distance, other.distance);
    }

    // Getters & Setters
    public String getFromId()          { return fromId; }
    public String getToId()            { return toId; }
    public double getDistance()        { return distance; }
    public int getCapacity()           { return capacity; }
    public int getCondition()          { return condition; }
    public boolean isExisting()        { return isExisting; }
    public double getConstructionCost(){ return constructionCost; }
    public int getMorningPeak()        { return morningPeak; }
    public int getAfternoon()          { return afternoon; }
    public int getEveningPeak()        { return eveningPeak; }
    public int getNight()              { return night; }

    public String getRoadId() {
        return fromId + "-" + toId;
    }

    @Override
    public String toString() {
        return String.format("Road(%s->%s, %.1fkm, cap=%d, cond=%d%s)",
                fromId, toId, distance, capacity, condition,
                isExisting ? "" : String.format(", cost=%.0fM EGP", constructionCost));
    }
}
