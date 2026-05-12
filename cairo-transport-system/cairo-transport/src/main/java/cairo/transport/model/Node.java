package cairo.transport.model;

/**
 * Represents a node (neighborhood, district, or facility) in Cairo's transportation network.
 */
public class Node {
    private String id;
    private String name;
    private String type;       // Residential, Mixed, Business, Industrial, Government, Airport, etc.
    private double x;          // longitude
    private double y;          // latitude
    private int population;    // 0 for facilities

    public Node(String id, String name, String type, double x, double y, int population) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.population = population;
    }

    // Haversine distance in km to another node (approximated via Euclidean on small scale)
    public double distanceTo(Node other) {
        double dx = (this.x - other.x) * 111.32 * Math.cos(Math.toRadians((this.y + other.y) / 2));
        double dy = (this.y - other.y) * 110.574;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isCriticalFacility() {
        return type.equalsIgnoreCase("Medical") || type.equalsIgnoreCase("Airport")
                || type.equalsIgnoreCase("Transit Hub") || type.equalsIgnoreCase("Government");
    }

    // Getters
    public String getId()       { return id; }
    public String getName()     { return name; }
    public String getType()     { return type; }
    public double getX()        { return x; }
    public double getY()        { return y; }
    public int getPopulation()  { return population; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", id, name, type);
    }
}
