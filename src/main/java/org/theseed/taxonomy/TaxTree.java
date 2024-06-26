/**
 *
 */
package org.theseed.taxonomy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;

/**
 * This object represents a taxonomy tree designed for the TaxonListDirectory.  The key here is that during
 * construction we skip intermeidate levels in the membership, but we want each group associated with the lowest
 * possible parent level.  Thus, the first time we may see a parent it may turn out that a better parent becomes
 * available.
 *
 * @author Bruce Parrello
 *
 */
public class TaxTree {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxTree.class);
    /** map of taxonomic grouping IDs to parent information */
    private Map<Integer, Node> linkMap;
    /** map file name */
    private File fileName;
    /** taxonomic grouping ID for the root group */
    public static final int ROOT_GROUP = 1;

    /**
     * This class contains a parent ID and the parent's level.
     */
    public static class Node {

        /** parent tax ID */
        private int parent;
        /** parent rank level */
        private int level;

        /**
         * Create a new link node.
         *
         * @param parentId		parent taxonomic grouping ID
         * @param rankLevel		parent rank level
         */
        public Node(int parentId, int rankLevel) {
            this.parent = parentId;
            this.level = rankLevel;
        }

        /**
         * Merge a new possible parent ID into this node.
         *
         * @param parentId		proposed parent taxonomic grouping ID
         * @param rankLevel		proposed parent rank level
         */
        public void merge(int parentId, int rankLevel) {
            if (rankLevel > this.level) {
                this.parent = parentId;
                this.level = rankLevel;
            }
        }

        /**
         * @return the parent ID for this node
         */
        public int getParent() {
            return this.parent;
        }

    }

    /**
     * Create a taxonomic tree object from a file.  If the file does not exist, an empty
     * tree will be set up.
     *
     * @param mapFile	file containing the object
     *
     * @throws IOException
     */
    public TaxTree(File mapFile) throws IOException {
        this.linkMap = new HashMap<Integer, Node>();
        this.fileName = mapFile;
        if (mapFile.exists()) {
            // The map file exists, so we must load it.
            try (TabbedLineReader mapStream = new TabbedLineReader(mapFile)) {
                for (var line : mapStream) {
                    int childId = line.getInt(0);
                    int level = line.getInt(1);
                    int parentId = line.getInt(2);
                    this.linkMap.put(childId, new Node(parentId, level));
                }
                log.info("{} links loaded from taxonomic tree file {}.", this.linkMap.size(), mapFile);
            }
        }
    }

    /**
     * Add a new child-parent link to the tree.
     *
     * @param childId	taxonomic group ID for the child
     * @param parentId	proposed parent ID
     * @param rankLevel	rank level of the proposed parent
     */
    public void addLink(int childId, int parentId, int rankLevel) {
        // If the node does not exist, we create it filled-in.  The merge will be a no-op in that case.
        Node childNode = this.linkMap.computeIfAbsent(childId, x -> new Node(parentId, rankLevel));
        childNode.merge(parentId, rankLevel);
    }

    /**
     * Save this taxonomic tree object to its file.
     *
     * throws IOException
     */
    public void save() throws IOException {
        try (PrintWriter writer = new PrintWriter(this.fileName)) {
            writer.println("childId\tlevel\tparentId");
            for (var linkEntry : this.linkMap.entrySet()) {
                int childId = linkEntry.getKey();
                Node linkNode = linkEntry.getValue();
                writer.println(childId + "\t" + linkNode.level + "\t" + linkNode.parent);
            }
        }
    }

    /**
     * @return the actual taxonomic tree
     */
    public Map<Integer, Set<Integer>> getTree() {
        // Create a set of all the parents.
        Set<Integer> parents = this.linkMap.values().stream().map(x -> x.getParent()).collect(Collectors.toSet());
        // Build a map of parents to sets of children.
        Map<Integer, Set<Integer>> retVal = new HashMap<Integer, Set<Integer>>();
        for (var linkEntry : this.linkMap.entrySet()) {
            // Get this child ID and remove it from the parent set.
            int childId = linkEntry.getKey();
            parents.remove(childId);
            // Put the child in the parent's child list.
            Node linkNode = linkEntry.getValue();
            Set<Integer> childSet = retVal.computeIfAbsent(linkNode.getParent(), x -> new HashSet<Integer>());
            childSet.add(childId);
        }
        // Create a root node for the high-level parents.
        retVal.put(ROOT_GROUP, parents);
        log.info("Taxonomic tree built with {} parents and {} children.", retVal.size(), this.linkMap.size());
        return retVal;
    }

    /**
     * @return TRUE if the tree is empty
     */
    public boolean isEmpty() {
        return this.linkMap.isEmpty();
    }

    /**
     * @return the map file name
     */
    public File getFileName() {
        return this.fileName;
    }

}
