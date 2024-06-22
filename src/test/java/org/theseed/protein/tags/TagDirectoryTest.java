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
import org.theseed.protein.tags.scanner.FeatureScanner;
import org.theseed.protein.tags.scanner.TestTagScanners;
import org.theseed.proteins.RoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TagDirectoryTest implements FeatureScanner.IParms {

    private static final File ROLE_FILE_NAME = new File("data", "roles.in.subsystems");
    protected static final String[] GENOME_SET_1 = new String[] { "487.3695", "487.3696", "487.3699", "487.3700", "487.3705"};
    protected static final String[] GENOME_SET_2 = new String[] { "727.1530", "727.1531", "727.1536", "727.1539", "727.1540"};

    @Test
    void tagDirStressTest() throws IOException, ParseFailureException {
        // Create the role map and role scanner.
        FeatureScanner roleScanner = FeatureScanner.Type.ROLE.create(this);
        RoleMap roleMap = RoleMap.load(ROLE_FILE_NAME);
        File tagDir = new File("data", "tagTest");
        // Insure the directory does not exist.
        if (tagDir.isDirectory())
            FileUtils.deleteDirectory(tagDir);
        // Create a new tag directory.
        TagDirectory tagController = new TagDirectory(tagDir);
        assertThat(tagController.size(), equalTo(0));
        assertThat(tagController.getGenome("511145.12"), empty());
        // Process some genomes.
        for (String genomeId : GENOME_SET_1) {
            // Load a genome and store it in the directory.
            Genome genome = this.loadGTO(genomeId);
            tagController.addGenome(genome, roleScanner);
            assertThat(genomeId, tagController.isInDirectory(genomeId), equalTo(true));
            // Read it back and verify the tags.
            Set<String> roleTags = tagController.getGenome(genomeId);
            TestTagScanners.verifyRoleTags(genome, roleTags, "tag controller 1", roleMap);
        }
        // Now reload the tag directory.
        tagController = new TagDirectory(tagDir);
        assertThat(tagController.size(), equalTo(GENOME_SET_1.length));
        for (String genomeId : GENOME_SET_1) {
            assertThat(genomeId, tagController.isInDirectory(genomeId), equalTo(true));
            Set<String> roleTags = tagController.getGenome(genomeId);
            Genome genome = this.loadGTO(genomeId);
            TestTagScanners.verifyRoleTags(genome, roleTags, "tag controller 2", roleMap);
        }
    }

    /**
     * This is a utility method that loads a genome from the test data directory.
     *
     * @param genomeId		ID of the genome to load
     *
     * @return the genome loaded
     *
     * @throws IOException
     */
    protected Genome loadGTO(String genomeId) throws IOException {
        File gFile = new File("data", genomeId + ".gto");
        Genome retVal = new Genome(gFile);
        return retVal;
    }

    @Override
    public File getRoleFileName() {
        return ROLE_FILE_NAME;
    }

}
