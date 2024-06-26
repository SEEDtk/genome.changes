/**
 *
 */
package org.theseed.protein.tags;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.utils.SetPair;

/**
 * This object compares the genomes in two sets and outputs the distinguishing tags based on precomputed tag sets in
 * a TagDirectory.
 *
 * A tag is distinguishing if it occurs in most genomes in one set and few genomes in the other.  In this
 * case, the proportions required to qualify for "most" or "few" are specified as tuning parameters on
 * the constructor.  The tuning parameters and the TagDirectory remain inviolate.  The client can then
 * specify different genome set pairs and extract the two distinguishing tag sets.
 *
 * @author Bruce Parrello
 *
 */
public class GroupCompareEngine {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GroupCompareEngine.class);
    /** tag directory object */
    private TagDirectory tagDir;
    /** maxmimum fraction of a set to qualify as absent */
    private double maxAbsent;
    /** minimum fraction of a set to quality as present */
    private double minPresent;

    /**
     * Construct a new compare-groups object.
     *
     * @param tagSource		TagDirectory object for loading the tags
     * @oaram absent		maximum fraction of a set allowed for an absent tag
     * @param present		minimum fraction of a set allowed for a present tag
     *
     * @throws ParseFailureException
     */
    public GroupCompareEngine(TagDirectory tagSource, double absent, double present) throws ParseFailureException {
        // Validate the tuning parameters.
        if (absent < 0.0 || absent >= 1.0)
            throw new ParseFailureException("Fraction of " + absent + " specified for absence detection must be between 0 and 1.");
        if (present <= 0.0 || present > 1.0)
            throw new ParseFailureException("Fraction of " + present + " specified for presence detection must be between 0 and 1.");
        this.maxAbsent = absent;
        this.minPresent = present;
        this.tagDir = tagSource;
    }

    /**
     * Compute the present and semi-present tags for the specified genome set.  The semi-present tags
     * are those that are not frequent enough to be present, but too frequent to be absent.  A
     * tag is distinguishing if it is present in one genome set but neither present nor semi-present
     * in the other.
     *
     * @param set	genome set whose tags are to be counted
     *
     * @return a set pair consisting of the present tags and the semi-present tags, respectively
     *
     * @throws IOException
     */
    public SetPair<String> countTags(Set<String> set) throws IOException {
        TagCounts counts = new TagCounts();
        // Loop through the genome set.
        for (String genome : set) {
            Set<String> tags = this.tagDir.getGenome(genome);
            counts.count(tags);
        }
        // Now we want to separate out the presence and absence sets.
        Set<String> present = new HashSet<String>();
        Set<String> semi = new HashSet<String>();
        // Compute the presence and absence thresholds for the set.
        int setPresent = (int) Math.ceil(set.size() * this.minPresent);
        int setAbsent = (int) Math.floor(set.size() * this.maxAbsent);
        for (var counter : counts.getAllCounts()) {
            int count = counter.getValue().getValue();
            if (count >= setPresent)
                present.add(counter.getKey());
            else if (count > setAbsent)
                semi.add(counter.getKey());
        }
        // Return the two sets.
        return new SetPair<String>(present, semi);
    }

    /**
     * Compare two genome sets, and return the distinguishing tags for each.
     *
     * @param leftSet	genome IDs for the first set
     * @param rightSet	genome IDs for the second set
     *
     * @return a pair of tag sets, representing the distinguishing tags for the left and right genomes, respectively
     *
     * @throws IOException
     */
    public SetPair<String> distinguish(Set<String> leftSet, Set<String> rightSet) throws IOException {
        // Create the tag counts for the left and right sets.
        SetPair<String> leftTags = this.countTags(leftSet);
        SetPair<String> rightTags = this.countTags(rightSet);
        // Compare the tags.
        SetPair<String> retVal = this.distinguish(leftTags, rightTags);
        return retVal;
    }

    /**
     * Compare two genome sets, and return the distinguishing tags for each.  This method takes as input
     * two set pairs, one for the left genome set and one for the right.  Each set pair consists of the
     * set of present tags and the set of semi-present tags, respectively.  (A tag is semi-present
     * if it is neither frequent enough to be present nor rare enough to be absent.)
     *
     * @param leftTags		present/semi-present set pair for the left genome set
     * @param rightTags		present/semi-present set pair for the right genome set
     *
     * @return a pair of sets, indicating the distingushing tags for the left and right sets, respectively
     */
    public SetPair<String> distinguish(SetPair<String> leftTags, SetPair<String> rightTags) {
        // The distinguishing tags on the left are present on the left but absent on the right.
        Set<String> left = this.computeDistinguishing(leftTags, rightTags);
        // The distinguishing tags on the right are present on the right but absent on the left.
        Set<String> right = this.computeDistinguishing(rightTags, leftTags);
        // Return the pair of sets.
        return new SetPair<String>(left, right);
    }

    /**
     * Compute the tags that distinguish the first set.  A tag is distinguishing if it is present in the first
     * set but absent in the second.  Each incoming set pair contains present and semi-present tags (which are
     * non-intersecting sets).  A tag is absent if it is neither present nor semi-present.
     *
     * @param set1tags		present/semi-present set pair for the target genome set
     * @param set2Tags		present/semi-present set pair for the other genome set
     *
     * @return the set of distinguishing tags for the first set
     */
    private Set<String> computeDistinguishing(SetPair<String> set1Tags, SetPair<String> set2Tags) {
        Set<String> retVal = new HashSet<String>(set1Tags.getSet1());
        retVal.removeAll(set2Tags.getSet1());
        retVal.removeAll(set2Tags.getSet2());
        return retVal;
    }

}
