/**
 *
 */
package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
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
 * The positional parameter is the directory or file containing the genome source.  A separate report will be
 * written for each genome, in a subdirectory with the genome ID as its name.  (This is an odd choice for the
 * sake of a specific project.)  The master output directory is the second positional parameter.
 *
 * A temporary tag directory and a temporary taxonomic tree directory will be used.  The taxonomic tree directory
 * will be re-used if it is nonempty, but the tag directory will always be rebuilt.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
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
    /** map of genome IDs to lineages */
    private Map<String, int[]> lineageMap;
    /** map of genome IDs to names */
    private Map<String, String> gNameMap;

    // COMMAND-LINE OPTIONS

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

    /** output directory name */
    @Argument(index = 1, metaVar = "outDir", usage = "master output directory")
    private File outDir;


    @Override
    protected void setSourceDefaults() {
        File curDir = new File(System.getProperty("user.dir"));
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
        // Validate the output directory.
        if (this.outDir.isDirectory())
            log.info("Output will be to directory {}.", this.outDir);
        else {
            log.info("Creating output directory {}.", this.outDir);
            FileUtils.forceMkdir(this.outDir);
        }
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
            // The first step is to fill the tag directory and create the genome lineage map.
            log.info("Computing tags into {}.", this.tagDir);
            Set<String> genomeIDs = this.getGenomeIds();
            final int hashSize = genomeIDs.size() * 4 / 3 + 1;
            this.lineageMap = new HashMap<String, int[]>(hashSize);
            this.gNameMap = new HashMap<String, String>(hashSize);
            for (String genomeID : genomeIDs) {
                Genome genome = this.getGenome(genomeID);
                log.info("Scanning for tags in {}.", genome);
                this.tagController.addGenome(genome, this.tagScanner);
                this.lineageMap.put(genomeID, genome.getLineage());
                this.gNameMap.put(genomeID, genome.getName());
            }
            Map<Integer, Set<String>> diffMap = this.doCompare();
            // We need a map of taxonomic names.
            Map<Integer, String> nameMap = this.getNameMap(diffMap.keySet());
            // Now we loop through the genomes again.  For each genome, we get its role set and its
            // taxonomy list, and then we query the differentiation map to create an output report.
            for (String genomeId : genomeIDs) {
                String genomeName = this.gNameMap.get(genomeId);
                log.info("Processing differentials for genome {} {}.", genomeId, genomeName);
                // Create the output file.
                File genomeDir = new File(this.outDir, genomeId);
                if (! genomeDir.isDirectory())
                    FileUtils.forceMkdir(genomeDir);
                try (PrintWriter writer = new PrintWriter(new File(genomeDir, "changes.tbl"))) {
                    writer.println("genome_id\tgenome_name\ttax_id\trank\tname\tparent_id\tparent_rank\tparent_name\ttag_name");
                    // Now get all the tags for the genome.  This is already in the tag directory.  The
                    // set we get back is a private copy, which is good because we are going to mess
                    // it up.
                    Set<String> genomeTags = this.tagController.getGenome(genomeId);
                    // Now we loop through the lineage.  For each taxonomic group, we get the differentiating
                    // tags, and output the ones found in this genome.  We will be moving from the smallest
                    // grouping to the largest.
                    int[] lineage = this.lineageMap.get(genomeId);
                    for (int i = lineage.length - 1; i >= 0; i--) {
                        int taxId = lineage[i];
                        int parentId = this.taxonTree.getParent(taxId);
                        // Only proceed if we have a known parent grouping.
                        if (parentId >= 0) {
                            // Get the roles for this child grouping.
                            Set<String> diffTags = diffMap.get(taxId);
                            if (diffTags != null) {
                                // Find the overlapping tags.
                                Set<String> myTags = diffTags.stream().filter(x -> genomeTags.contains(x)).collect(Collectors.toSet());
                                if (! myTags.isEmpty()) {
                                    // Here we have differentiating tags for this genome and this tax ID.
                                    String taxRank = this.taxController.getRank(taxId);
                                    String taxName = nameMap.getOrDefault(taxId, "<unknown>");
                                    String parentRank = this.taxController.getRank(parentId);
                                    String parentName = nameMap.getOrDefault(parentId, "<unknown>");
                                    String taxPrefix = genomeId + "\t" + genomeName + "\t" + taxId + "\t" + taxRank + "\t" + taxName
                                            + "\t" + parentId + "\t" + parentRank + "\t" + parentName + "\t";
                                    // Write one line per tag, deleting tags as we use them.
                                    for (String tag : myTags) {
                                        writer.println(taxPrefix + tagScanner.getTagName(tag));
                                        genomeTags.remove(tag);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (! this.keepFlag) {
                log.info("Erasing tag directory {}.", this.tagDir);
                FileUtils.cleanDirectory(this.tagDir);
            }
        }
    }

    /**
     * This gets a taxonomic name map for all the taxonomic IDs in the specified set and their parents.
     *
     * @param keySet	base set of taxonomic IDs
     *
     * @return a map from the IDs of the incoming taxonomic groups and their parents to the group names
     *
     * @throws IOException
     */
    private Map<Integer, String> getNameMap(Set<Integer> keySet) throws IOException {
        // Get a set and prime it with the incoming group IDs.
        Set<Integer> idSet = new HashSet<Integer>(keySet.size() * 8 / 3 + 1);
        idSet.addAll(keySet);
        // Get the parent IDs.
        for (int childId : keySet) {
            int parentId = this.taxonTree.getParent(childId);
            if (parentId >= 0)
                idSet.add(parentId);
        }
        // Now compute the name map for all of them.
        Map<Integer, String> retVal = this.taxController.getNameMap(idSet);
        return retVal;
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
