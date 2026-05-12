package cairo.transport.algorithms;

import cairo.transport.model.*;

import java.util.*;

/**
 * Dijkstra's Shortest Path Algorithm for Cairo Route Planning.
 *
 * Standard Complexity:
 *   Time:  O((V + E) log V) with binary heap / priority queue
 *   Space: O(V) for distance/predecessor maps
 *
 * Modifications for Cairo:
 *   1. Time-dependent weights: accounts for morning peak, afternoon, evening peak, night.
 *      Edge weight = f(distance, traffic_flow, capacity, time_period)
 *   2. Road condition factor: degraded roads have higher effective weight.
 *   3. Alternate route mode: can exclude specified edges (e.g., closed roads/accidents).
 *   4. Memoization: caches results for repeated (source, period) queries.
 */
public class DijkstraShortestPath {

    private TransportGraph graph;
    // Memoization cache: "source:period" -> {nodeId -> distance}
    private Map<String, Map<String, Double>> memo = new HashMap<>();
    private Map<String, Map<String, String>> predMemo = new HashMap<>();

    public static final int MORNING_PEAK  = 0;
    public static final int AFTERNOON     = 1;
    public static final int EVENING_PEAK  = 2;
    public static final int NIGHT         = 3;
    public static final String[] PERIOD_NAMES = {"Morning Peak", "Afternoon", "Evening Peak", "Night"};

    public DijkstraShortestPath(TransportGraph graph) {
        this.graph = graph;
    }

    /**
     * Run Dijkstra from source node.
     * @param sourceId   starting node ID
     * @param period     traffic period (0=morning,1=afternoon,2=evening,3=night)
     * @param excludeEdges set of "fromId-toId" to exclude (for alternate routes)
     * @return map of nodeId -> shortest distance from source
     */
    public Map<String, Double> run(String sourceId, int period, Set<String> excludeEdges) {
        String cacheKey = sourceId + ":" + period + ":" + excludeEdges.hashCode();
        if (memo.containsKey(cacheKey)) return memo.get(cacheKey); // memoized

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> pred = new HashMap<>();

        // Initialize all distances to infinity
        graph.getAllNodes().forEach(n -> dist.put(n.getId(), Double.MAX_VALUE));
        dist.put(sourceId, 0.0);

        // Min-heap: (distance, nodeId)
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.offer(new double[]{0.0, sourceId.hashCode()});

        // Map index -> nodeId (we encode nodeId as hashCode for PQ; store separately)
        Map<Integer, String> hashToId = new HashMap<>();
        graph.getAllNodes().forEach(n -> hashToId.put(n.getId().hashCode(), n.getId()));
        hashToId.put(sourceId.hashCode(), sourceId);

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            double[] curr = pq.poll();
            double currDist = curr[0];
            String currId = (String) getNodeIdFromHash(curr[1], hashToId);
            if (currId == null) continue;
            if (visited.contains(currId)) continue;
            visited.add(currId);

            for (Edge edge : graph.getNeighbors(currId)) {
                String neighbor = edge.getToId();
                // Check if edge is excluded
                if (excludeEdges.contains(currId + "-" + neighbor)) continue;

                double weight = edge.getWeight(period);
                double newDist = currDist + weight;

                if (newDist < dist.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    pred.put(neighbor, currId);
                    hashToId.put(neighbor.hashCode(), neighbor);
                    pq.offer(new double[]{newDist, neighbor.hashCode()});
                }
            }
        }

        memo.put(cacheKey, dist);
        predMemo.put(cacheKey, pred);
        return dist;
    }

    /** Simplified run without edge exclusions */
    public Map<String, Double> run(String sourceId, int period) {
        return run(sourceId, period, Collections.emptySet());
    }

    /** Run with default afternoon period */
    public Map<String, Double> run(String sourceId) {
        return run(sourceId, AFTERNOON);
    }

    /**
     * Reconstruct shortest path from source to target.
     */
    public List<String> getPath(String sourceId, String targetId, int period) {
        String cacheKey = sourceId + ":" + period + ":" + Collections.emptySet().hashCode();
        // Ensure dijkstra has been run
        if (!predMemo.containsKey(cacheKey)) run(sourceId, period);

        Map<String, String> pred = predMemo.get(cacheKey);
        if (pred == null) return Collections.emptyList();

        LinkedList<String> path = new LinkedList<>();
        String curr = targetId;
        while (curr != null && !curr.equals(sourceId)) {
            path.addFirst(curr);
            curr = pred.get(curr);
        }
        if (curr == null) return Collections.emptyList(); // no path
        path.addFirst(sourceId);
        return path;
    }

    /**
     * Find shortest path with congestion-aware alternate routing.
     * Excludes edges where congestion ratio > threshold.
     */
    public List<String> getAlternatePath(String sourceId, String targetId, int period,
                                          double congestionThreshold) {
        Set<String> overloadedEdges = new HashSet<>();
        for (Edge e : graph.getAllEdges()) {
            if (e.getCongestionRatio(period) > congestionThreshold) {
                overloadedEdges.add(e.getFromId() + "-" + e.getToId());
                overloadedEdges.add(e.getToId() + "-" + e.getFromId());
            }
        }
        String cacheKey = sourceId + ":" + period + ":" + overloadedEdges.hashCode();
        run(sourceId, period, overloadedEdges);
        Map<String, String> pred = predMemo.get(cacheKey);
        if (pred == null) return Collections.emptyList();

        LinkedList<String> path = new LinkedList<>();
        String curr = targetId;
        while (curr != null && !curr.equals(sourceId)) {
            path.addFirst(curr);
            curr = pred.get(curr);
        }
        if (curr == null) return Collections.emptyList();
        path.addFirst(sourceId);
        return path;
    }

    private String getNodeIdFromHash(double hash, Map<Integer, String> map) {
        return map.get((int) hash);
    }

    public void clearMemo() { memo.clear(); predMemo.clear(); }

    /**
     * Print shortest path result.
     */
    public void printPath(String sourceId, String targetId, int period) {
        Map<String, Double> dist = run(sourceId, period);
        List<String> path = getPath(sourceId, targetId, period);
        double d = dist.getOrDefault(targetId, Double.MAX_VALUE);

        System.out.printf("%n=== Dijkstra: %s → %s [%s] ===%n",
                getNodeName(sourceId), getNodeName(targetId), PERIOD_NAMES[period]);
        if (path.isEmpty()) {
            System.out.println("  No path found.");
            return;
        }
        System.out.print("  Path: ");
        for (int i = 0; i < path.size(); i++) {
            System.out.print(getNodeName(path.get(i)));
            if (i < path.size() - 1) System.out.print(" → ");
        }
        System.out.printf("%n  Total weight: %.2f%n", d);
        System.out.printf("  Hops: %d%n", path.size() - 1);
    }

    private String getNodeName(String id) {
        Node n = graph.getNode(id);
        return n != null ? n.getName() : id;
    }

    /**
     * Identify top-N most congested roads in a given period.
     */
    public List<String> getMostCongestedRoads(int period, int topN) {
        List<Edge> edges = new ArrayList<>(graph.getAllEdges());
        edges.sort((a, b) -> Double.compare(b.getCongestionRatio(period), a.getCongestionRatio(period)));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, edges.size()); i++) {
            Edge e = edges.get(i);
            result.add(String.format("%s → %s (%.0f%%)",
                    getNodeName(e.getFromId()), getNodeName(e.getToId()),
                    e.getCongestionRatio(period) * 100));
        }
        return result;
    }
}
