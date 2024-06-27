/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.protein.tags.TaxonCompare;

/**
 * This command uses a pre-built taxonomic list directory and tag directory to do a taxonomic differential analysis.  Essentially,
 * for each taxonomic grouping, it will output the tags that distinguish it from its siblings in the taxon tree.
 *
 * The positional parameters are the names of the taxonomic list directory and the tag directory, respectively.  These should have
 * been built by BuildTagDirectoryProcessor and BuildTaxonomyListProcessor, respectively, from the same genome source.  If they
 * were not built from the same source, many terrible things will happen.
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
public class TaxonAnalysisProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxonAnalysisProcessor.class);
    /** taxonomic comparison engine */
    private TaxonCompare compareEngine;

    // COMMAND-LINE OPTIONS

    /** maximum fraction for absent tags */
    @Option(name = "--absent", metaVar = "0.1", usage = "maximum fraction of genomes in a set that can have an absent tag")
    private double maxAbsent;

    /** minimum fraction for present tags */
    @Option(name = "--present", metaVar = "0.9", usage = "minimum fraction of genomes in a set that can have a present tag")
    private double minPresent;

    /** name of the taxonomic list directory */
    @Argument(index = 0, metaVar = "taxDir", usage = "name of the taxonomic list directory", required = true)
    private File taxDir;

    /** name of the tag directory */
    @Argument(index = 1, metaVar = "tagDir", usage = "name of the tag directory", required = true)
    private File tagDir;

    @Override
    protected void setReporterDefaults() {
        this.maxAbsent = 0.2;
        this.minPresent = 0.8;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Set up the comparison engine.  This also does all the validation.
        this.compareEngine = new TaxonCompare(this.taxDir, this.tagDir, this.maxAbsent, this.minPresent);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Get the differentiating tags from as many taxonomic groupings as we can.
        log.info("Processing differentiation using maxAbsent = {} and minPresent = {}.", this.maxAbsent, this.minPresent);
        Map<Integer, Set<String>> diffMap = this.compareEngine.computeDistinguishingTags();
        // Retrieve the group names.
        log.info("Retrieving group names for {} taxonomic IDs.", diffMap.size());
        Map<Integer, String> nameMap = this.compareEngine.getNameMap(diffMap.keySet());
        // Now write the output report.
        log.info("Writing report.");
        writer.println("tax_id\tname\ttags");
        for (var diffEntry : diffMap.entrySet()) {
            int taxId = diffEntry.getKey();
            String name = nameMap.getOrDefault(taxId, "<< unknown >>");
            String tags = StringUtils.join(diffEntry.getValue(), ',');
            writer.println(taxId + "\t" + name + "\t" + tags);
        }
    }

}
