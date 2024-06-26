/**
 *
 */
package org.theseed.protein.tags;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.taxonomy.TaxonListDirectory;

/**
 * This object does a mass taxonomy-based comparison on a set of genomes for whom a TaxonListDirectory and a TagDirectory
 * have already been created.  At each taxonomic level, it will compare each child group to the union of the other child
 * groups, thus showing the distinguishing tags unique to that group.  Obviously, this will not be done for singleton
 * groups.  The output will be a map from taxonomic grouping IDs to distinguishing tag sets.
 *
 * A full pipeline would create the two directories and then call this object.
 *
 *
 * @author Bruce Parrello
 *
 */
public class TaxonCompare {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxonCompare.class);
    /** taxonomic list directory */
    private TaxonListDirectory taxDir;
    /** taxonomic tree for the list directory */
    private Map<Integer, Set<Integer>> taxTree;
    /** tag directory for tag loading */
    private TagDirectory tagDir;
    /** group comparison engine */
    private GroupCompareEngine compareEngine;

    /**
     * This is a utility class that contains the data we need on a sibling genome set in order to
     * compute its distinguishing tags.
     */
    protected class SiblingData {

        /** taxonomic ID of the sibling */
        private int taxId;
        /** size of the genome set */
        private int size;
        /** tag counts */
        private TagCounts counts;

        /**
         * Compute the necessary data for a taxonomic grouping.
         *
         * @param id			taxonomic grouping ID
         * @param genomeSet		set of genome IDs for genomes in the grouping
         *
         * @throws IOException
         */
        protected SiblingData(int id, Set<String> genomeSet) throws IOException {
            // Save the ID and size.
            this.taxId = id;
            this.size = genomeSet.size();
            this.counts = TaxonCompare.this.tagDir.getTagCounts(genomeSet);
        }

        /**
         * @return the taxonomic group ID
         */
        public int getTaxId() {
            return this.taxId;
        }

        /**
         * @return the number of genomes in the group
         */
        public int getSize() {
            return this.size;
        }

        /**
         * @return the tag counts for the group
         */
        public TagCounts getCounts() {
            return this.counts;
        }
    }

    /**
     * Construct a new taxon comparison object.
     *
     * @param taxDirName	name of the taxonomic list directory
     * @param tagDirName	name of the tag directory
     * @oaram absent		maximum fraction of a set allowed for an absent tag
     * @param present		minimum fraction of a set allowed for a present tag
     *
     * @throws IOException, ParseFailureException
     */
    public TaxonCompare(File taxDirName, File tagDirName, double absent, double present) throws ParseFailureException, IOException {
        if (! taxDirName.isDirectory())
            throw new FileNotFoundException("Taxonomic list directory " + taxDirName + " is not found or invalid.");
        if (! tagDirName.isDirectory())
            throw new FileNotFoundException("Tag directory " + tagDirName + " is not found or invalid.");
        // Get the taxonomic stuff set up.
        this.taxDir = new TaxonListDirectory(taxDirName);
        this.taxTree = this.taxDir.getTaxTree();
        // Create the tag directory and the group comparison engine.
        this.tagDir = new TagDirectory(tagDirName);
        this.compareEngine = new GroupCompareEngine(this.tagDir, absent, present);
    }

    /**
     * Perform the mass comparison.
     *
     * @return a map from taxonomic group IDs to distinguishing tag sets
     */
    public Map<Integer, Set<String>> computeDistinguishingTags() {
        // Create the return map.  It is concurrent, because we will be updating it in parallel.
        final Map<Integer, Set<String>> retVal = new ConcurrentHashMap<Integer, Set<String>>();
        // Loop through the tree, processing children of sibling sets.  Comparisons only
        // matter if there is more than one sibling in the set.
        Set<Set<Integer>> siblingSets = this.taxTree.values().stream().filter(x -> x.size() > 1).collect(Collectors.toSet());
        siblingSets.parallelStream().forEach(x -> this.processChildren(retVal, x));
        return retVal;
    }

    /**
     * Find the distinguishing tags for all the specified siblings.  The distinguishing tags for a
     * sibling are those that are present in the sibling but not in any of its peers.
     *
     * @param outMap	output map of taxon IDs to distinguishing-tag sets
     * @param siblings	set of siblings to process
     */
    protected void processChildren(Map<Integer, Set<String>> outMap, Set<Integer> siblings) {
        try {
            // For each sibling we need to know the size of its genome set and its tag counts.
            List<SiblingData> siblingList = this.getList(siblings);
            // Create the master tag counts for the parent.
            TagCounts totalTags = new TagCounts();
            int totalSize = 0;
            for (SiblingData data : siblingList) {
                totalTags.merge(data.getCounts());
                totalSize += data.getSize();
            }
            // Now we compute the distinguishing sets and store them in the output map.
            for (SiblingData data : siblingList) {
                final int taxId = data.getTaxId();
                int leftSize = data.getSize();
                int rightSize = totalSize - leftSize;
                TagCounts leftTags = data.getCounts();
                TagCounts rightTags = totalTags.minus(leftTags);
                Set<String> distinguishingTags = this.compareEngine.distinguishLeft(leftTags, leftSize, rightTags, rightSize);
                outMap.put(taxId, distinguishingTags);
                log.info("{} distinguishing tags found for {}.", distinguishingTags.size(), taxId);
            }
        } catch (IOException e) {
            // Convert IO exceptions to unchecked so we can stream this method.
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Compute the necessary data for each sibling-- tag counts, ID, and set size
     *
     * @param siblings	set of sibling IDs to process
     *
     * @return a list of sibling-data objects for the siblings
     *
     * @throws IOException
     */
    private List<SiblingData> getList(Set<Integer> siblings) throws IOException {
        List<SiblingData> retVal = new ArrayList<SiblingData>(siblings.size());
        // Create a map from the siblings to their genome sets.
        Map<Integer, Set<String>> siblingMap = this.taxDir.getGenomeSets(siblings);
        // Loop through the siblings, creating the sibling data objects.
        for (Integer siblingId : siblings) {
            Set<String> genomes = siblingMap.get(siblingId);
            SiblingData data = new SiblingData(siblingId, genomes);
            retVal.add(data);
        }
        return retVal;
    }

}
