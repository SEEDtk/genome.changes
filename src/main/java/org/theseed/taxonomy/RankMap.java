/**
 *
 */
package org.theseed.taxonomy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.TaxItem;
import org.theseed.io.TabbedLineReader;

/**
 * This object contains the taxonomic lists for a particular taxonomic rank.  For each
 * taxonomic grouping, it contains the ID number, the name, and the list of IDs for the
 * genomes in the group.
 *
 * @author Bruce Parrello
 *
 */
public class RankMap {

    // FIELDS
    /** map of taxonomic IDs to descriptors */
    private Map<Integer, Taxon> taxMap;

    /**
     * This object contains the data for a specific taxonomic grouping.
     */
    public static class Taxon {

        /** taxonomy ID */
        private int id;
        /** taxonomic group name */
        private String name;
        /** set of genome IDs */
        private Set<String> genomes;

        /**
         * Construct a taxon descriptor for a specific taxonomic ID.
         *
         * @param taxId		taxonomic group ID
         * @param taxName	taxonomic group name
         */
        protected Taxon(int taxId, String taxName) {
            this.id = taxId;
            this.name = taxName;
            this.genomes = new TreeSet<String>();
        }

        /**
         * Add a genome to this taxonomic grouping
         *
         * @param genomeId	ID of the genome to add
         */
        public void add(String genomeId) {
            this.genomes.add(genomeId);
        }

        /**
         * @return the taxonomic ID
         */
        public int getId() {
            return this.id;
        }

        /**
         * @return the taxonomic group name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return the set of genomes in the group
         */
        public Set<String> getGenomes() {
            return this.genomes;
        }

    }

    /**
     * Construct a new, blank rank map.
     */
    public RankMap() {
        this.taxMap = new TreeMap<Integer, Taxon>();
    }

    /**
     * Construct a rank map from a rank-map save file.
     *
     * @param fileName	name of the file containing the saved rank map
     *
     * @throws IOException
     */
    public RankMap(File fileName) throws IOException {
        // Create the empty taxId map.
        this.taxMap = new TreeMap<Integer, Taxon>();
        // Loop through the input file, creating taxonomic groupings.
        try (TabbedLineReader inStream = new TabbedLineReader(fileName)) {
            for (var line : inStream) {
                final int taxId = line.getInt(0);
                Taxon lineTaxon = new Taxon(taxId, line.get(1));
                String[] genomes = StringUtils.split(line.get(2), ',');
                for (String genome : genomes)
                    lineTaxon.add(genome);
                this.taxMap.put(taxId, lineTaxon);
            }
        }
    }

    /**
     * Save this rank map to a file.
     *
     * @param fileName	name of the output file
     *
     * @throws IOException
     */
    public void save(File fileName) throws IOException {
        // Open the output file and write the header.
        try (PrintWriter outStream = new PrintWriter(fileName)) {
            outStream.println("tax_id\ttax_name\tgenomes");
            for (var taxon : this.taxMap.values())
                outStream.println(taxon.getId() + "\t" + taxon.getName() + "\t" + StringUtils.join(taxon.getGenomes(), ','));
        }
    }

    /**
     * @return an array of the taxonomic IDs in this rank map
     */
    public int[] getTaxIds() {
        final int n = this.taxMap.size();
        int[] retVal = new int[n];
        int i = 0;
        for (var taxId : this.taxMap.keySet()) {
            retVal[i] = taxId;
            i++;
        }
        return retVal;
    }

    /**
     * @return the taxonomic descriptor for the specified taxonomic grouping, or NULL if it is not present
     *
     * @param taxId		ID of the taxonomic grouping of interest
     */
    public Taxon getTaxData(int taxId) {
        return this.taxMap.get(taxId);
    }

    /**
     * Add a genome to this rank map.
     *
     * @param genomeID	ID of the genome to add
     * @param rankData	taxonomic ID and name for this rank
     */
    public void add(String genomeId, TaxItem rankData) {
        final int taxId = rankData.getId();
        final String name = rankData.getName();
        Taxon taxon = this.taxMap.computeIfAbsent(taxId, x -> new Taxon(taxId, name));
        taxon.add(genomeId);
    }

    /**
     * @return the number of taxonomic groups in the map
     */
    public int size() {
        return this.taxMap.size();
    }

}
