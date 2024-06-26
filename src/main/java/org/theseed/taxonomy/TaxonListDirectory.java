/**
 *
 */
package org.theseed.taxonomy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.TaxItem;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.io.TabbedLineReader;

/**
 * This object controls a master directory of taxonomy lists.  The master directory has one file for
 * each taxonomic rank, with the rank name and a suffix of ".tax", plus one file for the taxonomic
 * tree relationship with a name of "tree.links" and one that returns the rank of each taxonomic grouping
 * with a name of "ranks.idx".  The rank file contains a list of the genomes for each taxonomic grouping
 * within that rank.  The first column is the grouping ID, the second column is the grouping name, and the
 * third column is the genome IDs, comma-delimited.
 *
 * @author Bruce Parrello
 *
 */
public class TaxonListDirectory {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxonListDirectory.class);
    /** master directory name */
    private File dirName;
    /** array of rank-map files in rank-level order */
    private File[] rankFiles;
    /** file name for taxonomic tree */
    private File treeFile;
    /** map of taxonomic grouping IDs to ranks */
    private Map<Integer, String> rankIndex;
    /** file suffix for taxon rank map files */
    private static final String TAXON_RANK_MAP_SUFFIX = ".tax";
    /** array of taxonomic ranks in order */
    public static final String[] RANKS = new String[] { "superkingdom", "phylum", "class", "order", "family", "genus", "species" };
    /** empty rank map for creating new rank map files */
    private static final RankMap EMPTY_RANK_MAP = new RankMap();
    /** rank index file name */
    private static final String RANK_INDEX_NAME = "rank.index";

    /**
     * Return the level of a taxonomic rank.
     *
     * @param rank	taxonomic rank
     *
     * @return the rank level (0 being highest) or -1 if the rank is not found
     */
    public static int getRankLevel(String rank) {
        int retVal = RANKS.length - 1;
        while (retVal >= 0 && ! RANKS[retVal].contentEquals(rank)) retVal--;
        return retVal;
    }

    /**
     * This class is a comparator that sorts taxonomic ranks with the highest level first.  Invalid
     * ranks sort to the end.
     */
    public static class RankSorter implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            int l1 = getRankLevel(o1);
            if (l1 < 0) l1 = RANKS.length;
            int l2 = getRankLevel(o2);
            if (l2 < 0) l2 = RANKS.length;
            int retVal = l2 - l1;
            if (retVal == 0 && l1 >= RANKS.length)
                retVal = o1.compareTo(o2);
            return retVal;
        }

    }

    /**
     * Construct a list-directory object from a specified directory.  If the directory
     * does not exist it will be created.
     *
     * @param taxDir	name of the directory to use
     *
     * @throws IOException
     */
    public TaxonListDirectory(File taxDir) throws IOException {
        this.dirName = taxDir;
        if (! taxDir.isDirectory()) {
            FileUtils.forceMkdir(taxDir);
            log.info("Creating taxon list directory {}.", taxDir);
        } else
            log.info("Using taxon list directory {}.", taxDir);
        // Create the rank map.
        this.rankFiles = new File[RANKS.length];
        // Compute the taxonomic tree file name and the rank index name.
        this.treeFile = new File(taxDir, "tree.links");
        File rankIndexFile = new File(taxDir, RANK_INDEX_NAME);
        // Loop through the ranks, creating missing files and filling in the map.
        for (int i = 0; i < RANKS.length; i++) {
            String rank = RANKS[i];
            File rankFile = new File(taxDir, rank + TAXON_RANK_MAP_SUFFIX);
            if (! rankFile.canRead()) {
                log.info("Creating new rank file {}.", rankFile);
                EMPTY_RANK_MAP.save(rankFile);
            }
            this.rankFiles[i] = rankFile;
        }
        // Set up the rank index.
        this.rankIndex = new HashMap<Integer, String>();
        if (! rankIndexFile.canRead()) {
            log.info("Creating rank index file {}.", rankIndexFile);
            try (PrintWriter writer = new PrintWriter(rankIndexFile)) {
                writer.println("tax_id\trank");
            }
        } else {
            log.info("Reading rank index from {}.", rankIndexFile);
            try (TabbedLineReader rankStream = new TabbedLineReader(rankIndexFile)) {
                for (var line : rankStream)
                    this.rankIndex.put(line.getInt(0), line.get(1));
                log.info("{} taxonomic groupings found in rank index.", this.rankIndex.size());
            }
        }
    }

    /**
     * Get the rank map for a particular rank.
     *
     * @param rank	rank of interest
     *
     * @return the map for the rank, NULL if it does not exist
     *
     * @throws IOException
     */
    public RankMap getRankMap(String rank) throws IOException {
        int level = getRankLevel(rank);
        RankMap retVal;
        if (level < 0)
            retVal = null;
        else
            retVal = new RankMap(this.rankFiles[level]);
        return retVal;
    }

    /**
     * Update the rank maps from a genome source.  This is an expensive method that requires loading
     * all the rank maps and the taxonomic tree into memory at once.
     *
     * @throws IOException
     */
    public void updateRankMaps(GenomeSource genomes) throws IOException {
        // Load the data structures into memory.
        log.info("Loading taxonomy tree from {}.", this.treeFile);
        TaxTree taxTree = new TaxTree(this.treeFile);
        log.info("Loading rank maps from {}.", this.dirName);
        RankMap[] rankMaps = new RankMap[RANKS.length];
        for (int i = 0; i < RANKS.length; i++)
            rankMaps[i] = new RankMap(this.rankFiles[i]);
        // Loop through the genomes.
        final int nGenomes = genomes.size();
        int gCount = 0;
        int taxonCount = 0;
        for (Genome genome : genomes) {
            gCount++;
            log.info("Scanning genome {} of {}: {}.", gCount, nGenomes, genome);
            // Save a null value for the current child ID.
            int lastChild = -1;
            // Loop through the taxonomy.  We will go from children to parents.
            Iterator<TaxItem> iter = genome.taxonomy();
            while (iter.hasNext()) {
                TaxItem taxItem = iter.next();
                int rankLevel = getRankLevel(taxItem.getRank());
                if (rankLevel >= 0) {
                    // Here we have a taxonomic rank of interest.  Add it to the correct rank map.
                    RankMap rankMap = rankMaps[rankLevel];
                    rankMap.add(genome.getId(), taxItem);
                    taxonCount++;
                    // Form a parent-child link in the tree if needed.
                    if (lastChild >= 0)
                        taxTree.addLink(lastChild, taxItem.getId(), rankLevel);
                    lastChild = taxItem.getId();
                    // Add the taxonomic grouping to the rank index.
                    this.rankIndex.put(taxItem.getId(), taxItem.getRank());
                }
            }
        }
        log.info("{} genome IDs added to taxonomy rank maps for {} genomes.", taxonCount, gCount);
        // Save all the files.
        log.info("Saving data to {}.", this.dirName);
        taxTree.save();
        for (int i = 0; i < RANKS.length; i++)
            rankMaps[i].save(this.rankFiles[i]);
        File rankIndexFile = new File(this.dirName, RANK_INDEX_NAME);
        try (PrintWriter writer = new PrintWriter(rankIndexFile)) {
            writer.println("tax_id\trank");
            for (var rankEntry : this.rankIndex.entrySet())
                writer.println(rankEntry.getKey() + "\t" + rankEntry.getValue());
        }
    }

    /**
     * This loads all of the rank maps into memory and exposes them in a map keyed
     * by rank.  Note that it is very memory-intensive.
     *
     * @return a map of ranks to rank maps
     *
     * @throws IOException
     */
    public Map<String, RankMap> getAllRankMaps() throws IOException {
        Map<String, RankMap> retVal = new TreeMap<String, RankMap>();
        for (String rank : RANKS)
            retVal.put(rank, this.getRankMap(rank));
        return retVal;
    }

    /**
     * @return the taxonomy tree
     *
     * @throws IOException
     */
    public Map<Integer, Set<Integer>> getTaxTree() throws IOException {
        TaxTree taxTree = new TaxTree(this.treeFile);
        return taxTree.getTree();
    }

    /**
     * @return the rank of a taxonomic ID, or NULL if the ID does not exist in this directory
     *
     * @param taxId		taxonomic grouping ID of interest
     */
    public String getRank(int taxId) {
        return this.rankIndex.get(taxId);
    }

}
