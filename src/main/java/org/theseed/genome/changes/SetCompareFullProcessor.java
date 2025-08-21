/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.io.TabbedLineReader;
import org.theseed.p3api.P3Genome;
import org.theseed.protein.tags.GroupCompareEngine;
import org.theseed.protein.tags.TagDirectory;
import org.theseed.protein.tags.scanner.FeatureScanner;

/**
 * This command will perform a what's-changed comparison between two non-overlapping subsets of a genome source.  Unlike
 * SetCompareProcessor, this command will perform the tag scan to prepare for the comparison.
 *
 * The positional parameters are the genome source name (directory or file), the name of a file containing the first
 * genome set, and the name of a file containing the second genome set.  Both of these latter files should be tab-delimited
 * with the genome IDs in the first column.
 *
 * A temporary directory is used to hold the tags.  This directory is ALWAYS erased before processing.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 * -o	output file for report, if not STDOUT
 *
 * --absent		maximum fraction of genomes in a set that can have an absent tag (default 0.2)
 * --present	minimum fraction of genomes in a set that can have a present tag (default 0.8)
 * --tags		type of feature scanner to use (default ROLE)
 * --roles		name of the role definition file (default "roles.in.subsystems" in the current directory)
 * --temp		name of a temporary directory to hold the tag scan results (default "Temp" in the current directory)
 * --keep		if specified, the tag scan results will be kept after processing; otherwise, the temporary directory will be erased
 *
 * @author Bruce Parrello
 *
 */
public class SetCompareFullProcessor extends BaseGenomeProcessor implements FeatureScanner.IParms {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(SetCompareFullProcessor.class);
    /** tag directory controller */
    private TagDirectory tagDir;
    /** comparison engine */
    private GroupCompareEngine compareEngine;
    /** first genome set */
    private Set<String> genomeSet1;
    /** second genome set */
    private Set<String> genomeSet2;
    /** feature scanner for computing tags */
    private FeatureScanner scanner;
    /** output print writer */
    private PrintWriter writer;

    // COMMAND-LINE OPTIONS

    /** maximum fraction for absent tags */
    @Option(name = "--absent", metaVar = "0.1", usage = "maximum fraction of genomes in a set that can have an absent tag")
    private double maxAbsent;

    /** minimum fraction for present tags */
    @Option(name = "--present", metaVar = "0.9", usage = "minimum fraction of genomes in a set that can have a present tag")
    private double minPresent;

    /** type of tag scanner */
    @Option(name = "--tags", usage = "type of feature scanner for computing tags")
    private FeatureScanner.Type scanType;

    /** name of the role definition file */
    @Option(name = "--roles", metaVar = "roles.in.subsystems", usage = "name of the role definition file for role-based tags")
    private File roleFile;

    /** name of the temporary directory for the tag files */
    @Option(name = "--temp", metaVar = "TagDir", usage = "name of the temporary directory for tag files")
    private File tempDir;

    /** if specified, the temporary tag directory will not be erased after processing */
    @Option(name = "--keep", usage = "if specified, the temporary tag directory will not be erased after processing")
    private boolean keepFlag;

    /** report output file (if not STDOUT) */
    @Option(name = "-o", aliases = { "--output" }, usage = "output file for report (if not STDOUT)")
    private File outFile;

    /** name of the file containing the genome IDs in the first set */
    @Argument(index = 1, metaVar = "genomeList1.tbl", usage = "tab-delimited file with headers containing first-set genome IDs in column 1",
            required = true)
    private File genomeList1File;

    /** name of the file containing the genome IDs in the first set */
    @Argument(index = 2, metaVar = "genomeList2.tbl", usage = "tab-delimited file with headers containing second-set genome IDs in column 1",
            required = true)
    private File genomeList2File;

    @Override
    protected void setSourceDefaults() {
        this.maxAbsent = 0.2;
        this.minPresent = 0.8;
        this.keepFlag = false;
        File curDir = new File(System.getProperty("user.dir"));
        this.roleFile = new File(curDir, "roles.in.subsystems");
        this.scanType = FeatureScanner.Type.ROLE;
        this.tempDir = new File(curDir, "Temp");
        this.outFile = null;
        this.setLevel(P3Genome.Details.STRUCTURE_ONLY);
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // Process the two genome sets.
        log.info("Loading input sets from {} and {}.", this.genomeList1File, this.genomeList2File);
        this.genomeSet1 = TabbedLineReader.readSet(this.genomeList1File, "1");
        this.genomeSet2 = TabbedLineReader.readSet(this.genomeList2File, "1");
        // Validate the first genome set.
        for (String genomeId : this.genomeSet1) {
            if (! this.isAvailable(genomeId))
                throw new IOException(genomeId + " is not present in the genome source.");
            else if (this.genomeSet2.contains(genomeId))
                throw new IOException(genomeId + " is in both input sets.");
        }
        // Validate the second genome set.
        for (String genomeId : this.genomeSet2) {
            if (! this.isAvailable(genomeId))
                throw new IOException(genomeId + " is not present in the genome source.");
        }
        // Create the feature scanner.
        log.info("Creating feature scanner of type {}.", this.scanType);
        this.scanner = this.scanType.create(this);
        // Set up the temporary tag directory.
        if (! this.tempDir.isDirectory()) {
            log.info("Creating temporary tag directory {}.", this.tempDir);
            FileUtils.forceMkdir(this.tempDir);
        } else {
            log.info("Erasing temporary tag directory {}.", this.tempDir);
            FileUtils.cleanDirectory(this.tempDir);
        }
        // Build the tag directory controller.
        this.tagDir = new TagDirectory(this.tempDir);
        // Build the comparison engine.
        this.compareEngine = new GroupCompareEngine(this.tagDir, this.maxAbsent, this.minPresent);
        // Now create the output print writer.
        if (this.outFile == null) {
            this.writer = new PrintWriter(System.out);
            log.info("Report will be written to the standard output.");
        } else {
            this.writer = new PrintWriter(this.outFile);
            log.info("Report will be written to " + this.outFile);
        }
    }

    @Override
    protected void runCommand() throws Exception {
        try {
            // First we build the tag files from the genome sets.
            log.info("Scanning {} genomes from first set.", this.genomeSet1.size());
            this.scanGenomes(this.genomeSet1);
            log.info("Scanning {} genomes from second set.", this.genomeSet2.size());
            this.scanGenomes(this.genomeSet2);
            // Now use the compare engine to compare the sets.
            log.info("Processing comparison.");
            this.compareEngine.produceDiffReport(this.writer, this.genomeSet1, this.genomeSet2);
        } finally {
            // Insure the output file is closed.
            this.writer.close();
            // Delete the temporary directory if needed.
            if (! this.keepFlag) {
                log.info("Erasing tag files in temporary directory {}.", this.tempDir);
                FileUtils.cleanDirectory(this.tempDir);
            }
        }

    }

    /**
     * Scan the genomes in a genome set for tags.
     *
     * @param genomeSet		set of IDs for the genomes to scan
     *
     * @throws IOException
     */
    private void scanGenomes(Set<String> genomeSet) throws IOException {
        long lastMsg = System.currentTimeMillis();
        int count = 0;
        for (String genomeId : genomeSet) {
            Genome genome = this.getGenome(genomeId);
            this.tagDir.addGenome(genome, this.scanner);
            count++;
            long now = System.currentTimeMillis();
            if (now - lastMsg >= 5000) {
                log.info("{} of {} genomes processed.", count, genomeSet.size());
                lastMsg = now;
            }
        }
    }

    @Override
    public File getRoleFileName() {
        return this.roleFile;
    }

}
