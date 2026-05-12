package cairo.transport.model;

import java.util.List;

/**
 * Represents a Cairo bus route.
 */
public class BusRoute {
    private String id;
    private List<String> stops;       // node IDs
    private int busesAssigned;
    private int dailyPassengers;
    // DP scheduling result
    private int optimizedBuses;
    private double coverageScore;

    public BusRoute(String id, List<String> stops, int busesAssigned, int dailyPassengers) {
        this.id = id;
        this.stops = stops;
        this.busesAssigned = busesAssigned;
        this.dailyPassengers = dailyPassengers;
        this.optimizedBuses = busesAssigned;
        this.coverageScore = 0;
    }

    public String getId()                  { return id; }
    public List<String> getStops()         { return stops; }
    public int getBusesAssigned()          { return busesAssigned; }
    public int getDailyPassengers()        { return dailyPassengers; }
    public int getOptimizedBuses()         { return optimizedBuses; }
    public double getCoverageScore()       { return coverageScore; }
    public void setOptimizedBuses(int b)   { this.optimizedBuses = b; }
    public void setCoverageScore(double s) { this.coverageScore = s; }

    /** Passengers per bus (efficiency metric) */
    public double getEfficiency() {
        return busesAssigned > 0 ? (double) dailyPassengers / busesAssigned : 0;
    }

    @Override
    public String toString() {
        return String.format("Bus[%s] %d stops, %d buses, %,d daily passengers (%.0f p/bus)",
                id, stops.size(), busesAssigned, dailyPassengers, getEfficiency());
    }
}
