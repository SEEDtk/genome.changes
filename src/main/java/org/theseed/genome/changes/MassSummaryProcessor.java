package org.theseed.genome.changes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Argument;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.proteins.RoleMap;

/**
 * This command examines the output of a mass compare operation and creates a summary report of the results.
 * 
 * For each comparison in the mass compare output, it will list the left-only and right-only tags. The
 * tags must be role IDs, and the summary will contain the role names from the role definition file.
 * 
 * The positional parameters must be the name of the output directory from the mass compare (which
 * is the input to this command), and the name of the role definition file. The report will be written 
 * to the standard output.
 * 
 * The command-line options are as follows:
 * 
 * -h   display command-line usage
 * -v   display more frequent log messages
 * -o   output file for report (if not STDOUT)
 * 
 */
public class MassSummaryProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MassSummaryProcessor.class);
    /** role definition map */
    private RoleMap roleMap;
    /** list of comparison files from the mass compare output */
    private File[] compFiles;

    // COMMAND-LINE OPTIONS

    /** input directory containing mass compare output */
    @Argument(index = 0, metaVar = "inDir", usage = "input directory containing mass compare output")
    private File inDir;

    /** role definition file */
    @Argument(index = 1, metaVar = "roles.in.subsystems", usage = "role definition file")
    private File roleFile;

    @Override
    protected void setReporterDefaults() {
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Validate the input directory.
        if (! this.inDir.isDirectory())
            throw new IOException("Input directory " + this.inDir + " is not valid.");
        this.compFiles = this.inDir.listFiles((dir, name) -> name.endsWith(".tbl"));
        if (this.compFiles == null || this.compFiles.length == 0)
            throw new IOException("No comparison files found in input directory " + this.inDir + ".");
        log.info("{} input files found in {}.", this.compFiles.length, this.inDir);
        // Load the role map.
        if (! this.roleFile.canRead())
            throw new IOException("Role file " + this.roleFile + " is not found or unreadable.");
        this.roleMap = RoleMap.load(this.roleFile);
        log.info("{} role definitions loaded from {}.", this.roleMap.size(), this.roleFile);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // 
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'runReporter'");
    }

}
