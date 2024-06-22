/**
 *
 */
package org.theseed.protein.tags;

/**
 * This object compares two genome sets.  In the constructor, we specify a genome source and a feature analysis method
 * that returns a set of category strings (tags) for each feature in a genome.  For example, one method might return
 * the protein family and another might return a set of roles.  For each set, we catalog the tags that are present in
 * most of the genomes in the set, where "most" is defined by a tuning parameter in the constructor.  We then remove
 * tags that are more than minimally present in the other set. Again, this is also defined by a tuning parameter in the
 * constructor.  The remaining tags differentiate the first set from the second.  This procedure is then reversed to find
 * the differentiating tags for the second set.
 *
 * @author Bruce Parrello
 *
 */
public class CompareGenomeSets {

    // FIELDS

    // TODO data members for CompareGenomeSets

    // TODO constructors and methods for CompareGenomeSets
}
