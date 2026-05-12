package cairo.transport.algorithms;

import cairo.transport.model.*;
import cairo.transport.utils.UnionFind;

import java.util.*;

/**
 * Kruskal's Minimum Spanning Tree Algorithm for Cairo's Infrastructure Network Design.
 *
 * Standard Complexity:
 *   Time:  O(E log E) - dominated by sorting edges
 *   Space: O(V + E)   - Union-Find + edge list
 *
 * Modifications for Cairo transportation:
 *   1. Population-weighted edges: edge weight = distance / (log(popFrom) + log(popTo) + 1)
 *      High-population connections are prioritized (lower effective weight).
 *   2. Critical facility guarantee: after standard MST, forced inclusion of any unconnected
 *      critical facility (hospital, airport, transit hub, government).
 *   3. Road condition factor: poor-condition existing roads are penalized in weight.
 *   4. Considers both existing roads and potential new roads (combined edge list).
 */
public class KruskalMST {

    private TransportGraph graph;
    private List<Edge> mstEdges;
    private double totalDistance;
    private double totalCost;    // construction cost for new roads selected

    public KruskalMST(TransportGraph graph) {
        this.graph  = graph;
        this.mstEdges = new ArrayList<>();
    }

    /**
     * Run Kruskal's algorithm with Cairo-specific modifications.
     * @param includeNewRoads  if true, also considers potential new roads
     */
    public List<Edge> run(boolean includeNewRoads) {
        mstEdges.clear();
        totalDistance = 0;
        totalCost = 0;

        // Step 1: Collect all edges
        List<Edge> candidates = new ArrayList<>(graph.getAllEdges());
        if (includeNewRoads) candidates.addAll(graph.getPotentialEdges());

        // Step 2: Sort by modified weight (population-prioritized, condition-adjusted)
        candidates.sort(Comparator.comparingDouble(e -> modifiedWeight(e)));

        // Step 3: Initialize Union-Find with all node IDs
        Set<String> nodeIds = new HashSet<>();
        graph.getAllNodes().forEach(n -> nodeIds.add(n.getId()));
        UnionFind uf = new UnionFind(nodeIds);

        // Step 4: Standard Kruskal's
        for (Edge e : candidates) {
            if (!nodeIds.contains(e.getFromId()) || !nodeIds.contains(e.getToId())) continue;
            if (uf.union(e.getFromId(), e.getToId())) {
                mstEdges.add(e);
                totalDistance += e.getDistance();
                if (!e.isExisting()) totalCost += e.getConstructionCost();
                if (mstEdges.size() == nodeIds.size() - 1) break; // MST complete
            }
        }

        // Step 5: Guarantee critical facility connectivity
        ensureCriticalFacilitiesConnected(uf, candidates, nodeIds);

        return mstEdges;
    }

    /**
     * Modified weight function: lower weight for roads connecting populous areas,
     * penalty for poor road condition on existing roads.
     */
    private double modifiedWeight(Edge edge) {
        Node from = graph.getNode(edge.getFromId());
        Node to   = graph.getNode(edge.getToId());

        double base = edge.getDistance();

        // Population priority: divide by log-population sum (boosts high-pop connections)
        double popFrom = (from != null && from.getPopulation() > 0)
                ? Math.log1p(from.getPopulation()) : 1.0;
        double popTo   = (to   != null && to.getPopulation()   > 0)
                ? Math.log1p(to.getPopulation())   : 1.0;
        double popFactor = Math.max(1.0, (popFrom + popTo) / 2.0);

        // Road condition factor: bad roads weigh more
        double condFactor = edge.isExisting() ? (11.0 - edge.getCondition()) / 10.0 : 1.0;

        return (base * condFactor) / popFactor;
    }

    /**
     * After MST construction, check all critical facilities are reachable.
     * If not, greedily add the shortest edge connecting them.
     */
    private void ensureCriticalFacilitiesConnected(UnionFind uf, List<Edge> candidates,
                                                    Set<String> nodeIds) {
        for (Node n : graph.getAllNodes()) {
            if (!n.isCriticalFacility()) continue;
            // Check if this critical node is isolated (not yet connected to MST)
            boolean connected = mstEdges.stream().anyMatch(
                    e -> e.getFromId().equals(n.getId()) || e.getToId().equals(n.getId()));
            if (!connected) {
                // Find the shortest available edge to connect it
                for (Edge e : candidates) {
                    if ((e.getFromId().equals(n.getId()) || e.getToId().equals(n.getId()))
                            && !uf.connected(e.getFromId(), e.getToId())) {
                        uf.union(e.getFromId(), e.getToId());
                        mstEdges.add(e);
                        totalDistance += e.getDistance();
                        if (!e.isExisting()) totalCost += e.getConstructionCost();
                        break;
                    }
                }
            }
        }
    }

    /** Run standard MST on existing roads only */
    public List<Edge> runOnExistingOnly() { return run(false); }

    /** Run enhanced MST considering potential new roads */
    public List<Edge> runWithNewRoads()   { return run(true); }

    public List<Edge> getMstEdges()   { return mstEdges; }
    public double getTotalDistance()  { return totalDistance; }
    public double getTotalNewRoadCost() { return totalCost; }

    public void printResults() {
        System.out.println("\n========== KRUSKAL'S MST RESULTS ==========");
        System.out.println("MST edges selected: " + mstEdges.size());
        System.out.printf("Total network distance: %.1f km%n", totalDistance);
        if (totalCost > 0)
            System.out.printf("Total new road construction cost: %.0f Million EGP%n", totalCost);
        System.out.println("--------------------------------------------");
        for (Edge e : mstEdges) {
            Node from = graph.getNode(e.getFromId());
            Node to   = graph.getNode(e.getToId());
            String fromName = from != null ? from.getName() : e.getFromId();
            String toName   = to   != null ? to.getName()   : e.getToId();
            System.out.printf("  %-35s <-> %-35s  %.1f km%s%n",
                    fromName, toName, e.getDistance(),
                    e.isExisting() ? "" : String.format(" [NEW, %.0fM EGP]", e.getConstructionCost()));
        }

        // Count critical facilities in MST
        long criticalCount = graph.getAllNodes().stream()
                .filter(Node::isCriticalFacility)
                .filter(n -> mstEdges.stream().anyMatch(
                        e -> e.getFromId().equals(n.getId()) || e.getToId().equals(n.getId())))
                .count();
        System.out.printf("%nCritical facilities connected: %d / %d%n",
                criticalCount,
                graph.getAllNodes().stream().filter(Node::isCriticalFacility).count());
    }
}
