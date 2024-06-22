/**
 *
 */
package org.theseed.protein.tags;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.theseed.io.TabbedLineReader;

/**
 * This object counts tags for the genome comparison.  It is lighter weight than CountMap or WeightMap because we expect a large number
 * of tags to count.
 *
 * @author Bruce Parrello
 *
 */
public class TagCounts {

    // FIELDS
    /** map of tags to counts */
    private Map<String, Count> countMap;

    /**
     * This is a comparator that allows sorting the map entries from highest to lowest.
     */
    public static class Sorter implements Comparator<Map.Entry<String, Count>> {

        @Override
        public int compare(Entry<String, Count> o1, Entry<String, Count> o2) {
            int retVal = o2.getValue().compareTo(o1.getValue());
            if (retVal == 0)
                retVal = o1.getKey().compareTo(o2.getKey());
            return retVal;
        }

    }

    /**
     * Construct a blank, empty tag count map.
     */
    public TagCounts() {
        this.countMap = new HashMap<String, Count>();
    }

    /**
     * Construct a tag count map with a specified hash table capacity.
     *
     * @param capacity		initial hash table size
     */
    public TagCounts(int capacity) {
        this.countMap = new HashMap<String, Count>(capacity);
    }

    /**
     * Construct a tag count map from a save file.
     *
     * @param fileName		name of file containing the saved tag count map
     *
     * @throws IOException
     */
    public TagCounts(File saveFile) throws IOException {
        try (TabbedLineReader inStream = new TabbedLineReader(saveFile)) {
            var iter = inStream.iterator();
            if (! iter.hasNext()) {
                // Here we have an empty map.
                this.countMap = new HashMap<String, Count>();
            } else {
                // Get the first line and use it to estimate the hash size.
                var line = iter.next();
                String key = line.get(0);
                int count = line.getInt(1);
                int estimate = (int) (saveFile.length() / (key.length() + 5)) / 3 * 4 + 1;
                this.countMap = new HashMap<String, Count>(estimate);
                setCount(key, count);
                // Now read the rest of the lines.
                while (iter.hasNext()) {
                    line = iter.next();
                    this.setCount(line.get(0), line.getInt(1));
                }
            }
        }
    }

    /**
     * Write the tag counts to a save file.
     *
     * @param saveFile		name of the file to save this tag count map
     *
     * @throws IOException
     */
    public void save(File saveFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(saveFile)) {
            writer.println("tag\tcount");
            for (var countEntry : this.countMap.entrySet()) {
                String tag = countEntry.getKey();
                int count = countEntry.getValue().getValue();
                writer.println(tag + "\t" + count);
            }
        }
    }

    /**
     * Store a specified count value for a key.
     *
     * @param key		tag to count
     * @param count		count value to associate with it
     */
    protected void setCount(String key, int count) {
        this.countMap.put(key, new Count(count));
    }

    /**
     * @return the count value for a tag, or 0 if the tag does not exist.
     *
     * @param key	desired tag string
     */
    public int getCount(String key) {
        Count counter = this.countMap.get(key);
        int retVal;
        if (counter == null)
            retVal = 0;
        else
            retVal = counter.getValue();
        return retVal;
    }

    /**
     * Increment a count and return the value.
     *
     * @param key	desired tag string
     */
    public int count(String key) {
        return this.count(key, 1);
    }

    /**
     * Increment a count by a specified amount and return the value.
     *
     * @param key	desired tag string
     * @param incr	value by which to increment
     */
    public int count(String key, int incr) {
        Count counter = this.countMap.computeIfAbsent(key, x -> new Count());
        counter.add(incr);
        return counter.getValue();
    }

    /**
     * Erase all the counts in this map.
     */
    public void clear() {
        this.countMap.clear();
    }

    /**
     * @return the number of counts in this map
     */
    public int size() {
        return this.countMap.size();
    }

    /**
     * @return an unordered collection of all the counts
     */
    public Collection<Map.Entry<String, Count>> getAllCounts() {
        return this.countMap.entrySet();
    }

    /**
     * @return a list of all the counts, from highest to lowest
     */
    public List<Map.Entry<String, Count>> getSortedCounts() {
        List<Map.Entry<String, Count>> retVal = new ArrayList<Map.Entry<String, Count>>(this.getAllCounts());
        Collections.sort(retVal, new Sorter());
        return retVal;
    }

}
