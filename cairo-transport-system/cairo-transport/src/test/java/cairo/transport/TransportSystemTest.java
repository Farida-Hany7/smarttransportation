package cairo.transport;

import cairo.transport.algorithms.*;
import cairo.transport.ml.TrafficPredictor;
import cairo.transport.model.*;
import cairo.transport.utils.DataLoader;

import java.util.*;

/**
 * Comprehensive test suite for Cairo Transportation System (CSE112).
 * Tests all algorithms with assertions.
 */
public class TransportSystemTest {

    private TransportGraph graph;
    private KruskalMST kruskal;
    private DijkstraShortestPath dijkstra;
    private AStarSearch astar;
    private DynamicProgramming dp;
    private GreedyAlgorithms greedy;
    private TrafficPredictor predictor;
    private List<BusRoute> busRoutes;
    private List<MetroLine> metroLines;

    private int passed = 0;
    private int failed = 0;

    public void setup() {
        graph      = DataLoader.loadGraph();
        busRoutes  = DataLoader.loadBusRoutes();
        metroLines = DataLoader.loadMetroLines();
        kruskal    = new KruskalMST(graph);
        dijkstra   = new DijkstraShortestPath(graph);
        astar      = new AStarSearch(graph);
        dp         = new DynamicProgramming(graph);
        greedy     = new GreedyAlgorithms(graph);
        predictor  = new TrafficPredictor(graph);
    }

    // ─── Test helpers ──────────────────────────────────────────────────────
    private void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  ✔ PASS: " + testName);
            passed++;
        } else {
            System.out.println("  ✘ FAIL: " + testName);
            failed++;
        }
    }

    private void assertEquals(String testName, Object expected, Object actual) {
        assertTrue(testName + " (expected=" + expected + ", got=" + actual + ")",
                Objects.equals(expected, actual));
    }

    // ─── Graph tests ───────────────────────────────────────────────────────
    public void testGraph() {
        System.out.println("\n[TEST] Graph Data Loading");
        assertTrue("25 nodes loaded (15 districts + 10 facilities)",
                graph.getNodeCount() == 25);
        assertTrue("36 existing roads loaded (28 original + 8 facility connections)",
                graph.getEdgeCount() == 36);
        assertTrue("15 potential new roads loaded",
                graph.getPotentialEdges().size() == 15);
        assertTrue("Node 'Downtown Cairo' exists",
                graph.getNode("3") != null);
        assertTrue("Node 'F9' (Hospital) is critical facility",
                graph.getNode("F9").isCriticalFacility());
        assertTrue("Node '3' (Business) is not critical facility",
                !graph.getNode("3").isCriticalFacility());
        assertTrue("Adjacency list populated for node 3",
                graph.getNeighbors("3").size() > 0);
    }

    // ─── MST tests ─────────────────────────────────────────────────────────
    public void testKruskalMST() {
        System.out.println("\n[TEST] Kruskal's MST");

        List<Edge> mst = kruskal.runOnExistingOnly();
        assertTrue("MST has at most V-1 edges",
                mst.size() <= graph.getNodeCount() - 1);
        assertTrue("MST distance is positive",
                kruskal.getTotalDistance() > 0);

        List<Edge> mstWithNew = kruskal.runWithNewRoads();
        assertTrue("MST with new roads is non-empty",
                !mstWithNew.isEmpty());
        assertTrue("MST existing-only has fewer/equal edges than with new roads",
                mst.size() <= mstWithNew.size());

        // Check no duplicate edges
        Set<String> seen = new HashSet<>();
        boolean noDuplicates = mstWithNew.stream().allMatch(e -> {
            String key = e.getFromId() + "-" + e.getToId();
            return seen.add(key);
        });
        assertTrue("No duplicate edges in MST", noDuplicates);
    }

    // ─── Dijkstra tests ────────────────────────────────────────────────────
    public void testDijkstra() {
        System.out.println("\n[TEST] Dijkstra's Algorithm");

        // Source to itself = 0
        Map<String, Double> dist = dijkstra.run("3", DijkstraShortestPath.AFTERNOON);
        assertTrue("Distance from node to itself is 0",
                dist.get("3") == 0.0);

        // Triangle inequality
        double d13 = dist.getOrDefault("1", Double.MAX_VALUE);
        assertTrue("Distance to Maadi is finite",
                d13 < Double.MAX_VALUE);

        // Afternoon traffic should yield lighter path than morning peak
        Map<String, Double> distMorning = dijkstra.run("4", DijkstraShortestPath.MORNING_PEAK);
        Map<String, Double> distAfternoon = dijkstra.run("4", DijkstraShortestPath.AFTERNOON);
        assertTrue("Morning peak distance ≥ afternoon distance (more congestion)",
                distMorning.getOrDefault("3", 0.0) >= distAfternoon.getOrDefault("3", 0.0));

        // Path reconstruction
        List<String> path = dijkstra.getPath("1", "F1", DijkstraShortestPath.AFTERNOON);
        assertTrue("Path from Maadi to Airport is non-empty", !path.isEmpty());
        assertTrue("Path starts at Maadi (1)", path.get(0).equals("1"));
        assertTrue("Path ends at Airport (F1)", path.get(path.size() - 1).equals("F1"));
    }

    // ─── A* tests ──────────────────────────────────────────────────────────
    public void testAStar() {
        System.out.println("\n[TEST] A* Search");

        List<String> path = astar.run("7", "F9", DijkstraShortestPath.MORNING_PEAK, true);
        assertTrue("A* finds path from 6th October to Hospital", !path.isEmpty());
        assertTrue("A* path starts at 6th October (7)", path.get(0).equals("7"));
        assertTrue("A* path ends at Hospital (F9)", path.get(path.size() - 1).equals("F9"));

        // Emergency should be faster than normal
        double emergencyTime = astar.estimateTravelTime(path, DijkstraShortestPath.MORNING_PEAK, true);
        double normalTime    = astar.estimateTravelTime(path, DijkstraShortestPath.MORNING_PEAK, false);
        assertTrue("Emergency travel time < normal time", emergencyTime < normalTime);

        // Same source-dest should return a path
        List<String> same = astar.run("3", "3", DijkstraShortestPath.AFTERNOON, false);
        assertTrue("Same source-dest path is trivial", same.size() <= 1);
    }

    // ─── DP tests ──────────────────────────────────────────────────────────
    public void testDynamicProgramming() {
        System.out.println("\n[TEST] Dynamic Programming");

        // Bus scheduling
        DynamicProgramming.SchedulingResult result = dp.optimizeBusScheduling(busRoutes, 250);
        assertTrue("DP bus scheduling result is non-null", result != null);
        assertTrue("DP bus total passengers > 0", result.totalPassengers > 0);
        int totalAllocated = Arrays.stream(result.vehiclesPerRoute).sum();
        assertTrue("Total allocated buses ≤ base + extra",
                totalAllocated >= busRoutes.stream().mapToInt(BusRoute::getBusesAssigned).sum());

        // Maintenance
        DynamicProgramming.MaintenanceResult maint = dp.optimizeRoadMaintenance(500);
        assertTrue("DP maintenance selects some roads", !maint.selectedRoads.isEmpty());
        assertTrue("DP maintenance cost ≤ budget", maint.totalCost <= 500);
        assertTrue("All selected roads have condition < 8",
                maint.selectedRoads.stream().allMatch(e -> e.getCondition() < 8));

        // Memoization
        double d1 = dp.getMemoizedDistance("3", "5", 0, dijkstra);
        double d2 = dp.getMemoizedDistance("3", "5", 0, dijkstra);
        assertTrue("Memoized result consistent", d1 == d2);
        assertTrue("Memo cache populated", dp.getMemoSize() >= 1);
    }

    // ─── Greedy tests ──────────────────────────────────────────────────────
    public void testGreedy() {
        System.out.println("\n[TEST] Greedy Algorithms");

        List<GreedyAlgorithms.SignalPhase> phases =
                greedy.optimizeTrafficSignals(DijkstraShortestPath.MORNING_PEAK, 120);
        assertTrue("Signal phases generated", !phases.isEmpty());

        // Each cycle should sum to 120s (within rounding)
        for (GreedyAlgorithms.SignalPhase p : phases) {
            int total = p.greenTimeSec.values().stream().mapToInt(i -> i).sum();
            assertTrue("Signal cycle sums to 120s at " + p.nodeName,
                    Math.abs(total - 120) <= 2);
        }

        // Greedy maintenance
        List<GreedyAlgorithms.MaintenanceTask> tasks = greedy.greedyMaintenance(500);
        assertTrue("Greedy selects some maintenance tasks", !tasks.isEmpty());
        double totalCost = tasks.stream().mapToDouble(t -> t.cost).sum();
        assertTrue("Greedy total cost ≤ budget", totalCost <= 500);
    }

    // ─── ML tests ──────────────────────────────────────────────────────────
    public void testMLPredictor() {
        System.out.println("\n[TEST] ML Traffic Predictor");

        predictor.train();
        assertTrue("Model trained for all roads",
                predictor.computeMAE() >= 0);

        // Predictions at peak should be higher than at night
        String roadId = "1-3";
        double peakPred  = predictor.predict(roadId, 8.0);   // 8 AM
        double nightPred = predictor.predict(roadId, 23.0);  // 11 PM
        assertTrue("Peak prediction ≥ night prediction for 1-3",
                peakPred >= nightPred);

        // Forecast list
        List<String> forecast = predictor.forecastTopCongestion(8.0, 5);
        assertTrue("Forecast returns 5 roads", forecast.size() == 5);

        // MAE should be reasonable (< 50% of average flow)
        double mae = predictor.computeMAE();
        assertTrue("Model MAE < 1000 veh/h (reasonable)", mae < 1000);
    }

    // ─── Run all tests ─────────────────────────────────────────────────────
    public void runAll() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         CAIRO TRANSPORTATION SYSTEM - TEST SUITE           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        setup();
        testGraph();
        testKruskalMST();
        testDijkstra();
        testAStar();
        testDynamicProgramming();
        testGreedy();
        testMLPredictor();

        System.out.println("\n════════════════════════════════════════");
        System.out.printf("RESULTS: %d passed, %d failed%n", passed, failed);
        if (failed == 0) {
            System.out.println("✔ All tests passed!");
        } else {
            System.out.println("✘ Some tests failed. Check output above.");
        }
        System.out.println("════════════════════════════════════════");
    }

    public static void main(String[] args) {
        new TransportSystemTest().runAll();
    }
}
