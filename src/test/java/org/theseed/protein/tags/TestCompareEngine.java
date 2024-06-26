/**
 *
 */
package org.theseed.protein.tags;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.protein.tags.scanner.FeatureScanner;
import org.theseed.utils.SetPair;

/**
 * @author Bruce Parrello
 *
 */
class TestCompareEngine implements FeatureScanner.IParms {

    Set<String> GENOMES1 = Set.of("487.3695", "487.3696", "487.3699" , "487.3700" ,"487.3705");
    Set<String> GENOMES2 = Set.of("727.1530", "727.1531", "727.1536", "727.1539", "727.1540", "1028307.3",
            "107806.10", "511145.12", "685038.3");

    @Test
    void testGroupCompareEngine() throws IOException, ParseFailureException {
        // Create a tag directory for all the genomes.
        GenomeSource genomes = GenomeSource.Type.DIR.create(new File("data"));
        FeatureScanner scanner = FeatureScanner.Type.ROLE.create(this);
        File tagDir = new File("data", "tagTest");
        if (tagDir.isDirectory())
            FileUtils.deleteDirectory(tagDir);
        TagDirectory tags = new TagDirectory(tagDir);
        for (Genome genome : genomes)
            tags.addGenome(genome, scanner);
        // Set up tag counts for the two sets to verify the tag data later.
        TagCounts tags1 = getSetTags(tags, GENOMES1);
        TagCounts tags2 = getSetTags(tags, GENOMES2);
        // Now we have all the tags set up.
        GroupCompareEngine testEngine = new GroupCompareEngine(tags, 0.2, 0.8);
        SetPair<String> tagSets1 = testEngine.countTags(GENOMES1);
        // We will verify the presence/semi-presence for this first tag set.  Absence is 1 or less,
        // presence 4 or more.
        for (String pTag : tagSets1.getSet1()) {
            int count1 = tags1.getCount(pTag);
            assertThat(pTag, count1, greaterThanOrEqualTo(4));
        }
        for (String pTag : tagSets1.getSet2()) {
            int count2 = tags1.getCount(pTag);
            assertThat(pTag, count2, greaterThan(1));
            assertThat(pTag, count2, lessThan(4));
        }
        // Get the second tag set.
        SetPair<String> tagSets2 = testEngine.countTags(GENOMES2);
        // Verify it has the key protein.
        assertThat(tagSets2.getSet1(), hasItem("PhenTrnaSyntAlph"));
        assertThat(tagSets2.getSet2(), not(hasItem("PhenTrnaSyntAlph")));
        // Compute the distinguishing set.
        SetPair<String> diff = testEngine.distinguish(tagSets1, tagSets2);
        // Test the left set.
        Set<String> diff1 = diff.getSet1();
        for (String dTag : diff1) {
            assertThat(dTag, tags1.getCount(dTag), greaterThanOrEqualTo(4));
            assertThat(dTag, tags2.getCount(dTag), lessThanOrEqualTo(1));
        }
        // Test the right set.
        Set<String> diff2 = diff.getSet2();
        for (String dTag : diff2) {
            assertThat(dTag, tags2.getCount(dTag), greaterThanOrEqualTo(8));
            assertThat(dTag, tags1.getCount(dTag), lessThanOrEqualTo(1));
        }
        // Now try again without the pre-computation.
        SetPair<String> newDiff = testEngine.distinguish(GENOMES1, GENOMES2);
        assertThat(newDiff.getSet1(), equalTo(diff.getSet1()));
        assertThat(newDiff.getSet2(), equalTo(diff.getSet2()));
        // Now try for the left only with the tag count interface.
        TagCounts counts1 = tags.getTagCounts(GENOMES1);
        TagCounts counts2 = tags.getTagCounts(GENOMES2);
        Set<String> newDiff1 = testEngine.distinguishLeft(counts1, GENOMES1.size(), counts2, GENOMES2.size());
        assertThat(newDiff1, equalTo(diff1));
    }

    /**
     * Compute the tag counts for a genome set.
     *
     * @param tags		master tag directory
     * @param genomes	set of IDs for genomes to count
     *
     * @return the tag counts for the genome set
     *
     * @throws IOException
     */
    public TagCounts getSetTags(TagDirectory tags, Set<String> genomes) throws IOException {
        TagCounts retVal = new TagCounts();
        for (String genomeId : genomes) {
            Set<String> tags1Genome = tags.getGenome(genomeId);
            retVal.count(tags1Genome);
        }
        return retVal;
    }

    @Override
    public File getRoleFileName() {
        return new File("data", "roles.in.subsystems");
    }

}
