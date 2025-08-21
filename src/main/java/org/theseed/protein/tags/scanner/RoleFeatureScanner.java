/**
 *
 */
package org.theseed.protein.tags.scanner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(RoleFeatureScanner.class);
    /** map of role names to role IDs */
    private final RoleMap roleMap;

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
            retVal = switch (roles.size()) {
                case 0 -> EMPTY_TAG_SET;
                case 1 -> Set.of(roles.get(0).getId());
                default -> roles.stream().map(x -> x.getId()).collect(Collectors.toSet());
            };
        }
        return retVal;
    }

    @Override
    public String getTagName(String tag) {
        String retVal = this.roleMap.getName(tag);
        if (retVal == null)
            retVal = tag;
        return retVal;
    }

}
