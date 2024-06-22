/**
 *
 */
package org.theseed.protein.tags.scanner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * This is the base class for feature scanners.  A feature scanner finds all the tags for a genome's features
 * and returns them as a set.
 *
 * @author Bruce Parrello
 *
 */
public abstract class FeatureScanner {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(FeatureScanner.class);
    /** empty set for subclasses to return */
    protected static final Set<String> EMPTY_TAG_SET = Collections.emptySet();

    /**
     * This interface specifies the information that a controlling command processor must
     * be able to provide for feature scanners.
     */
    public interface IParms {

        /**
         * @return the name of the role definition file
         */
        public File getRoleFileName();

    }

    /**
     * This enum specifies the different types of feature scanners.
     */
    public static enum Type {
        /** protein roles */
        ROLE {
            @Override
            public FeatureScanner create(IParms processor) throws IOException, ParseFailureException {
                return new RoleFeatureScanner(processor);
            }
        },
        /** global pattyfam */
        PGFAM {
            @Override
            public FeatureScanner create(IParms processor) {
                return new PattyFamFeatureScanner(processor);
            }
        };

        /**
         * @return a feature scanner of this type
         *
         * @param processor		controlling command processor
         *
         * @throws ParseFailureException
         * @throws IOException
         */
        public abstract FeatureScanner create(IParms processor) throws IOException, ParseFailureException;

    }

    /**
     * Construct a feature scanner.
     *
     * @param processor		controlling command processor
     */
    public FeatureScanner(IParms processor) {
        // This constructor does nothing and only exists to force a uniform constructor signature.
    }

    /**
     * This is the main working method for this object.  Given a genome, it returns the
     * set of tags.
     *
     * @param genome	genome to scan
     *
     * @return a set of the tags found
     */
    public final Set<String> getTags(Genome genome) {
        Set<String> retVal = new HashSet<String>();
        for (Feature feat : genome.getFeatures()) {
            Set<String> tags = this.getFeatureTags(feat);
            retVal.addAll(tags);
        }
        return retVal;
    }

    /**
     * @return the set of tags for the current feature
     *
     * @param feat		feature to scan
     */
    protected abstract Set<String> getFeatureTags(Feature feat);

}
