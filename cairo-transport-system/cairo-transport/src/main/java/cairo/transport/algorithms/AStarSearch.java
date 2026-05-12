package cairo.transport.algorithms;

import cairo.transport.model.*;

import java.util.*;

/**
 * A* Search Algorithm for Emergency Vehicle Routing in Cairo.
 *
 * Standard Complexity:
 *   Time:  O(E) in the best case; O(b^d) worst case where b=branching factor, d=depth.
 *          With admissible heuristic: typically much faster than Dijkstra in practice.
 *   Space: O(V) for open/closed sets
 *
 * Heuristic: Euclidean/Haversine geographic distance to goal (admissible - never overestimates).
 *
 * Modifications for Cairo Emergency Routing:
 *   1. Emergency preemption: intersections on the path get a signal priority bonus (weight reduction).
 *   2. Traffic-aware: avoids severely congested roads (ratio > 0.95) unless no alternative.
 *   3. Medical facility bias: extra priority when routing TO medical facilities.
 *   4. Path reconstructed with emergency corridor marking.
 */
public class AStarSearch {

    private TransportGraph graph;

    public AStarSearch(TransportGraph graph) {
        this.graph = graph;
    }

    /**
     * Run A* from source to target, optimized for emergency routing.
     * @param sourceId    start node
     * @param targetId    destination (typically a medical facility)
     * @param period      traffic period for congestion awareness
     * @param emergencyMode if true, applies signal preemption and avoids severe congestion
     * @return ordered list of node IDs representing the route
     */
    public List<String> run(String sourceId, String targetId, int period, boolean emergencyMode) {
        Node goal = graph.getNode(targetId);
        if (goal == null) return Collections.emptyList();

        // f(n) = g(n) + h(n)
        Map<String, Double> gScore = new HashMap<>(); // actual cost from start
        Map<String, Double> fScore = new HashMap<>(); // estimated total cost
        Map<String, String> cameFrom = new HashMap<>();

        graph.getAllNodes().forEach(n -> {
            gScore.put(n.getId(), Double.MAX_VALUE);
            fScore.put(n.getId(), Double.MAX_VALUE);
        });
        gScore.put(sourceId, 0.0);
        fScore.put(sourceId, heuristic(sourceId, targetId));

        // Open set ordered by f-score
        PriorityQueue<String> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(id -> fScore.getOrDefault(id, Double.MAX_VALUE)));
        openSet.add(sourceId);
        Set<String> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            String current = openSet.poll();
            if (current.equals(targetId)) {
                return reconstructPath(cameFrom, targetId);
            }
            closedSet.add(current);

            for (Edge edge : graph.getNeighbors(current)) {
                String neighbor = edge.getToId();
                if (closedSet.contains(neighbor)) continue;

                // Emergency mode: skip severely congested roads (if alternate exists)
                if (emergencyMode && edge.getCongestionRatio(period) > 0.95
                        && graph.getNeighbors(current).size() > 1) continue;

                double edgeWeight = computeEmergencyWeight(edge, period, emergencyMode, targetId);
                double tentativeG = gScore.get(current) + edgeWeight;

                if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    double h = heuristic(neighbor, targetId);
                    fScore.put(neighbor, tentativeG + h);

                    if (!openSet.contains(neighbor)) openSet.add(neighbor);
                    else {
                        // Re-add to trigger re-ordering (Java PQ doesn't support decrease-key)
                        openSet.remove(neighbor);
                        openSet.add(neighbor);
                    }
                }
            }
        }
        return Collections.emptyList(); // no path
    }

    /**
     * Emergency weight: reduces weight for low-congestion roads,
     * applies signal preemption bonus (15% reduction) in emergency mode.
     */
    private double computeEmergencyWeight(Edge edge, int period, boolean emergencyMode,
                                           String targetId) {
        double weight = edge.getDistance();
        // Congestion penalty
        double congestion = edge.getCongestionRatio(period);
        weight *= (1 + congestion * 0.5); // penalty proportional to congestion

        // Emergency signal preemption: 15% bonus on all roads
        if (emergencyMode) weight *= 0.85;

        // Extra bonus if this edge leads toward a medical facility
        Node to = graph.getNode(edge.getToId());
        if (emergencyMode && to != null && to.isCriticalFacility()) weight *= 0.70;

        // Road condition penalty
        double condFactor = Math.max(0.5, edge.getCondition() / 10.0);
        weight /= condFactor;

        return weight;
    }

    /**
     * Admissible heuristic: straight-line geographic distance (never overestimates road distance).
     */
    private double heuristic(String nodeId, String goalId) {
        Node n    = graph.getNode(nodeId);
        Node goal = graph.getNode(goalId);
        if (n == null || goal == null) return 0;
        return n.distanceTo(goal); // Euclidean approx in km
    }

    private List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        LinkedList<String> path = new LinkedList<>();
        while (cameFrom.containsKey(current)) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }
        path.addFirst(current);
        return path;
    }

    /**
     * Compute total travel time estimate for a given path (in minutes).
     * Assumes average speed of 40 km/h under normal conditions, 60 km/h for emergency.
     */
    public double estimateTravelTime(List<String> path, int period, boolean emergencyMode) {
        if (path.size() < 2) return 0;
        double totalKm = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Optional<Edge> edge = graph.getEdge(path.get(i), path.get(i + 1));
            totalKm += edge.map(Edge::getDistance).orElse(0.0);
        }
        double speedKmh = emergencyMode ? 60.0 : 40.0;
        // Morning/evening peak: further slowdown for non-emergency
        if (!emergencyMode && (period == 0 || period == 2)) speedKmh *= 0.7;
        return (totalKm / speedKmh) * 60; // minutes
    }

    public void printEmergencyRoute(String sourceId, String targetId, int period) {
        List<String> path = run(sourceId, targetId, period, true);
        List<String> normalPath = run(sourceId, targetId, period, false);

        System.out.printf("%n=== A* Emergency Route: %s → %s [%s] ===%n",
                getNodeName(sourceId), getNodeName(targetId),
                DijkstraShortestPath.PERIOD_NAMES[period]);

        if (path.isEmpty()) { System.out.println("  No route found!"); return; }

        System.out.print("  Emergency route: ");
        for (int i = 0; i < path.size(); i++) {
            System.out.print(getNodeName(path.get(i)));
            if (i < path.size() - 1) System.out.print(" → ");
        }
        double emergencyTime = estimateTravelTime(path, period, true);
        double normalTime    = estimateTravelTime(normalPath.isEmpty() ? path : normalPath, period, false);
        System.out.printf("%n  Emergency ETA:    %.1f min%n", emergencyTime);
        System.out.printf("  Normal ETA:       %.1f min%n", normalTime);
        System.out.printf("  Time saved:       %.1f min (%.0f%% faster)%n",
                normalTime - emergencyTime,
                normalTime > 0 ? (normalTime - emergencyTime) / normalTime * 100 : 0);
    }

    private String getNodeName(String id) {
        Node n = graph.getNode(id);
        return n != null ? n.getName() : id;
    }

    /**
     * Find nearest medical facility from a given location.
     */
    public String findNearestMedicalFacility(String sourceId, int period) {
        String nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Node n : graph.getAllNodes()) {
            if (!n.getType().equalsIgnoreCase("Medical")) continue;
            List<String> path = run(sourceId, n.getId(), period, true);
            if (path.isEmpty()) continue;
            double dist = estimateTravelTime(path, period, true);
            if (dist < minDist) { minDist = dist; nearest = n.getId(); }
        }
        return nearest;
    }
}
