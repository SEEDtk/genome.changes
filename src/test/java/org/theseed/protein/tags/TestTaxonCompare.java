/**
 *
 */
package org.theseed.protein.tags;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.protein.tags.scanner.FeatureScanner;
import org.theseed.taxonomy.TaxonListDirectory;
import org.theseed.utils.SetPair;

/**
 * @author Bruce Parrello
 *
 */
class TestTaxonCompare implements FeatureScanner.IParms {

    @Test
    void testTaxComparePipeline() throws IOException, ParseFailureException {
        File dataDir = new File("data");
        File taxDirName = new File(dataDir, "taxTest");
        File tagDirName = new File(dataDir, "tagTest");
        // Reset the file system state.
        if (taxDirName.isDirectory())
            FileUtils.deleteDirectory(taxDirName);
        if (tagDirName.isDirectory())
            FileUtils.deleteDirectory(tagDirName);
        // Create the feature scanner.
        FeatureScanner scanner = FeatureScanner.Type.ROLE.create(this);
        // Connect to the genome source.
        GenomeSource source = GenomeSource.Type.DIR.create(dataDir);
        // Create the taxon list directory.
        TaxonListDirectory taxDir = new TaxonListDirectory(taxDirName);
        taxDir.updateRankMaps(source);
        // Create the tag directory.
        TagDirectory tagDir = new TagDirectory(tagDirName);
        for (Genome genome : source)
            tagDir.addGenome(genome, scanner);
        // Now we create the compare object.
        TaxonCompare compareEngine = new TaxonCompare(taxDirName, tagDirName, 0.2, 0.8);
        Map<Integer, Set<String>> taxonTagMap = compareEngine.computeDistinguishingTags();
        // Verify that we have comparison sets for all the groups with siblings.
        Map<Integer, Set<Integer>> taxTree = taxDir.getTaxTree();
        for (var taxEntry : taxTree.entrySet()) {
            int parent = taxEntry.getKey();
            Set<Integer> children = taxEntry.getValue();
            if (children.size() >= 2) {
                for (int child : children) {
                    String label = "Child " + child + " of " + parent;
                    Set<String> diffs = taxonTagMap.get(child);
                    assertThat(label, diffs, not(nullValue()));
                }
            }
        }
        // Now we do a spot check.
        Set<Integer> taxes = Set.of(1236, 28216);
        Map<Integer, Set<String>> gSetMap = taxDir.getGenomeSets(taxes);
        Set<String> g1236 = gSetMap.get(1236);
        Set<String> g28216 = gSetMap.get(28216);
        GroupCompareEngine compareTester = new GroupCompareEngine(tagDir, 0.2, 0.8);
        SetPair<String> diffs = compareTester.distinguish(g1236, g28216);
        Set<String> tags1236 = taxonTagMap.get(1236);
        assertThat(tags1236, equalTo(diffs.getSet1()));
        Set<String> tags28216 = taxonTagMap.get(28216);
        assertThat(tags28216, equalTo(diffs.getSet2()));
    }

    @Override
    public File getRoleFileName() {
        return new File("data", "roles.in.subsystems");
    }

}
