package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.protein.tags.GroupCompareEngine;
import org.theseed.protein.tags.TagDirectory;
import org.theseed.utils.BaseMultiReportProcessor;

/**
 * This command is designed to allow a mass compare operation on genome set pairs in a directory using a single master
 * tag directory.
 * 
 * Each genome set is represented by a two-column tab-delimited file, with the first column containing genome IDs and
 * the second containing genome names. The sets are arranged in pairs, with each file in the pair having the same name
 * except for a one-word difference. The two words that differentiate files in the pairs are specified as parameters to
 * this command. So, for example, if the words were "left" and "right", then the files "set1.left.tbl" and "set1.right.tbl"
 * would form a pair for comparison.
 * 
 * All files should have a ".tbl" extension.
 * 
 * This is a crude technique, but it works for the tests we've designed so far.
 * 
 * The positional parameters should be the name of tag directory, the name of the input directory containing the
 * genome set files, and finally the two words that differentiate the file pairs. The compare reports will 
 * be sent to the output directory, with the differentiation word replaced by "compare" in the file name. (So, the 
 * compare between "set1.left.tbl" and "set1.right.tbl" would be output as "set1.compare.tbl".)
 * 
 * The command-line options are as follows:
 * 
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -D	output directory for reports (default is "Compares" in the current directory)
 *
 * --absent		maximum fraction of genomes in a set that can have an absent tag (default 0.2)
 * --present	minimum fraction of genomes in a set that can have a present tag (default 0.8)
 * --clear      erase the output directory before processing
 */
public class MassCompareProcessor extends BaseMultiReportProcessor {

    // FIELDS
    /* logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MassCompareProcessor.class);
    /** comparison engine */
    private GroupCompareEngine compareEngine;
    /** tag directory controller */
    private TagDirectory tagDir;
    /** map of genome set left file names to genome set right file names */
    private Map<String, String> fileMap; 

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

    /** name of the input directory containing the genome set files */
    @Argument(index = 1, metaVar = "inputDir", usage = "name of the input directory containing the genome set files", required = true)
    private File inputDir;

    /** first differentiation word */
    @Argument(index = 2, metaVar = "word1", usage = "first differentiation word", required = true)
    private String word1;

    /** second differentiation word */
    @Argument(index = 3, metaVar = "word2", usage = "second differentiation word", required = true)
    private String word2;

    @Override
    protected File setDefaultOutputDir(File curDir) {
        return new File(curDir, "Compares");
    }

    @Override
    protected void setMultiReportDefaults() {
        this.maxAbsent = 0.2;
        this.minPresent = 0.8;
    }

    @Override
    protected void validateMultiReportParms() throws IOException, ParseFailureException {
        // Insure the tag directory exists
        if (! this.tagDirName.isDirectory())
            throw new IOException("Tag directory " + this.tagDirName + " is not found or inalid.");
        // Verify that the differentiation words are distinct.
        if (this.word1.equals(this.word2))
            throw new ParseFailureException("Differentiation words must be distinct.");
        // Insure the input directory exists
        if (! this.inputDir.isDirectory())
            throw new IOException("Input directory " + this.inputDir + " is not found or invalid.");
        // Load the tag directory.
        log.info("Loading tag directory {}.", this.tagDirName);
        this.tagDir = new TagDirectory(this.tagDirName);
        // Construct the comparison engine.
        log.info("Building comparison engine.");
        this.compareEngine = new GroupCompareEngine(this.tagDir, this.maxAbsent, this.minPresent);
        // Now we must find the input files in the input directory.
        File[] inputFiles = this.inputDir.listFiles((dir, name) -> name.endsWith(".tbl"));
        if (inputFiles == null || inputFiles.length == 0)
            throw new IOException("No input files found in directory " + this.inputDir);
        // Loop through the input files, sorting them into pairs.
        this.fileMap = new TreeMap<>();
        for (File inputFile : inputFiles) {
            String fileName = inputFile.getName();
            if (inputFile.isFile() &&fileName.contains(this.word1)) {
                // Here we have a valid left input file.
                String otherFileName = fileName.replace(this.word1, this.word2);
                File otherFile = new File(this.inputDir, otherFileName);
                if (otherFile.isFile()) {
                    // Here we have a valid pair of input files.
                    this.fileMap.put(fileName, otherFileName);
                }
            }
        }
        if (this.fileMap.isEmpty())
            throw new IOException("No valid input file pairs found in directory " + this.inputDir);
        log.info("Found {} valid input file pairs.", this.fileMap.size());
    }

    @Override
    protected void runMultiReports() throws Exception {
        // Loop through the input file pairs and produce comparison reports for each.
        int pairCount = 0;
        for (Map.Entry<String, String> entry : this.fileMap.entrySet()) {
            String leftFileName = entry.getKey();
            String rightFileName = entry.getValue();
            log.info("Processing file pair {} and {}.", leftFileName, rightFileName);
            // Compute the three file names for the comparison report.
            String reportFileName = leftFileName.replace(this.word1, "compare");
            File leftFile = new File(this.inputDir, leftFileName);
            File rightFile = new File(this.inputDir, rightFileName);
            File reportFile = this.getOutFile(reportFileName);
            Set<String> leftGenomes = TabbedLineReader.readSet(leftFile, "1");
            Set<String> rightGenomes = TabbedLineReader.readSet(rightFile, "1");
            log.info("Read {} genomes from {} and {} genomes from {}.", 
                    leftGenomes.size(), leftFileName, rightGenomes.size(), rightFileName);
            // Here you would call the method to produce the comparison report for this file pair.
            try (PrintWriter writer = new PrintWriter(reportFile)) {
                this.compareEngine.produceDiffReport(writer, leftGenomes, rightGenomes);
            }
            pairCount++;
        }
        log.info("Processed {} file pairs.", pairCount);
    }

    

}
