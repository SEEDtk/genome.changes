package org.theseed.genome.changes;

import java.util.Arrays;

import org.theseed.basic.BaseProcessor;

/**
 * Commands for analyzing differences between features in sets of genomes..
 *
 *	buildTags		build a tag directory for a genome source
 *  buildTax		build taxonomy lists for a genome source
 *
 */
public class App
{
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
        default:
            throw new RuntimeException("Invalid command " + command);
        }
        // Process it.
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
