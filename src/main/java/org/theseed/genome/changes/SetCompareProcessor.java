/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.protein.tags.GroupCompareEngine;
import org.theseed.protein.tags.TagDirectory;

/**
 * This command performs a what's-changed comparison on two genome sets.  Like the TaxonAnalysisProcessor, it relies on
 * a pre-built tag directory.
 *
 * The two sets are described by two tab-delimited files with the genome IDs in the first column.
 *
 * The positional parameters are the name of the tag directory followed by the names of the two set files.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -o	output file for report (if not STDOUT)
 *
 * --absent		maximum fraction of genomes in a set that can have an absent tag (default 0.2)
 * --present	minimum fraction of genomes in a set that can have a present tag (default 0.8)
 *
 * @author Bruce Parrello
 *
 */
public class SetCompareProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(SetCompareProcessor.class);
    /** comparison engine */
    private GroupCompareEngine compareEngine;
    /** tag directory controller */
    private TagDirectory tagDir;
    /** first genome set */
    private Set<String> genomeSet1;
    /** second genome set */
    private Set<String> genomeSet2;

    // COMMAND-LINE OPTIONS

    /** maximum fraction for absent tags */
    @Option(name = "--absent", metaVar = "0.1", usage = "maximum fraction of genomes in a set that can have an absent tag")
    private double maxAbsent;

    /** minimum fraction for present tags */
    @Option(name = "--present", metaVar = "0.9", usage = "minimum fraction of genomes in a set that can have a present tag")
    private double minPresent;

    /** name of the tag directory */
    @Argument(index = 0, metaVar = "tagDir", usage = "name of the tag directory", required = true)
    private File tagDirName;

    /** name of the file containing the genome IDs in the first set */
    @Argument(index = 1, metaVar = "genomeList1.tbl", usage = "tab-delimited file with headers containing first-set genome IDs in column 1",
            required = true)
    private File genomeList1File;

    /** name of the file containing the genome IDs in the first set */
    @Argument(index = 2, metaVar = "genomeList2.tbl", usage = "tab-delimited file with headers containing second-set genome IDs in column 1",
            required = true)
    private File genomeList2File;

    @Override
    protected void setReporterDefaults() {
        this.maxAbsent = 0.2;
        this.minPresent = 0.8;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Verify the genome set files.
        this.genomeSet1 = TabbedLineReader.readSet(genomeList1File, "1");
        this.genomeSet2 = TabbedLineReader.readSet(genomeList2File, "1");
        log.info("{} genomes in set 1, {} in set 2.", this.genomeSet1.size(), this.genomeSet2.size());
        // Insure the sets don't overlap.
        Optional<String> duplicate = this.genomeSet1.stream().filter(x -> this.genomeSet2.contains(x)).findAny();
        if (duplicate.isPresent())
            throw new ParseFailureException(duplicate.get() + " is present in both sets.");
        // Set up the tag directory.
        if (! this.tagDirName.isDirectory())
            throw new FileNotFoundException("Tag directory is not found or invalid.");
        this.tagDir = new TagDirectory(this.tagDirName);
        log.info("{} genomes found in tag directory {}.", this.tagDir.size(), this.tagDirName);
        // Verify that all the genomes are in the tag directory.
        Optional<String> missing = this.genomeSet1.stream().filter(x -> ! this.tagDir.isInDirectory(x)).findAny();
        if (missing.isEmpty())
            missing = this.genomeSet2.stream().filter(x -> ! this.tagDir.isInDirectory(x)).findAny();
        if (missing.isPresent())
            throw new ParseFailureException(missing.get() + " is not present in the tag directory.");
        // Now construct the comparison engine.
        log.info("Building comparison engine.");
        this.compareEngine = new GroupCompareEngine(this.tagDir, this.maxAbsent, this.minPresent);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Process the comparison.
        this.compareEngine.produceDiffReport(writer, genomeSet1, genomeSet2);
    }

}
