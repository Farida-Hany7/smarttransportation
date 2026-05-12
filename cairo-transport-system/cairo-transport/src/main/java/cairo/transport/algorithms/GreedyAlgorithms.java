package cairo.transport.algorithms;

import cairo.transport.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Greedy Algorithms for Cairo Traffic Management.
 *
 * Two greedy applications:
 *
 * 1. TRAFFIC SIGNAL OPTIMIZATION (Greedy interval scheduling):
 *    At each intersection, greedily assign green-phase time proportional to
 *    incoming traffic flow. Locally optimal per intersection.
 *    Time:  O(I × D) where I = intersections, D = directions per intersection
 *    Space: O(I)
 *    Analysis: Optimal for isolated intersections; suboptimal when intersections
 *              are coupled (green wave coordination requires global DP).
 *
 * 2. EMERGENCY VEHICLE PREEMPTION (Greedy priority queue):
 *    Maintain a priority queue of emergency vehicles sorted by urgency score.
 *    At each step, preempt signals on the highest-priority vehicle's path.
 *    Time:  O(E log E) for queue operations
 *    Space: O(E) where E = active emergency vehicles
 *
 * 3. GREEDY ROAD MAINTENANCE (Activity selection variant):
 *    Select maintenance tasks sorted by value/cost ratio (fractional knapsack).
 *    NOTE: This gives optimal result for fractional knapsack but suboptimal
 *    for 0/1 knapsack — illustrates greedy vs DP tradeoff.
 */
public class GreedyAlgorithms {

    private TransportGraph graph;

    public GreedyAlgorithms(TransportGraph graph) {
        this.graph = graph;
    }

    // =====================================================================
    // 1. TRAFFIC SIGNAL OPTIMIZATION
    // =====================================================================

    public static class SignalPhase {
        public String nodeId;
        public String nodeName;
        public Map<String, Integer> greenTimeSec; // direction -> green time in seconds
        public int cycleTimeSec;

        public SignalPhase(String nodeId, String nodeName, int cycleTimeSec) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.cycleTimeSec = cycleTimeSec;
            this.greenTimeSec = new LinkedHashMap<>();
        }
    }

    /**
     * Greedy traffic signal timing at all major intersections.
     * Green time for each incoming direction is proportional to its traffic flow.
     *
     * @param period  traffic period for flow data
     * @param cycleSec total signal cycle length in seconds (default 120)
     * @return list of optimized signal phases
     */
    public List<SignalPhase> optimizeTrafficSignals(int period, int cycleSec) {
        List<SignalPhase> results = new ArrayList<>();

        for (Node node : graph.getAllNodes()) {
            List<Edge> incoming = graph.getNeighbors(node.getId());
            if (incoming.size() < 2) continue; // not an intersection

            int totalFlow = incoming.stream()
                    .mapToInt(e -> e.getFlowForPeriod(period))
                    .sum();
            if (totalFlow == 0) continue;

            SignalPhase phase = new SignalPhase(node.getId(), node.getName(), cycleSec);

            // Greedy: allocate green time proportional to flow
            int allocated = 0;
            Edge lastEdge = null;
            for (Edge e : incoming) {
                int flow = e.getFlowForPeriod(period);
                int greenTime = (int)((double) flow / totalFlow * cycleSec);
                greenTime = Math.max(5, greenTime); // minimum 5 sec green
                phase.greenTimeSec.put(e.getFromId(), greenTime);
                allocated += greenTime;
                lastEdge = e;
            }
            // Adjust last direction to fill cycle exactly
            if (lastEdge != null && allocated != cycleSec) {
                int adjustment = cycleSec - allocated;
                String lastFrom = lastEdge.getFromId();
                phase.greenTimeSec.put(lastFrom,
                        phase.greenTimeSec.get(lastFrom) + adjustment);
            }

            results.add(phase);
        }
        return results;
    }

    /**
     * Identify intersections where greedy signal optimization is suboptimal.
     * Returns nodes where cascade effects (green waves) would be beneficial.
     * These are corridors with 3+ consecutive high-congestion intersections.
     */
    public List<String> findSuboptimalGreedyIntersections(int period) {
        List<String> problematic = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            List<Edge> neighbors = graph.getNeighbors(node.getId());
            long highCongestionNeighbors = neighbors.stream()
                    .filter(e -> e.getCongestionRatio(period) > 0.80)
                    .count();
            if (highCongestionNeighbors >= 2) {
                problematic.add(node.getName() != null ? node.getName() : node.getId());
            }
        }
        return problematic;
    }

    // =====================================================================
    // 2. EMERGENCY VEHICLE PREEMPTION
    // =====================================================================

    public static class EmergencyVehicle {
        public String vehicleId;
        public String currentLocation;
        public String destination;
        public int urgencyLevel;    // 1-10
        public String vehicleType;  // ambulance, fire, police

        public EmergencyVehicle(String id, String from, String to, int urgency, String type) {
            this.vehicleId = id;
            this.currentLocation = from;
            this.destination = to;
            this.urgencyLevel = urgency;
            this.vehicleType = type;
        }

        public double priorityScore() {
            double typeBonus = switch (vehicleType.toLowerCase()) {
                case "ambulance" -> 2.0;
                case "fire"      -> 1.8;
                default          -> 1.0;
            };
            return urgencyLevel * typeBonus;
        }
    }

    /**
     * Greedy priority-based emergency preemption.
     * Processes vehicles in priority order, allocating signal preemption corridor.
     *
     * @param vehicles list of active emergency vehicles
     * @param astar    A* router for path computation
     * @param period   current traffic period
     * @return ordered list of preemption actions (vehicle -> path)
     */
    public List<String[]> preemptSignals(List<EmergencyVehicle> vehicles,
                                          AStarSearch astar, int period) {
        // Greedy step: sort by priority score (highest first)
        PriorityQueue<EmergencyVehicle> pq = new PriorityQueue<>(
                (a, b) -> Double.compare(b.priorityScore(), a.priorityScore()));
        pq.addAll(vehicles);

        List<String[]> actions = new ArrayList<>();
        Set<String> preemptedNodes = new HashSet<>(); // avoid conflicting preemptions

        while (!pq.isEmpty()) {
            EmergencyVehicle ev = pq.poll();
            List<String> path = astar.run(ev.currentLocation, ev.destination, period, true);
            if (path.isEmpty()) continue;

            // Check for conflicts with already-preempted intersections
            boolean conflict = path.stream().anyMatch(preemptedNodes::contains);

            for (String nodeId : path) {
                preemptedNodes.add(nodeId);
            }

            Node dest = graph.getNode(ev.destination);
            String pathStr = path.stream()
                    .map(id -> { Node n = graph.getNode(id); return n != null ? n.getName() : id; })
                    .collect(Collectors.joining(" → "));

            actions.add(new String[]{
                    ev.vehicleId,
                    ev.vehicleType,
                    String.valueOf(ev.urgencyLevel),
                    dest != null ? dest.getName() : ev.destination,
                    pathStr,
                    conflict ? "DELAYED (conflict)" : "PREEMPTED"
            });
        }
        return actions;
    }

    // =====================================================================
    // 3. GREEDY ROAD MAINTENANCE (Fractional Knapsack - for comparison)
    // =====================================================================

    public static class MaintenanceTask {
        public Edge road;
        public double cost;
        public double value;
        public double valuePerCost;
    }

    /**
     * Greedy fractional knapsack for road maintenance.
     * NOTE: Suboptimal for 0/1 (indivisible) maintenance tasks —
     *       included to demonstrate where greedy fails vs DP.
     */
    public List<MaintenanceTask> greedyMaintenance(double budgetM) {
        List<MaintenanceTask> tasks = new ArrayList<>();
        for (Edge e : graph.getAllEdges()) {
            if (e.getCondition() >= 8) continue;
            MaintenanceTask t = new MaintenanceTask();
            t.road = e;
            t.cost = e.getDistance() * (10 - e.getCondition()) * 2.0;
            double condImprovement = (10 - e.getCondition()) / 10.0;
            t.value = e.getCapacity() * condImprovement * e.getDistance() / 100.0;
            t.valuePerCost = t.value / t.cost;
            tasks.add(t);
        }
        // Greedy: sort by value/cost ratio
        tasks.sort((a, b) -> Double.compare(b.valuePerCost, a.valuePerCost));

        List<MaintenanceTask> selected = new ArrayList<>();
        double remaining = budgetM;
        for (MaintenanceTask t : tasks) {
            if (t.cost <= remaining) {
                selected.add(t);
                remaining -= t.cost;
            }
        }
        return selected;
    }

    // =====================================================================
    // PRINT RESULTS
    // =====================================================================

    public void printSignalResults(List<SignalPhase> phases, int topN) {
        System.out.println("\n========== GREEDY TRAFFIC SIGNAL OPTIMIZATION ==========");
        System.out.println("Showing top " + topN + " busiest intersections:");
        phases.stream()
                .sorted((a, b) -> Integer.compare(
                        b.greenTimeSec.values().stream().mapToInt(i->i).sum(),
                        a.greenTimeSec.values().stream().mapToInt(i->i).sum()))
                .limit(topN)
                .forEach(p -> {
                    System.out.printf("  %-30s (cycle=%ds):%n", p.nodeName, p.cycleTimeSec);
                    p.greenTimeSec.forEach((from, t) -> {
                        Node n = graph.getNode(from);
                        String fromName = n != null ? n.getName() : from;
                        System.out.printf("    From %-25s → %3d sec green%n", fromName, t);
                    });
                });
    }

    public void printPreemptionResults(List<String[]> actions) {
        System.out.println("\n========== EMERGENCY PREEMPTION RESULTS ==========");
        for (String[] a : actions) {
            System.out.printf("  [%s] %s (urgency %s) → %s: %s%n  Route: %s%n%n",
                    a[0], a[1], a[2], a[3], a[5], a[4]);
        }
    }
}
