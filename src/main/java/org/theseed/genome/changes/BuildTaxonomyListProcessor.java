/**
 *
 */
package org.theseed.genome.changes;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.genome.iterator.BaseGenomeProcessor;

/**
 * This command will build taxonomy lists from a genome source.  For each named taxonomic rank, it will create lists of the
 * genome IDs for each taxonomic grouping of that rank.  These lists will be stored in a master ouput directory with one
 * sub-directory for each taxonomic rank, and each list contained in a file whose name is the taxonomy number with an extension
 * of ".tbl".
 *
 *
 * @author Bruce Parrello
 *
 */
public class BuildTaxonomyListProcessor extends BaseGenomeProcessor {

    @Override
    protected void setSourceDefaults() {
        // TODO code for setSourceDefaults

    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // TODO code for validateSourceParms

    }

    @Override
    protected void runCommand() throws Exception {
        // TODO code for runCommand

    }
    // FIELDS
    // TODO data members for BuildTaxonomyListProcessor

    // TODO constructors and methods for BuildTaxonomyListProcessor
}
