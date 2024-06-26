/**
 *
 */
package org.theseed.taxonomy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.TaxItem;
import org.theseed.genome.iterator.GenomeSource;

/**
 * @author Bruce Parrello
 *
 */
class TestTaxonListDirectory {

    @Test
    void testTaxonScan() throws IOException, ParseFailureException {
        File taxDir = new File("data", "taxTest");
        if (taxDir.isDirectory())
            FileUtils.deleteDirectory(taxDir);
        TaxonListDirectory taxController = new TaxonListDirectory(taxDir);
        GenomeSource genomes = GenomeSource.Type.DIR.create(new File("data"));
        for (String rank : TaxonListDirectory.RANKS) {
            File rankFile = new File(taxDir, rank + ".tax");
            assertThat(rank, rankFile.canRead(), equalTo(true));
            RankMap rankMap = taxController.getRankMap(rank);
            assertThat(rankMap.size(), equalTo(0));
        }
        taxController.updateRankMaps(genomes);
        File treeFile = new File(taxDir, "tree.links");
        File rankFile = new File(taxDir, "rank.index");
        assertThat(treeFile.canRead(), equalTo(true));
        assertThat(rankFile.canRead(), equalTo(true));
        // This map will help us check the taxonomy tree.
        Map<Integer, String> taxons = new HashMap<Integer, String>();
        // Get the tree itself.
        Map<Integer, Set<Integer>> taxTree = taxController.getTaxTree();
        // Load all the rank maps.  We will loop through the genomes, validating the taxonomy data.
        Map<String, RankMap> rankMapMap = taxController.getAllRankMaps();
        assertThat(rankMapMap.keySet(), containsInAnyOrder(TaxonListDirectory.RANKS));
        for (Genome genome : genomes) {
            Iterator<TaxItem> taxIter = genome.taxonomy();
            while (taxIter.hasNext()) {
                TaxItem taxItem = taxIter.next();
                String rank = taxItem.getRank();
                RankMap rankMap = rankMapMap.get(rank);
                String label = genome.getId() + " rank " + rank;
                if (rankMap != null) {
                    // Validate the taxonomy group.
                    var taxData = rankMap.getTaxData(taxItem.getId());
                    assertThat(label, taxData, not(nullValue()));
                    assertThat(label, taxData.getName(), equalTo(taxItem.getName()));
                    assertThat(label, taxData.getGenomes(), hasItem(genome.getId()));
                    // Now insure we're in the taxonomy tree.
                    if (! rank.contentEquals("species")) {
                        // Here we are a parent group, so we'll be in the taxonomy tree.
                        assertThat(taxTree.containsKey(taxItem.getId()), equalTo(true));
                    }
                    // Insure we're in the rank index.
                    assertThat(taxController.getRank(taxItem.getId()), equalTo(rank));
                    // Save the rank for later.
                    taxons.put(taxItem.getId(), rank);
                }
            }
        }
        // Now test the taxonomy tree. We sure that each parent contains all the genomes
        // in its children.
        for (var taxEntry : taxTree.entrySet()) {
            // Get the parent's genome set.
            int parent = taxEntry.getKey();
            String pLabel = "parent " + parent;
            String rank = taxons.get(parent);
            // Only proceed if we are NOT the virtual root parent.
            if (parent != 1) {
                assertThat(pLabel, rank, not(nullValue()));
                RankMap parentMap = rankMapMap.get(rank);
                Set<String> parentGenomes = parentMap.getTaxData(parent).getGenomes();
                // Check the children.
                for (int child : taxEntry.getValue()) {
                    String cLabel = pLabel + " child " + child;
                    String rank2 = taxons.get(child);
                    assertThat(cLabel, rank2, not(nullValue()));
                    RankMap childMap = rankMapMap.get(rank2);
                    Set<String> childGenomes = childMap.getTaxData(child).getGenomes();
                    for (String childGenome : childGenomes)
                        assertThat(cLabel, parentGenomes, hasItem(childGenome));
                }
            }
        }
        // Now test the mass genome-set retrieval.
        Set<Integer> taxSet = Set.of(481, 561, 724, 570);
        Map<Integer, Set<String>> genomeSetMap = taxController.getGenomeSets(taxSet);
        for (var taxEntry : genomeSetMap.entrySet()) {
            // Get the tax ID and the genome ID.
            int taxId = taxEntry.getKey();
            String label = "taxon " + taxId;
            Set<String> genomeSet = taxEntry.getValue();
            RankMap rankMap = rankMapMap.get(taxController.getRank(taxId));
            assertThat(label, rankMap, not(nullValue()));
            var taxData = rankMap.getTaxData(taxId);
            assertThat(label, taxData, not(nullValue()));
            assertThat(label, genomeSet, equalTo(taxData.getGenomes()));
        }
    }

}
