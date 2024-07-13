/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.protein.tags.GroupCompareEngine;
import org.theseed.protein.tags.TagDirectory;
import org.theseed.protein.tags.TaxonCompare;
import org.theseed.protein.tags.scanner.FeatureScanner;
import org.theseed.taxonomy.TaxTree;
import org.theseed.taxonomy.TaxonListDirectory;

/**
 * This is a full taxonomic what's-changed pipeline for all the genomes in a genome source.  the output report
 * will specify for each differentiating tag the tag itself, the parent taxonomic group and the child
 * taxonomic group.  The intent is that given a genome in a taxonomic group and the tag of one of its features,
 * you can derive a statement about that feature differentiating the group from others under the same parent.
 *
 * The positional parameter is the directory or file containing the genome source.  The report will be written
 * to the standard output.
 *
 * A temporary tag directory and a temporary taxonomic tree directory will be used.  The taxonomic tree directory
 * will be re-used if it is nonempty, but the tag directory will always be rebuilt.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 * -o	output file for report (if not STDOUT)
 *
 * --taxDir		working directory for the taxonomic tree; default "TaxTree" in the current directory
 * --tagDir		temporary directory for the tags; default "Temp" in the current directory
 * --tags		type of feature scanner to use (default ROLE)
 * --roles		name of the role definition file (default "roles.in.subsystems" in the current directory)
 * --clear		erase and rebuild the taxonomic tree directory before processing
 * --absent		maximum fraction of genomes in a set that can have an absent tag (default 0.2)
 * --present	minimum fraction of genomes in a set that can have a present tag (default 0.8)
 * --keep		do not erase the tag directory when done
 *
 * @author Bruce Parrello
 *
 */
public class TaxonPipeProcessor extends BaseGenomeProcessor implements FeatureScanner.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxonPipeProcessor.class);
    /** taxonomy tree controller */
    private TaxTree taxonTree;
    /** tag directory controller */
    private TagDirectory tagController;
    /** taxonomy directory controller */
    private TaxonListDirectory taxController;
    /** feature scanner for tags */
    private FeatureScanner tagScanner;
    /** output print writer for report */
    private PrintWriter writer;

    // COMMAND-LINE OPTIONS

    /** output file (if not STDOUT) */
    @Option(name = "--output", aliases = { "-o" }, usage = "output file for report (if not STDOUT)")
    private File outFile;

    /** taxonomic tree directory name */
    @Option(name = "--taxDir", metaVar = "TaxTree", usage = "directory for building or loading the taxonomic tree")
    private File taxDir;

    /** tag directory name */
    @Option(name = "--tagDir", metaVar = "TempTags", usage = "temporary directory for building tag files")
    private File tagDir;

    /** if specified, the output directory will be erased before processing */
    @Option(name = "--clear", usage = "if specified, the output directory will be erased before processing")
    private boolean clearFlag;

    /** type of tag scanner */
    @Option(name = "--tags", usage = "type of feature scanner for computing tags")
    private FeatureScanner.Type scanType;

    /** name of the role definition file */
    @Option(name = "--roles", metaVar = "roles.in.subsystems", usage = "name of the role definition file for role-based tags")
    private File roleFile;

    /** maximum fraction for absent tags */
    @Option(name = "--absent", metaVar = "0.1", usage = "maximum fraction of genomes in a set that can have an absent tag")
    private double maxAbsent;

    /** minimum fraction for present tags */
    @Option(name = "--present", metaVar = "0.9", usage = "minimum fraction of genomes in a set that can have a present tag")
    private double minPresent;

    /** keep the tag directory when complete */
    @Option(name = "--keep", usage = "if specified, the tag directory will not be erased after processing")
    private boolean keepFlag;


    @Override
    protected void setSourceDefaults() {
        File curDir = new File(System.getProperty("user.dir"));
        this.outFile = null;
        this.taxDir = new File(curDir, "TaxTree");
        this.tagDir = new File(curDir, "Temp");
        this.clearFlag = false;
        this.scanType = FeatureScanner.Type.ROLE;
        this.roleFile = new File(curDir, "roles.in.subsystems");
        this.maxAbsent = 0.2;
        this.minPresent = 0.8;
        this.keepFlag = false;
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // Validate the tuning parameters.
        GroupCompareEngine.validateTuning(this.maxAbsent, this.minPresent);
        // Verify that we can create the tag scanner.
        this.tagScanner = this.scanType.create(this);
        // Set up the tag directory.
        if (this.tagDir.isDirectory()) {
            log.info("Tags will be built in {}.", this.tagDir);
            FileUtils.cleanDirectory(this.tagDir);
        } else {
            log.info("Creating tag directory {}.", this.tagDir);
            FileUtils.forceMkdir(this.tagDir);
        }
        this.tagController = new TagDirectory(this.tagDir);
        // Set up the taxon tree directory.
        if (! this.taxDir.isDirectory()) {
            log.info("Creating taxonomic tree directory {}.", this.taxDir);
            FileUtils.forceMkdir(this.tagDir);
            this.createTaxDir();
        } else if (this.clearFlag) {
            log.info("Erasing taxonomic tree directory {}.", this.taxDir);
            FileUtils.cleanDirectory(this.taxDir);
            this.createTaxDir();
        } else {
            log.info("Taxonomy data will be stored in directory {}.", this.taxDir);
            this.taxController = new TaxonListDirectory(this.taxDir);
        }
        // Get the taxonomy tree itself.
        this.taxonTree = this.taxController.getTaxTreeObject();
        // Open the output print writer.
        if (this.outFile == null) {
            log.info("Report will be written to the standard output.");
            this.writer = new PrintWriter(System.out);
        } else {
            log.info("Report will be written to {}.", this.outFile);
            this.writer = new PrintWriter(this.outFile);
        }
    }

    /**
     * This method fills an empty taxonomic list directory with data from the genome source and then
     * loads its taxonomy tree controller.
     *
     * @throws IOException
     */
    private void createTaxDir() throws IOException {
        this.taxController = new TaxonListDirectory(this.taxDir);
        this.taxController.updateRankMaps(this.getSource());
    }

    @Override
    protected void runCommand() throws Exception {
        try {
            // The first step is to fill the tag directory.
            log.info("Computing tags into {}.", this.tagDir);
            Set<String> genomeIDs = this.getGenomeIds();
            for (String genomeID : genomeIDs) {
                Genome genome = this.getGenome(genomeID);
                log.info("Scanning for tags in {}.", genome);
                this.tagController.addGenome(genome, this.tagScanner);
            }
            Map<Integer, Set<String>> diffMap = this.doCompare();
            // Get the map of taxonomic names.
            Map<Integer, String> nameMap = this.taxController.getNameMap(diffMap.keySet());
            // Write the output header.
            this.writer.println("tax_id\trank\tname\tparent_id\tparent_rank\tparent_name\ttag");
            long lastMessage = System.currentTimeMillis();
            int processed = 0;
            log.info("Writing output.");
            // Loop through the map entries, writing data.
            for (var diffEntry : diffMap.entrySet()) {
                processed++;
                // Get the taxonomic data.
                int taxId = diffEntry.getKey();
                int parentId = this.taxonTree.getParent(taxId);
                // Only proceed if we have a valid parent.
                if (parentId >= 0) {
                    // Get the taxonomic data.
                    String taxRank = this.taxController.getRank(taxId);
                    String parentRank = this.taxController.getRank(parentId);
                    String taxName = nameMap.getOrDefault(taxId, "<unknown>");
                    String parentName = nameMap.getOrDefault(parentId, "<unknown>");
                    String taxPrefix = taxId + "\t" + taxRank + "\t" + taxName + "\t" + parentId + "\t" + parentRank
                            + "\t" + parentName + "\t";
                    // Write one line per tag.
                    for (String tag : diffEntry.getValue())
                        writer.println(taxPrefix + tag);
                }
                long now = System.currentTimeMillis();
                if (now - lastMessage >= 5000) {
                    log.info("{} of {} taxonomic IDs processed.", processed, diffMap.size());
                    lastMessage = now;
                }
            }
            this.writer.flush();
        } finally {
            this.writer.close();
            if (! this.keepFlag) {
                log.info("Erasing tag directory {}.", this.tagDir);
                FileUtils.cleanDirectory(this.tagDir);
            }
        }
    }

    /**
     * Create the comparison engine and return the tag sets for each taxonomic grouping.
     *
     * @return a map from taxonomic IDs to tag sets
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public Map<Integer, Set<String>> doCompare() throws IOException, ParseFailureException {
        // Create the comparison engine.
        log.info("Initializing comparison engine.");
        TaxonCompare compareEngine = new TaxonCompare(this.taxController, this.tagController, this.maxAbsent, this.minPresent);
        // Get a map from taxonomic IDs to distinguishing tags.
        log.info("Performing comparisons.");
        Map<Integer, Set<String>> diffMap = compareEngine.computeDistinguishingTags();
        return diffMap;
    }

    @Override
    public File getRoleFileName() {
        return this.roleFile;
    }

}
