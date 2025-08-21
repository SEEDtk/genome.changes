/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.p3api.P3Genome;
import org.theseed.protein.tags.TagDirectory;
import org.theseed.protein.tags.scanner.FeatureScanner;
import org.theseed.protein.tags.scanner.FeatureScanner.IParms;

/**
 * This command creates a tag directory from a genome source.  The positional parameters are the name of the
 * genome source (file or directory) and the name of the output tag directory.  The command-line options are
 * as follows.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 *
 * --missing	do not overwrite genomes already in the output directory
 * --clear		erase the output directory before processing
 * --tags		type of feature scanner to use (default ROLE)
 * --roles		name of the role definition file (default "roles.in.subsystems" in the current directory)
 *
 * @author Bruce Parrello
 *
 */
public class BuildTagDirectoryProcessor extends BaseGenomeProcessor implements IParms {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(BuildTagDirectoryProcessor.class);
    /** tag directory to create */
    private TagDirectory tagController;
    /** feature scanner to use */
    private FeatureScanner scanner;

    // COMMAND-LINE OPTIONS

    /** if specified, pre-existing tag files will not be overwritten */
    @Option(name = "--missing", usage = "if specified, pre-existing tag files will not be overwritten")
    private boolean missingFlag;

    /** if specified, the output directory will be erased before processing */
    @Option(name = "--clear", usage = "if specified, the output directory will be erased before processing")
    private boolean clearFlag;

    /** type of tag scanner */
    @Option(name = "--tags", usage = "type of feature scanner for computing tags")
    private FeatureScanner.Type scanType;

    /** name of the role definition file */
    @Option(name = "--roles", metaVar = "roles.in.subsystems", usage = "name of the role definition file for role-based tags")
    private File roleFile;

    /** name of the output directory */
    @Argument(index = 1, metaVar = "tagDir", usage = "name of output tag directory", required = true)
    private File tagDir;

    @Override
    protected void setSourceDefaults() {
        this.missingFlag = false;
        this.clearFlag = false;
        this.scanType = FeatureScanner.Type.ROLE;
        this.setLevel(P3Genome.Details.STRUCTURE_ONLY);
        this.roleFile = new File(System.getProperty("user.dir"), "roles.in.subsystems");
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // if the tag directory exists and we are clearing, erase it here.
        if (this.tagDir.isDirectory() && this.clearFlag) {
            log.info("Erasing output tag directory {}.", this.tagDir);
            FileUtils.cleanDirectory(this.tagDir);
        }
        // Create the feature scanner.
        this.scanner = this.scanType.create(this);
        // Create the tag controller for the output directory.
        this.tagController = new TagDirectory(this.tagDir);
    }

    @Override
    protected void runCommand() throws Exception {
        // Loop through the genomes in the source.
        Set<String> genomeIds = this.getGenomeIds();
        final int nGenomes = genomeIds.size();
        int gCount = 0;
        int skipped = 0;
        for (String genomeId : genomeIds) {
            gCount++;
            if (this.missingFlag && this.tagController.isInDirectory(genomeId)) {
                // Here we can skip this genome.
                log.info("Genome {} already in output directory.", genomeId);
                skipped++;
            } else {
                log.info("Processing genome {} of {}: {}.", gCount, nGenomes, genomeId);
                Genome genome = this.getGenome(genomeId);
                this.tagController.addGenome(genome, this.scanner);
            }
        }
        log.info("All done. {} genomes processed, {} skipped.", gCount, skipped);
    }

    @Override
    public File getRoleFileName() {
        return this.roleFile;
    }

}
