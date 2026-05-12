package cairo.transport.utils;

import cairo.transport.model.*;
import java.util.*;

/**
 * Loads all project-provided data into the TransportGraph and related structures.
 * Data is hardcoded from the CSE112 Project Provided Data document.
 */
public class DataLoader {

    public static TransportGraph loadGraph() {
        TransportGraph graph = new TransportGraph();
        loadNodes(graph);
        loadExistingRoads(graph);
        loadPotentialRoads(graph);
        loadTrafficFlow(graph);
        return graph;
    }

    private static void loadNodes(TransportGraph graph) {
        // Neighborhoods & Districts: (ID, Name, Population, Type, X(lon), Y(lat))
        graph.addNode(new Node("1",  "Maadi",                       "Residential", 31.25, 29.96, 250000));
        graph.addNode(new Node("2",  "Nasr City",                   "Mixed",       31.34, 30.06, 500000));
        graph.addNode(new Node("3",  "Downtown Cairo",              "Business",    31.24, 30.04, 100000));
        graph.addNode(new Node("4",  "New Cairo",                   "Residential", 31.47, 30.03, 300000));
        graph.addNode(new Node("5",  "Heliopolis",                  "Mixed",       31.32, 30.09, 200000));
        graph.addNode(new Node("6",  "Zamalek",                     "Residential", 31.22, 30.06,  50000));
        graph.addNode(new Node("7",  "6th October City",            "Mixed",       30.98, 29.93, 400000));
        graph.addNode(new Node("8",  "Giza",                        "Mixed",       31.21, 29.99, 550000));
        graph.addNode(new Node("9",  "Mohandessin",                 "Business",    31.20, 30.05, 180000));
        graph.addNode(new Node("10", "Dokki",                       "Mixed",       31.21, 30.03, 220000));
        graph.addNode(new Node("11", "Shubra",                      "Residential", 31.24, 30.11, 450000));
        graph.addNode(new Node("12", "Helwan",                      "Industrial",  31.33, 29.85, 350000));
        graph.addNode(new Node("13", "New Administrative Capital",  "Government",  31.80, 30.02,  50000));
        graph.addNode(new Node("14", "Al Rehab",                    "Residential", 31.49, 30.06, 120000));
        graph.addNode(new Node("15", "Sheikh Zayed",                "Residential", 30.94, 30.01, 150000));

        // Important Facilities: (ID, Name, Type, X, Y)
        graph.addNode(new Node("F1",  "Cairo International Airport",  "Airport",     31.41, 30.11, 0));
        graph.addNode(new Node("F2",  "Ramses Railway Station",       "Transit Hub", 31.25, 30.06, 0));
        graph.addNode(new Node("F3",  "Cairo University",             "Education",   31.21, 30.03, 0));
        graph.addNode(new Node("F4",  "Al-Azhar University",          "Education",   31.26, 30.05, 0));
        graph.addNode(new Node("F5",  "Egyptian Museum",              "Tourism",     31.23, 30.05, 0));
        graph.addNode(new Node("F6",  "Cairo International Stadium",  "Sports",      31.30, 30.07, 0));
        graph.addNode(new Node("F7",  "Smart Village",                "Business",    30.97, 30.07, 0));
        graph.addNode(new Node("F8",  "Cairo Festival City",          "Commercial",  31.40, 30.03, 0));
        graph.addNode(new Node("F9",  "Qasr El Aini Hospital",        "Medical",     31.23, 30.03, 0));
        graph.addNode(new Node("F10", "Maadi Military Hospital",      "Medical",     31.25, 29.95, 0));
    }

    private static void loadExistingRoads(TransportGraph graph) {
        // (FromID, ToID, Distance(km), Capacity(veh/h), Condition(1-10))
        graph.addEdge(new Edge("1",  "3",  8.5,  3000, 7));
        graph.addEdge(new Edge("1",  "8",  6.2,  2500, 6));
        graph.addEdge(new Edge("2",  "3",  5.9,  2800, 8));
        graph.addEdge(new Edge("2",  "5",  4.0,  3200, 9));
        graph.addEdge(new Edge("3",  "5",  6.1,  3500, 7));
        graph.addEdge(new Edge("3",  "6",  3.2,  2000, 8));
        graph.addEdge(new Edge("3",  "9",  4.5,  2600, 6));
        graph.addEdge(new Edge("3",  "10", 3.8,  2400, 7));
        graph.addEdge(new Edge("4",  "2",  15.2, 3800, 9));
        graph.addEdge(new Edge("4",  "14", 5.3,  3000, 10));
        graph.addEdge(new Edge("5",  "11", 7.9,  3100, 7));
        graph.addEdge(new Edge("6",  "9",  2.2,  1800, 8));
        graph.addEdge(new Edge("7",  "8",  24.5, 3500, 8));
        graph.addEdge(new Edge("7",  "15", 9.8,  3000, 9));
        graph.addEdge(new Edge("8",  "10", 3.3,  2200, 7));
        graph.addEdge(new Edge("8",  "12", 14.8, 2600, 5));
        graph.addEdge(new Edge("9",  "10", 2.1,  1900, 7));
        graph.addEdge(new Edge("10", "11", 8.7,  2400, 6));
        graph.addEdge(new Edge("11", "F2", 3.6,  2200, 7));
        graph.addEdge(new Edge("12", "1",  12.7, 2800, 6));
        graph.addEdge(new Edge("13", "4",  45.0, 4000, 10));
        graph.addEdge(new Edge("14", "13", 35.5, 3800, 9));
        graph.addEdge(new Edge("15", "7",  9.8,  3000, 9));
        graph.addEdge(new Edge("F1", "5",  7.5,  3500, 9));
        graph.addEdge(new Edge("F1", "2",  9.2,  3200, 8));
        graph.addEdge(new Edge("F2", "3",  2.5,  2000, 7));
        graph.addEdge(new Edge("F7", "15", 8.3,  2800, 8));
        graph.addEdge(new Edge("F8", "4",  6.1,  3000, 9));

        // Hospital & facility connections (required for emergency routing)
        graph.addEdge(new Edge("F9",  "3",  1.5,  2000, 8));  // Qasr El Aini ↔ Downtown
        graph.addEdge(new Edge("F9",  "10", 2.0,  1800, 7));  // Qasr El Aini ↔ Dokki
        graph.addEdge(new Edge("F10", "1",  2.3,  2200, 7));  // Maadi Military ↔ Maadi
        graph.addEdge(new Edge("F10", "12", 10.5, 2000, 6));  // Maadi Military ↔ Helwan

        // Other facility connections
        graph.addEdge(new Edge("F3",  "10", 0.5,  1500, 8));  // Cairo University ↔ Dokki
        graph.addEdge(new Edge("F4",  "3",  2.1,  1800, 8));  // Al-Azhar ↔ Downtown
        graph.addEdge(new Edge("F5",  "3",  1.2,  1600, 8));  // Egyptian Museum ↔ Downtown
        graph.addEdge(new Edge("F6",  "5",  3.5,  2000, 8));  // Stadium ↔ Heliopolis
    }

    private static void loadPotentialRoads(TransportGraph graph) {
        // (FromID, ToID, Distance(km), Capacity(veh/h), CostMillionEGP)
        graph.addPotentialEdge(new Edge("1",  "4",  22.8, 4000, 450));
        graph.addPotentialEdge(new Edge("1",  "14", 25.3, 3800, 500));
        graph.addPotentialEdge(new Edge("2",  "13", 48.2, 4500, 950));
        graph.addPotentialEdge(new Edge("3",  "13", 56.7, 4500, 1100));
        graph.addPotentialEdge(new Edge("5",  "4",  16.8, 3500, 320));
        graph.addPotentialEdge(new Edge("6",  "8",  7.5,  2500, 150));
        graph.addPotentialEdge(new Edge("7",  "13", 82.3, 4000, 1600));
        graph.addPotentialEdge(new Edge("9",  "11", 6.9,  2800, 140));
        graph.addPotentialEdge(new Edge("10", "F7", 27.4, 3200, 550));
        graph.addPotentialEdge(new Edge("11", "13", 62.1, 4200, 1250));
        graph.addPotentialEdge(new Edge("12", "14", 30.5, 3600, 610));
        graph.addPotentialEdge(new Edge("14", "5",  18.2, 3300, 360));
        graph.addPotentialEdge(new Edge("15", "9",  22.7, 3000, 450));
        graph.addPotentialEdge(new Edge("F1", "13", 40.2, 4000, 800));
        graph.addPotentialEdge(new Edge("F7", "9",  26.8, 3200, 540));
    }

    private static void loadTrafficFlow(TransportGraph graph) {
        // (RoadID, MorningPeak, Afternoon, EveningPeak, Night)
        graph.setTrafficFlow("1",  "3",  2800, 1500, 2600, 800);
        graph.setTrafficFlow("1",  "8",  2200, 1200, 2100, 600);
        graph.setTrafficFlow("2",  "3",  2700, 1400, 2500, 700);
        graph.setTrafficFlow("2",  "5",  3000, 1600, 2800, 650);
        graph.setTrafficFlow("3",  "5",  3200, 1700, 3100, 800);
        graph.setTrafficFlow("3",  "6",  1800, 1400, 1900, 500);
        graph.setTrafficFlow("3",  "9",  2400, 1300, 2200, 550);
        graph.setTrafficFlow("3",  "10", 2300, 1200, 2100, 500);
        graph.setTrafficFlow("4",  "2",  3600, 1800, 3300, 750);
        graph.setTrafficFlow("4",  "14", 2800, 1600, 2600, 600);
        graph.setTrafficFlow("5",  "11", 2900, 1500, 2700, 650);
        graph.setTrafficFlow("6",  "9",  1700, 1300, 1800, 450);
        graph.setTrafficFlow("7",  "8",  3200, 1700, 3000, 700);
        graph.setTrafficFlow("7",  "15", 2800, 1500, 2600, 600);
        graph.setTrafficFlow("8",  "10", 2000, 1100, 1900, 450);
        graph.setTrafficFlow("8",  "12", 2400, 1300, 2200, 500);
        graph.setTrafficFlow("9",  "10", 1800, 1200, 1700, 400);
        graph.setTrafficFlow("10", "11", 2200, 1300, 2100, 500);
        graph.setTrafficFlow("11", "F2", 2100, 1200, 2000, 450);
        graph.setTrafficFlow("12", "1",  2600, 1400, 2400, 550);
        graph.setTrafficFlow("13", "4",  3800, 2000, 3500, 800);
        graph.setTrafficFlow("14", "13", 3600, 1900, 3300, 750);
        graph.setTrafficFlow("15", "7",  2800, 1500, 2600, 600);
        graph.setTrafficFlow("F1", "5",  3300, 2200, 3100, 1200);
        graph.setTrafficFlow("F1", "2",  3000, 2000, 2800, 1100);
        graph.setTrafficFlow("F2", "3",  1900, 1600, 1800, 900);
        graph.setTrafficFlow("F7", "15", 2600, 1500, 2400, 550);
        graph.setTrafficFlow("F8", "4",  2800, 1600, 2600, 600);

        // Hospital & facility traffic flows
        graph.setTrafficFlow("F9",  "3",  1600, 1200, 1800, 600);
        graph.setTrafficFlow("F9",  "10", 1400, 1000, 1500, 500);
        graph.setTrafficFlow("F10", "1",  1200,  900, 1300, 400);
        graph.setTrafficFlow("F10", "12",  900,  700, 1000, 300);
        graph.setTrafficFlow("F3",  "10",  800,  900,  850, 300);
        graph.setTrafficFlow("F4",  "3",  1500, 1200, 1600, 500);
        graph.setTrafficFlow("F5",  "3",   900,  800, 1000, 400);
        graph.setTrafficFlow("F6",  "5",  1800, 1400, 2000, 500);
    }

    public static List<MetroLine> loadMetroLines() {
        List<MetroLine> lines = new ArrayList<>();
        lines.add(new MetroLine("M1", "Line 1 (Helwan-New Marg)",
                Arrays.asList("12", "1", "3", "F2", "11"), 1500000));
        lines.add(new MetroLine("M2", "Line 2 (Shubra-Giza)",
                Arrays.asList("11", "F2", "3", "10", "8"), 1200000));
        lines.add(new MetroLine("M3", "Line 3 (Airport-Imbaba)",
                Arrays.asList("F1", "5", "2", "3", "9"), 800000));
        return lines;
    }

    public static List<BusRoute> loadBusRoutes() {
        List<BusRoute> routes = new ArrayList<>();
        routes.add(new BusRoute("B1",  Arrays.asList("1","3","6","9"),       25, 35000));
        routes.add(new BusRoute("B2",  Arrays.asList("7","15","8","10","3"), 30, 42000));
        routes.add(new BusRoute("B3",  Arrays.asList("2","5","F1"),          20, 28000));
        routes.add(new BusRoute("B4",  Arrays.asList("4","14","2","3"),      22, 31000));
        routes.add(new BusRoute("B5",  Arrays.asList("8","12","1"),          18, 25000));
        routes.add(new BusRoute("B6",  Arrays.asList("11","5","2"),          24, 33000));
        routes.add(new BusRoute("B7",  Arrays.asList("13","4","14"),         15, 21000));
        routes.add(new BusRoute("B8",  Arrays.asList("F7","15","7"),         12, 17000));
        routes.add(new BusRoute("B9",  Arrays.asList("1","8","10","9","6"),  28, 39000));
        routes.add(new BusRoute("B10", Arrays.asList("F8","4","2","5"),      20, 28000));
        return routes;
    }

    // Public transportation demand: (FromID, ToID, DailyPassengers)
    public static Map<String, Integer> loadTransportDemand() {
        Map<String, Integer> demand = new LinkedHashMap<>();
        demand.put("3-5",   15000);
        demand.put("1-3",   12000);
        demand.put("2-3",   18000);
        demand.put("F2-11", 25000);
        demand.put("F1-3",  20000);
        demand.put("7-3",   14000);
        demand.put("4-3",   16000);
        demand.put("8-3",   22000);
        demand.put("3-9",   13000);
        demand.put("5-2",   17000);
        demand.put("11-3",  24000);
        demand.put("12-3",  11000);
        demand.put("1-8",    9000);
        demand.put("7-F7",  18000);
        demand.put("4-F8",  12000);
        demand.put("13-3",   8000);
        demand.put("14-4",   7000);
        return demand;
    }
}
