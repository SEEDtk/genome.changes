/**
 *
 */
package org.theseed.protein.tags.scanner;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Feature;

/**
 * This scanner returns the global protein family of a feature.  It only scans protein features.
 *
 * @author Bruce Parrello
 *
 */
public class PattyFamFeatureScanner extends FeatureScanner {

    public PattyFamFeatureScanner(IParms processor) {
        super(processor);
    }

    @Override
    protected Set<String> getFeatureTags(Feature feat) {
        Set<String> retVal;
        if (! feat.getType().equals("CDS"))
            retVal = EMPTY_TAG_SET;
        else {
            String pgfam = feat.getPgfam();
            if (StringUtils.isBlank(pgfam))
                retVal = EMPTY_TAG_SET;
            else
                retVal = Set.of(pgfam);
        }
        return retVal;
    }

    @Override
    public String getTagName(String tag) {
        // A pattyfam's name is the tag itself.
        return tag;
    }

}
