package cairo.transport.model;

import java.util.List;

/**
 * Represents a Cairo metro line.
 */
public class MetroLine {
    private String id;
    private String name;
    private List<String> stations; // node IDs
    private int dailyPassengers;
    // DP scheduling fields
    private int assignedTrains;
    private double optimizedFrequencyMin; // minutes between trains

    public MetroLine(String id, String name, List<String> stations, int dailyPassengers) {
        this.id = id;
        this.name = name;
        this.stations = stations;
        this.dailyPassengers = dailyPassengers;
        this.assignedTrains = 0;
        this.optimizedFrequencyMin = 0;
    }

    public String getId()                 { return id; }
    public String getName()               { return name; }
    public List<String> getStations()     { return stations; }
    public int getDailyPassengers()       { return dailyPassengers; }
    public int getAssignedTrains()        { return assignedTrains; }
    public double getOptimizedFrequencyMin() { return optimizedFrequencyMin; }
    public void setAssignedTrains(int t)  { this.assignedTrains = t; }
    public void setOptimizedFrequencyMin(double f) { this.optimizedFrequencyMin = f; }

    @Override
    public String toString() {
        return String.format("Metro[%s] %s - %d stations, %,d daily passengers",
                id, name, stations.size(), dailyPassengers);
    }
}
