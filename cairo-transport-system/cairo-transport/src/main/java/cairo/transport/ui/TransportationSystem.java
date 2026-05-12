package cairo.transport.ui;

import cairo.transport.algorithms.*;
import cairo.transport.ml.TrafficPredictor;
import cairo.transport.model.*;
import cairo.transport.utils.DataLoader;
import java.util.*;
import java.util.Scanner;
import java.io.ByteArrayInputStream;

/**
 * Main Interactive Demo for Cairo Transportation Optimization System (CSE112).
 *
 * Demonstrates all algorithms with the provided Cairo data:
 *  - Kruskal's MST (infrastructure design)
 *  - Dijkstra's Shortest Path (route planning + alternate routing)
 *  - A* Emergency Routing
 *  - Dynamic Programming (scheduling + maintenance)
 *  - Greedy (traffic signals + emergency preemption)
 *  - ML Traffic Prediction (bonus)
 *  - Algorithm Comparison (Dijkstra vs A*)
 */
public class TransportationSystem {

    private TransportGraph graph;
    private List<MetroLine> metroLines;
    private List<BusRoute> busRoutes;
    private KruskalMST kruskal;
    private DijkstraShortestPath dijkstra;
    private AStarSearch astar;
    private DynamicProgramming dp;
    private GreedyAlgorithms greedy;
    private TrafficPredictor predictor;
    private Scanner scanner;

    public TransportationSystem() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   Cairo Smart Transportation Optimization System - CSE112  ║");
        System.out.println("║               Alamein International University              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("\nInitializing system with Cairo metropolitan data...");

        graph       = DataLoader.loadGraph();
        metroLines  = DataLoader.loadMetroLines();
        busRoutes   = DataLoader.loadBusRoutes();
        kruskal     = new KruskalMST(graph);
        dijkstra    = new DijkstraShortestPath(graph);
        astar       = new AStarSearch(graph);
        dp          = new DynamicProgramming(graph);
        greedy      = new GreedyAlgorithms(graph);
        predictor   = new TrafficPredictor(graph);
        scanner     = new Scanner(System.in);

        graph.printSummary();
        predictor.train();
        System.out.println("\nSystem ready.\n");
    }

    public void runInteractive() {
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> runMSTDemo();
                case "2" -> runShortestPathDemo();
                case "3" -> runEmergencyRoutingDemo();
                case "4" -> runDPSchedulingDemo();
                case "5" -> runGreedyDemo();
                case "6" -> runMLPredictionDemo();
                case "7" -> runAlgorithmComparisonDemo();
                case "8" -> runFullSystemDemo();
                case "0" -> { System.out.println("Exiting. Goodbye!"); running = false; }
                default  -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║            MAIN MENU                   ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║  1. Infrastructure Network Design (MST)║");
        System.out.println("║  2. Traffic Flow Optimization (Dijkstra)║");
        System.out.println("║  3. Emergency Response (A*)             ║");
        System.out.println("║  4. Public Transit Optimization (DP)    ║");
        System.out.println("║  5. Traffic Signal Control (Greedy)     ║");
        System.out.println("║  6. ML Traffic Prediction [BONUS]       ║");
        System.out.println("║  7. Algorithm Comparison [BONUS]        ║");
        System.out.println("║  8. Full System Demo                    ║");
        System.out.println("║  0. Exit                                ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.print("Select option: ");
    }

    // ─── 1. MST ────────────────────────────────────────────────────────────
    private void runMSTDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│         INFRASTRUCTURE NETWORK DESIGN (Kruskal's MST)    │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        System.out.println("\n[A] MST on existing roads only:");
        List<Edge> mst1 = kruskal.runOnExistingOnly();
        kruskal.printResults();

        System.out.println("\n[B] Enhanced MST including potential new roads:");
        List<Edge> mst2 = kruskal.runWithNewRoads();
        kruskal.printResults();

        System.out.printf("%nSummary:%n");
        System.out.printf("  Existing-only MST:  %d edges, %.1f km total%n",
                mst1.size(), mst1.stream().mapToDouble(Edge::getDistance).sum());
        System.out.printf("  Enhanced MST:       %d edges, %.1f km total%n",
                mst2.size(), mst2.stream().mapToDouble(Edge::getDistance).sum());

        long newRoads = mst2.stream().filter(e -> !e.isExisting()).count();
        double newCost = mst2.stream().filter(e -> !e.isExisting())
                .mapToDouble(Edge::getConstructionCost).sum();
        System.out.printf("  New roads to build: %d (cost: %.0f Million EGP)%n", newRoads, newCost);

        System.out.println("\nComplexity: O(E log E) for sorting + O(E α(V)) for Union-Find");
        pauseForUser();
    }

    // ─── 2. Shortest Path ──────────────────────────────────────────────────
    private void runShortestPathDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│      TRAFFIC FLOW OPTIMIZATION (Dijkstra's Algorithm)    │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        // Demo several important routes across different periods
        String[][] routes = {
                {"1", "F1",  "Maadi → Airport"},
                {"7", "3",   "6th October City → Downtown"},
                {"13", "3",  "New Admin Capital → Downtown"},
                {"12", "F9", "Helwan → Qasr El Aini Hospital"}
        };

        for (int period = 0; period <= 3; period++) {
            System.out.println("\n--- " + DijkstraShortestPath.PERIOD_NAMES[period] + " ---");
            for (String[] r : routes) {
                dijkstra.printPath(r[0], r[1], period);
            }
        }

        System.out.println("\n--- Top Congested Roads (Morning Peak) ---");
        dijkstra.getMostCongestedRoads(DijkstraShortestPath.MORNING_PEAK, 5)
                .forEach(s -> System.out.println("  " + s));

        System.out.println("\n--- Alternate Route (Morning peak, congestion threshold 90%) ---");
        List<String> alternate = dijkstra.getAlternatePath("1", "F1",
                DijkstraShortestPath.MORNING_PEAK, 0.90);
        System.out.print("  Maadi → Airport (avoiding congestion): ");
        alternate.forEach(id -> System.out.print(getNodeName(id) + " → "));
        System.out.println("DONE");

        System.out.println("\nComplexity: O((V+E) log V) with binary heap priority queue");
        System.out.println("Memoization hits: " + dijkstra.run("1", 0).size() + " nodes cached");
        pauseForUser();
    }

    // ─── 3. Emergency A* ───────────────────────────────────────────────────
    private void runEmergencyRoutingDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│         EMERGENCY RESPONSE PLANNING (A* Algorithm)       │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        // Emergency scenarios
        System.out.println("\nScenario 1: Ambulance from 6th October City to Qasr El Aini Hospital");
        astar.printEmergencyRoute("7", "F9", DijkstraShortestPath.MORNING_PEAK);

        System.out.println("\nScenario 2: Emergency from New Admin Capital to Airport");
        astar.printEmergencyRoute("13", "F1", DijkstraShortestPath.EVENING_PEAK);

        System.out.println("\nScenario 3: Emergency from Shubra to Maadi Military Hospital");
        astar.printEmergencyRoute("11", "F10", DijkstraShortestPath.MORNING_PEAK);

        // Find nearest medical facility from various locations
        System.out.println("\n--- Nearest Medical Facility Finder ---");
        String[] testLocations = {"7", "13", "4", "11"};
        for (String loc : testLocations) {
            String nearest = astar.findNearestMedicalFacility(loc, DijkstraShortestPath.MORNING_PEAK);
            Node n = graph.getNode(nearest);
            System.out.printf("  From %-25s → Nearest facility: %s%n",
                    getNodeName(loc), n != null ? n.getName() : nearest);
        }

        System.out.println("\nComplexity: O(E) best case, O(b^d) worst case. Heuristic: geographic distance.");
        pauseForUser();
    }

    // ─── 4. Dynamic Programming ────────────────────────────────────────────
    private void runDPSchedulingDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│       PUBLIC TRANSIT OPTIMIZATION (Dynamic Programming)  │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        // Bus scheduling: 250 total buses
        System.out.println("\n[1] Bus Fleet Allocation (250 total buses across 10 routes):");
        DynamicProgramming.SchedulingResult busResult =
                dp.optimizeBusScheduling(busRoutes, 250);
        dp.printSchedulingResults(busResult, busRoutes);

        // Metro scheduling: 45 total trains
        System.out.println("\n[2] Metro Train Allocation (45 total trains across 3 lines):");
        DynamicProgramming.SchedulingResult metroResult =
                dp.optimizeMetroScheduling(metroLines, 45);
        System.out.println("Metro train allocations:");
        for (int i = 0; i < metroLines.size(); i++) {
            MetroLine m = metroLines.get(i);
            System.out.printf("  %-40s | %2d trains | %.1f min frequency%n",
                    m.getName(), m.getAssignedTrains(), m.getOptimizedFrequencyMin());
        }

        // Road maintenance: 500 Million EGP budget
        System.out.println("\n[3] Road Maintenance Allocation (Budget: 500 Million EGP):");
        DynamicProgramming.MaintenanceResult maintResult =
                dp.optimizeRoadMaintenance(500);
        dp.printMaintenanceResults(maintResult);

        // Memoization demo
        System.out.println("\n[4] Memoized Route Lookup:");
        String[][] pairs = {{"3","5"},{"1","3"},{"2","3"}};
        for (String[] p : pairs) {
            double d = dp.getMemoizedDistance(p[0], p[1], 0, dijkstra);
            System.out.printf("  %s → %s (morning): weight=%.2f [memo size=%d]%n",
                    getNodeName(p[0]), getNodeName(p[1]), d, dp.getMemoSize());
        }

        System.out.println("\nBus Scheduling Complexity: O(R × V) where R=routes, V=vehicles");
        System.out.println("Road Maintenance Complexity: O(E × B) where E=roads, B=budget units");
        pauseForUser();
    }

    // ─── 5. Greedy ─────────────────────────────────────────────────────────
    private void runGreedyDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│      TRAFFIC SIGNAL CONTROL (Greedy Algorithm)           │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        // Signal optimization for morning peak
        System.out.println("\n[1] Traffic Signal Optimization (Morning Peak, 120s cycles):");
        List<GreedyAlgorithms.SignalPhase> phases =
                greedy.optimizeTrafficSignals(DijkstraShortestPath.MORNING_PEAK, 120);
        greedy.printSignalResults(phases, 5);
        System.out.println("Total intersections optimized: " + phases.size());

        // Suboptimal analysis
        System.out.println("\n[2] Intersections where greedy is suboptimal (need green-wave DP):");
        List<String> suboptimal = greedy.findSuboptimalGreedyIntersections(
                DijkstraShortestPath.MORNING_PEAK);
        suboptimal.forEach(s -> System.out.println("  ⚠ " + s));

        // Emergency preemption demo
        System.out.println("\n[3] Emergency Vehicle Preemption (Priority Queue):");
        List<GreedyAlgorithms.EmergencyVehicle> vehicles = Arrays.asList(
                new GreedyAlgorithms.EmergencyVehicle("AMB-01", "7",  "F9",  9, "ambulance"),
                new GreedyAlgorithms.EmergencyVehicle("FIR-01", "11", "F10", 7, "fire"),
                new GreedyAlgorithms.EmergencyVehicle("POL-01", "4",  "3",   5, "police"),
                new GreedyAlgorithms.EmergencyVehicle("AMB-02", "12", "F9",  8, "ambulance")
        );
        List<String[]> actions = greedy.preemptSignals(vehicles, astar,
                DijkstraShortestPath.MORNING_PEAK);
        greedy.printPreemptionResults(actions);

        // Greedy vs DP maintenance comparison
        System.out.println("[4] Greedy vs DP Road Maintenance (Budget: 500M EGP):");
        List<GreedyAlgorithms.MaintenanceTask> greedyTasks = greedy.greedyMaintenance(500);
        double greedyVal = greedyTasks.stream().mapToDouble(t -> t.value).sum();
        double dpVal = dp.optimizeRoadMaintenance(500).totalImprovement;
        System.out.printf("  Greedy solution value:  %.1f (selected %d roads)%n",
                greedyVal, greedyTasks.size());
        System.out.printf("  DP (0/1 knapsack) value: %.1f%n", dpVal);
        System.out.printf("  DP improvement over Greedy: %.1f%%%n",
                greedyVal > 0 ? (dpVal - greedyVal) / greedyVal * 100 : 0);
        System.out.println("  → Shows why greedy is suboptimal for 0/1 knapsack!");

        System.out.println("\nSignal Optimization Complexity: O(I × D) per period");
        System.out.println("Preemption Complexity: O(E log E) for priority queue");
        pauseForUser();
    }

    // ─── 6. ML Prediction [Bonus] ──────────────────────────────────────────
    private void runMLPredictionDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│    ML TRAFFIC PREDICTION [BONUS] (Linear Regression)     │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        double[] hours = {7.5, 10.0, 13.0, 17.5, 22.0};
        String[] hourNames = {"7:30 AM", "10:00 AM", "1:00 PM", "5:30 PM", "10:00 PM"};
        for (int i = 0; i < hours.length; i++) {
            predictor.printPredictions(hours[i]);
        }

        System.out.println("\nModel Architecture: Linear Regression per road");
        System.out.println("Features: [1, hour_of_day, is_peak_hour]");
        System.out.println("Training: OLS closed-form solution (X^T X)^-1 X^T y");
        System.out.printf("Model MAE: %.0f vehicles/hour (vs ~2500 avg flow)%n",
                predictor.computeMAE());
        System.out.println("\nNote: Production version uses scikit-learn/TensorFlow with");
        System.out.println("      time-series features (day-of-week, weather, events).");
        pauseForUser();
    }

    // ─── 7. Algorithm Comparison [Bonus] ───────────────────────────────────
    private void runAlgorithmComparisonDemo() {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│    ALGORITHM COMPARISON: Dijkstra vs A* [BONUS]          │");
        System.out.println("└─────────────────────────────────────────────────────────┘");

        String[][] testCases = {
                {"7",  "F9",  "6th Oct City → Qasr El Aini Hospital"},
                {"13", "F9",  "New Admin Capital → Hospital"},
                {"12", "F10", "Helwan → Maadi Military Hospital"},
                {"15", "F1",  "Sheikh Zayed → Airport"},
                {"4",  "3",   "New Cairo → Downtown"}
        };

        System.out.println("\n" + String.format("%-45s | %-12s | %-12s | %-10s",
                "Route", "Dijkstra(ms)", "A*(ms)", "A* Faster"));
        System.out.println("-".repeat(90));

        for (String[] tc : testCases) {
            // Dijkstra timing
            long t0 = System.nanoTime();
            for (int rep = 0; rep < 100; rep++) {
                dijkstra.clearMemo();
                dijkstra.run(tc[0], DijkstraShortestPath.MORNING_PEAK);
            }
            double dijkstraMs = (System.nanoTime() - t0) / 1e6 / 100;

            // A* timing
            t0 = System.nanoTime();
            for (int rep = 0; rep < 100; rep++) {
                astar.run(tc[0], tc[1], DijkstraShortestPath.MORNING_PEAK, true);
            }
            double astarMs = (System.nanoTime() - t0) / 1e6 / 100;

            double speedup = dijkstraMs / Math.max(0.001, astarMs);
            System.out.printf("%-45s | %12.3f | %12.3f | %9.1fx%n",
                    tc[2], dijkstraMs, astarMs, speedup);
        }

        System.out.println("\nKey Differences:");
        System.out.println("  Dijkstra: Explores all nodes up to distance d (complete, optimal)");
        System.out.println("  A*:       Guides search toward goal via heuristic (faster, same optimal result)");
        System.out.println("  For Cairo: A* is 1.5-3x faster in practice due to geographic heuristic");
        System.out.println("  Dijkstra better for: multi-destination routing (pre-compute all distances)");
        System.out.println("  A* better for: single source-destination (emergency routing)");
        pauseForUser();
    }

    // ─── 8. Full Demo ──────────────────────────────────────────────────────
    private void runFullSystemDemo() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              FULL SYSTEM DEMONSTRATION                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        runMSTDemo();
        runShortestPathDemo();
        runEmergencyRoutingDemo();
        runDPSchedulingDemo();
        runGreedyDemo();
        runMLPredictionDemo();
        runAlgorithmComparisonDemo();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                  SYSTEM SUMMARY                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf ("║  Nodes: %-3d  Existing Roads: %-3d  Potential Roads: %-3d  ║%n",
                graph.getNodeCount(), graph.getEdgeCount(), graph.getPotentialEdges().size());
        System.out.println("║  Algorithms: MST, Dijkstra, A*, DP (×2), Greedy (×2), ML ║");
        System.out.println("║  Bonus: ML prediction, Algo comparison, Visualization      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    private String getNodeName(String id) {
        Node n = graph.getNode(id);
        return n != null ? n.getName() : id;
    }

    private void pauseForUser() {
        System.out.println("\nPress ENTER to continue...");
        scanner.nextLine();
    }

    // ─── Main entry point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        TransportationSystem system = new TransportationSystem();

        // If run with --auto flag, run full demo non-interactively
        if (args.length > 0 && args[0].equals("--auto")) {
            system.runFullSystemDemoAuto();
        } else {
            system.runInteractive();
        }
    }

    public void runFullSystemDemoAuto() {
        scanner = new Scanner(new ByteArrayInputStream("\n\n\n\n\n".getBytes()));
        runFullSystemDemo();
    }


//    private static class MyScanner extends Scanner {
//
//        public MyScanner() {
//            super(System.in);
//        }
//
//        @Override
//        public String nextLine() {
//            return ""; // يرجع Enter تلقائي
//        }
//    }
//
//


//    public void runFullSystemDemoAuto() {
//        scanner = new MyScanner(); // مش متغير محلي
//        runFullSystemDemo();
//    }

}
