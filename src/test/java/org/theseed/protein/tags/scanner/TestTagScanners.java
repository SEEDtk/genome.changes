/**
 *
 */
package org.theseed.protein.tags.scanner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TestTagScanners implements FeatureScanner.IParms {

    @Test
    void testRoleScanner() throws IOException, ParseFailureException {
        FeatureScanner scanner = FeatureScanner.Type.ROLE.create(this);
        Genome genome = new Genome(new File("data", "511145.12.gto"));
        Set<String> roleTags = scanner.getTags(genome);
        assertThat(roleTags.size(), equalTo(2659));
        // We need to verify that every useful role in the genome is in the tag set.
        RoleMap roleMap = RoleMap.load(this.getRoleFileName());
        // Verify that every role is in the set.
        for (Feature feat : genome.getPegs()) {
            List<Role> roles = feat.getUsefulRoles(roleMap);
            String fid = feat.getId();
            for (Role role : roles)
                assertThat(fid, role.getId(),in(roleTags));
        }
    }

    @Test
    void testPattyFamScanner() throws IOException, ParseFailureException {
        FeatureScanner scanner = FeatureScanner.Type.PGFAM.create(this);
        Genome genome = new Genome(new File("data", "511145.12.gto"));
        Set<String> famTags = scanner.getTags(genome);
        assertThat(famTags.size(), equalTo(4267));
        // We need to verify that every PGFAM in the genome is in the tag set.
        for (Feature feat : genome.getPegs()) {
            String fid = feat.getId();
            String pgFam = feat.getPgfam();
            if (pgFam != null && ! pgFam.isEmpty())
                assertThat(fid, pgFam, in(famTags));
        }
    }

    @Override
    public File getRoleFileName() {
        return new File("data", "roles.in.subsystems");
    }

}
