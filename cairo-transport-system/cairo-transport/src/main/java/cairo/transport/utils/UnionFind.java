package cairo.transport.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Union-Find (Disjoint Set Union) data structure with path compression and union by rank.
 *
 * Time Complexity: O(α(n)) per operation (nearly O(1) amortized) where α is inverse Ackermann.
 * Space Complexity: O(n)
 *
 * Used by Kruskal's MST algorithm.
 */
public class UnionFind {
    private Map<String, String> parent;
    private Map<String, Integer> rank;

    public UnionFind(Iterable<String> elements) {
        parent = new HashMap<>();
        rank   = new HashMap<>();
        for (String e : elements) {
            parent.put(e, e);   // each node is its own parent initially
            rank.put(e, 0);
        }
    }

    /** Find root of element with path compression */
    public String find(String x) {
        if (!parent.get(x).equals(x)) {
            parent.put(x, find(parent.get(x))); // path compression
        }
        return parent.get(x);
    }

    /** Union two sets by rank. Returns true if they were in different sets. */
    public boolean union(String x, String y) {
        String rootX = find(x);
        String rootY = find(y);
        if (rootX.equals(rootY)) return false; // already connected

        // Union by rank
        if (rank.get(rootX) < rank.get(rootY)) {
            parent.put(rootX, rootY);
        } else if (rank.get(rootX) > rank.get(rootY)) {
            parent.put(rootY, rootX);
        } else {
            parent.put(rootY, rootX);
            rank.put(rootX, rank.get(rootX) + 1);
        }
        return true;
    }

    public boolean connected(String x, String y) {
        return find(x).equals(find(y));
    }
}
