/**
 *
 */
package org.theseed.taxonomy;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object controls a master directory of taxonomy lists.  The master directory has one file for
 * each taxonomic rank, with the rank name and a suffix of ".tax".  The file contains a list of the
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
    /** map of rank names to subdirectories */
    private Map<String, File> rankMap;



    // TODO data members for TaxonListDirectory

    // TODO constructors and methods for TaxonListDirectory
}
