package cairo.transport.model;

import java.util.*;

/**
 * Weighted undirected graph representing Cairo's transportation network.
 * Supports both existing roads and potential new roads.
 * Provides adjacency list representation with O(1) node lookup.
 *
 * Space Complexity: O(V + E)
 */
public class TransportGraph {
    private Map<String, Node> nodes;
    private Map<String, List<Edge>> adjacencyList;
    private List<Edge> allEdges;
    private List<Edge> potentialEdges;

    public TransportGraph() {
        nodes = new LinkedHashMap<>();
        adjacencyList = new LinkedHashMap<>();
        allEdges = new ArrayList<>();
        potentialEdges = new ArrayList<>();
    }

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        String from = edge.getFromId();
        String to   = edge.getToId();
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);
        // Add reverse edge for undirected graph
        Edge reverse = new Edge(to, from, edge.getDistance(), edge.getCapacity(), edge.getCondition());
        reverse.setTrafficFlow(edge.getMorningPeak(), edge.getAfternoon(),
                edge.getEveningPeak(), edge.getNight());
        adjacencyList.computeIfAbsent(to, k -> new ArrayList<>()).add(reverse);
        allEdges.add(edge);
    }

    public void addPotentialEdge(Edge edge) {
        potentialEdges.add(edge);
    }

    public Node getNode(String id) { return nodes.get(id); }

    public List<Edge> getNeighbors(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public Collection<Node> getAllNodes()      { return nodes.values(); }
    public List<Edge> getAllEdges()            { return allEdges; }
    public List<Edge> getPotentialEdges()      { return potentialEdges; }
    public Map<String, Node> getNodeMap()      { return nodes; }
    public int getNodeCount()                  { return nodes.size(); }
    public int getEdgeCount()                  { return allEdges.size(); }

    /** Returns all edges (existing + potential) for MST consideration */
    public List<Edge> getAllEdgesIncludingPotential() {
        List<Edge> combined = new ArrayList<>(allEdges);
        combined.addAll(potentialEdges);
        return combined;
    }

    /** Find edge between two nodes */
    public Optional<Edge> getEdge(String fromId, String toId) {
        return adjacencyList.getOrDefault(fromId, Collections.emptyList())
                .stream()
                .filter(e -> e.getToId().equals(toId))
                .findFirst();
    }

    /** Set traffic flow data on an existing edge */
    public void setTrafficFlow(String fromId, String toId,
                                int morning, int afternoon, int evening, int night) {
        adjacencyList.getOrDefault(fromId, Collections.emptyList())
                .stream()
                .filter(e -> e.getToId().equals(toId))
                .findFirst()
                .ifPresent(e -> e.setTrafficFlow(morning, afternoon, evening, night));
        // Also set on reverse direction
        adjacencyList.getOrDefault(toId, Collections.emptyList())
                .stream()
                .filter(e -> e.getToId().equals(fromId))
                .findFirst()
                .ifPresent(e -> e.setTrafficFlow(morning, afternoon, evening, night));
    }

    public void printSummary() {
        System.out.println("=== Cairo Transportation Graph ===");
        System.out.println("Nodes (locations): " + nodes.size());
        System.out.println("Existing edges (roads): " + allEdges.size());
        System.out.println("Potential new roads: " + potentialEdges.size());
    }
}
