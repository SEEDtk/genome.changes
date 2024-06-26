/**
 *
 */
package org.theseed.protein.tags;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.io.SetFile;
import org.theseed.protein.tags.scanner.FeatureScanner;

/**
 * This object manages a directory of tag sets, one per genome.  A subset of the directory can then be loaded into
 * a count map and used for tag comparison to another subset.  This avoids having to re-scan the genomes for each
 * comparison.
 *
 * @author Bruce Parrello
 *
 */
public class TagDirectory {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TagDirectory.class);
    /** directory name */
    private File dirName;
    /** map of genome IDs to file names */
    private Map<String, File> fileMap;
    /** tag count file suffix */
    private static final String TAG_FILE_SUFFIX = ".tags";
    /** tag file name match pattern */
    private static final Pattern TAG_FILE_PATTERN = Pattern.compile("\\d+\\.\\d+\\" + TAG_FILE_SUFFIX);
    /** empty tag set */
    private static final Set<String> EMPTY_TAG_SET = Collections.emptySet();
    /** tag file name filter */
    private static FileFilter TAG_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            // Only pass on a readable file with the proper suffix following a genome ID.
            boolean retVal = pathname.canRead();
            if (retVal) {
                String basename = pathname.getName();
                retVal = (TAG_FILE_PATTERN.matcher(basename).matches());
            }
            return retVal;
        }
    };

    /**
     * Load an existing tag directory.
     *
     * @param tagDir	tag directory to load
     *
     * @throws IOException
     */
    public TagDirectory(File tagDir) throws IOException {
        this.dirName = tagDir;
        if (! tagDir.isDirectory()) {
            log.info("Creating directory {} for tag sets.", tagDir);
            FileUtils.forceMkdir(tagDir);
            this.fileMap = new HashMap<String, File>();
        } else {
            File[] tagFiles = tagDir.listFiles(TAG_FILE_FILTER);
            this.fileMap = new HashMap<String, File>((tagFiles.length + 2) / 3 * 4 + 1);
            for (File tagFile : tagFiles) {
                String genomeId = StringUtils.substringBeforeLast(tagFile.getName(), TAG_FILE_SUFFIX);
                this.fileMap.put(genomeId, tagFile);
            }
            log.info("{} tag files found in {}.", this.fileMap.size(), tagDir);
        }
    }

    /**
     * @return the tag counts for a set of genomes
     *
     * @param genomeSet		set of IDs for the genomes to count
     *
     * @throws IOException
     */
    public TagCounts getTagCounts(Set<String> genomeSet) throws IOException {
        TagCounts retVal = new TagCounts();
        for (String genomeId : genomeSet) {
            Set<String> tags = this.getGenome(genomeId);
            retVal.count(tags);
        }
        return retVal;
    }

    /**
     * Convert a genome ID to a file name.
     *
     * @param genomeId		ID of the genome of interest
     *
     * @return the name of the file that would contain that genome's tags
     */
    protected File getGenomeFile(String genomeId) {
        return new File(this.dirName, genomeId + TAG_FILE_SUFFIX);
    }

    /**
     * Add a genome to the tag directory.
     *
     * @param genome	genome to add
     * @param scanner	feature scanner to use
     *
     * @throws IOException
     */
    public void addGenome(Genome genome, FeatureScanner scanner) throws IOException {
        // Compute the tag set for this genome.
        Set<String> tags = scanner.getTags(genome);
        // Compute the associated file.
        String genomeId = genome.getId();
        File genomeFile = this.getGenomeFile(genomeId);
        // Write the set to the file.
        SetFile.save(genomeFile, tags);
        // Update the map.
        this.fileMap.put(genomeId, genomeFile);
    }

    /**
     * Fetch a genome's tag set from the tag directory.
     *
     * @param genomeId	ID of the target genome
     *
     * @return the set of tags for that genome
     *
     * @throws IOException
     */
    public Set<String> getGenome(String genomeId) throws IOException {
        Set<String> retVal;
        File genomeFile = this.fileMap.get(genomeId);
        if (genomeFile == null)
            retVal = EMPTY_TAG_SET;
        else
            retVal = SetFile.load(genomeFile);
        return retVal;
    }

    /**
     * @return TRUE if the specified genome has a tag set in this directory
     *
     * @param genomeId	ID of the target genome
     */
    public boolean isInDirectory(String genomeId) {
        return this.fileMap.containsKey(genomeId);
    }

    /**
     * @return the number of genomes in this tag directory
     */
    public int size() {
        return this.fileMap.size();
    }

}
