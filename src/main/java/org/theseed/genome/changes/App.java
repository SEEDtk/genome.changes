package org.theseed.genome.changes;

import java.util.Arrays;

import org.theseed.basic.BaseProcessor;

/**
 * Commands for analyzing differences between features in sets of genomes..
 *
 *	buildTags		build a tag directory for a genome source
 *  buildTax		build taxonomy lists for a genome source
 *  taxonCompare	find tag differences between taxonomic subgroups of a genome source
 *  setCompare		find tag differences between two non-intersecting subsets of a genome source
 *  setProcess		scan a genome source for tags and find tag differences between two non-intersecting subsets
 *  taxonPipe		build the tag directory and taxonomy lists for a genome source and output differentiating tags
 *
 */
public class App
{
    /** static array containing command names and comments */
    protected static final String[] COMMANDS = new String[] {
             "buildTags", "build a tag directory for a genome source",
             "buildTax", "build taxonomy lists for a genome source",
             "taxonCompare", "find tag differences between taxonomic subgroups of a genome source",
             "setCompare", "find tag differences between two non-intersecting subsets of a genome source",
             "setProcess", "scan a genome source for tags and find tag differences between two non-intersecting subsets",
             "taxonPipe", "build the tag directory and taxonomy lists for a genome source and output differentiating tags"
    };

    public static void main( String[] args )
    {
        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        BaseProcessor processor;
        // Determine the command to process.
        switch (command) {
        case "buildTax" :
            processor = new BuildTaxonomyListProcessor();
            break;
        case "buildTags" :
            processor = new BuildTagDirectoryProcessor();
            break;
        case "taxonCompare" :
            processor = new TaxonAnalysisProcessor();
            break;
        case "setCompare" :
            processor = new SetCompareProcessor();
            break;
        case "setProcess" :
            processor = new SetCompareFullProcessor();
            break;
        case "taxonPipe" :
            processor = new TaxonPipeProcessor();
            break;
        case "-h" :
        case "--help" :
            processor = null;
            break;
        default :
            throw new RuntimeException("Invalid command " + command + ".");
        }
        if (processor == null)
            BaseProcessor.showCommands(COMMANDS);
        else {
            processor.parseCommand(newArgs);
            processor.run();
        }
    }
}
