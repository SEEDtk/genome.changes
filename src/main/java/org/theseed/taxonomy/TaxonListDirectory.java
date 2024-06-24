/**
 *
 */
package org.theseed.taxonomy;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.TaxItem;
import org.theseed.genome.iterator.GenomeSource;

/**
 * This object controls a master directory of taxonomy lists.  The master directory has one file for
 * each taxonomic rank, with the rank name and a suffix of ".tax", plus one file for the taxonomic
 * tree relationship with a name of "tree.links"  The rank file contains a list of the
 * genomes for each taxonomic grouping within that rank.  The first column is the grouping ID, the
 * second column is the grouping name, and the third column is the genome IDs, comma-delimited.
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
    /** file suffix for taxon rank map files */
    private static final String TAXON_RANK_MAP_SUFFIX = ".tax";
    /** array of taxonomic ranks in order */
    public static final String[] RANKS = new String[] { "superkingdom", "phylum", "class", "order", "family", "genus", "species" };
    /** empty rank map for creating new rank map files */
    private static final RankMap EMPTY_RANK_MAP = new RankMap();

    /**
     * Return the level of a taxonomic rank.
     *
     * @param rank	taxonomic rank
     *
     * @return the rank level (0 being highest) or -1 if the rank is not found
     */
    public static int getRankLevel(String rank) {
        int retVal = RANKS.length - 1;
        while (! RANKS[retVal].contentEquals(rank)) retVal--;
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
     * @param dirName	name of the directory to use
     *
     * @throws IOException
     */
    public TaxonListDirectory(File dirName) throws IOException {
        if (! dirName.isDirectory())
            FileUtils.forceMkdir(dirName);
        // Create the rank map.
        this.rankFiles = new File[RANKS.length];
        // Compute the taxonomic tree file name.
        this.treeFile = new File(dirName, "tree.links");
        // Loop through the ranks, creating missing files and filling in the map.
        for (int i = 0; i < RANKS.length; i++) {
            String rank = RANKS[i];
            File rankFile = new File(dirName, rank + TAXON_RANK_MAP_SUFFIX);
            if (! rankFile.canRead()) {
                log.info("Creating new rank file {}.", rankFile);
                EMPTY_RANK_MAP.save(rankFile);
            }
            this.rankFiles[i] = rankFile;
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
                    if (lastChild >= 0) {
                        taxTree.addLink(lastChild, taxItem.getId(), rankLevel);
                        lastChild = taxItem.getId();
                    }
                }
            }
        }
        log.info("{} genome IDs added to taxonomy rank maps for {} genomes.", taxonCount, gCount);
        // Save all the files.
        log.info("Saving data to {}.", this.dirName);
        taxTree.save();
        for (int i = 0; i < RANKS.length; i++)
            rankMaps[i].save(this.rankFiles[i]);
    }

}
