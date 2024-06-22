/**
 *
 */
package org.theseed.protein.tags.scanner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

/**
 * This method uses as tags the role IDs for well-annotated roles in a feature.  The role IDs (and the set of
 * well-annotated roles) is given by a role file (generally called "roles.in.subsystems".  Only protein features
 * are scanned.
 *
 * @author Bruce Parrello
 *
 */
public class RoleFeatureScanner extends FeatureScanner {

    // FIELDS
    /** map of role names to role IDs */
    private RoleMap roleMap;

    /**
     * @param processor
     */
    public RoleFeatureScanner(IParms processor) throws IOException, ParseFailureException {
        super(processor);
        File roleFileName = processor.getRoleFileName();
        log.info("Loading role definitions from {}.", roleFileName);
        this.roleMap = RoleMap.load(roleFileName);
        log.info("{} role definitions found in {}.", this.roleMap.size(), roleFileName);
    }

    @Override
    protected Set<String> getFeatureTags(Feature feat) {
        // Only proceed for proteins.
        Set<String> retVal;
        if (! feat.getType().equals("CDS")) {
            retVal = EMPTY_TAG_SET;
        } else {
            List<Role> roles = feat.getUsefulRoles(this.roleMap);
            // Generally, there is only zero or one, so we optimize those cases.
            switch (roles.size()) {
            case 0:
                retVal = EMPTY_TAG_SET;
                break;
            case 1:
                retVal = Set.of(roles.get(0).getId());
                break;
            default :
                retVal = roles.stream().map(x -> x.getId()).collect(Collectors.toSet());
            }
        }
        return retVal;
    }

}
