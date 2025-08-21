/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.p3api.P3Genome;
import org.theseed.taxonomy.TaxonListDirectory;

/**
 * This command will build taxonomy lists from a genome source.  For each major taxonomic rank, it will create lists of the
 * genome IDs for each taxonomic grouping of that rank and a representation of the taxonomy tree for those ranks.
 *
 * The positional parameters are the name of the genome source (directory or file) and the name of the output directory.
 * If the output directory already exists, the new genomes will be added.  If a genome already in the directory has had
 * its lineage changed, this will cause chaos, so use "--clear" if there is a risk of that.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 *
 * --clear		erase the output directory before processing
 *
 * @author Bruce Parrello
 *
 */
public class BuildTaxonomyListProcessor extends BaseGenomeProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(BuildTaxonomyListProcessor.class);
    /** taxon list directory controller */
    private TaxonListDirectory taxDir;

    // COMMAND-LINE OPTIONS

    /** if specified, the output directory will be erased before processing */
    @Option(name = "--clear", usage = "if specified, the output directory will be erased before processing")
    private boolean clearFlag;

    /** output directory name */
    @Argument(index = 1, metaVar = "outDir", usage = "output directory name")
    private File outDir;

    @Override
    protected void setSourceDefaults() {
        this.clearFlag = false;
        this.setLevel(P3Genome.Details.STRUCTURE_ONLY);
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        if (this.outDir.isDirectory() && this.clearFlag) {
            // Here we need to erase the output directory.
            log.info("Erasing output directory {}.", this.outDir);
            FileUtils.cleanDirectory(this.outDir);
        }
        this.taxDir = new TaxonListDirectory(this.outDir);
    }

    @Override
    protected void runCommand() throws Exception {
        // Update the directory with the genomes in the source.
        this.taxDir.updateRankMaps(this.getSource());
    }

}
