/**
 *
 */
package org.theseed.protein.tags;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestTagCounts {

    @Test
    void testCounter() {
        Count counter = new Count();
        assertThat(counter.getValue(), equalTo(0));
        counter.add(3);
        assertThat(counter.getValue(), equalTo(3));
        counter.add(1);
        assertThat(counter.getValue(), equalTo(4));
        Count counter2 = new Count(2);
        assertThat(counter2.getValue(), equalTo(2));
        Count counter3 = new Count(-1);
        List<Count> countList = new ArrayList<Count>();
        countList.add(counter3);
        countList.add(counter2);
        for (int i = 1; i < 100; i += 5) {
            Count newCount = new Count(i);
            countList.add(newCount);
        }
        countList.add(counter);
        // Test the count sort.
        Collections.sort(countList);
        for (int i = 1; i < countList.size(); i++)
            assertThat(String.valueOf(i), countList.get(i-1).getValue(), lessThanOrEqualTo(countList.get(i).getValue()));
        counter.clear();
        assertThat(counter.getValue(), equalTo(0));
        counter2.add(counter3);
        assertThat(counter2.getValue(), equalTo(1));
    }

    @Test
    void testTagMap() throws IOException {
        TagCounts tagMap = new TagCounts();
        tagMap.count("A");
        assertThat(tagMap.getCount("A"), equalTo(1));
        tagMap.count("B", 2);
        assertThat(tagMap.getCount("B"), equalTo(2));
        assertThat(tagMap.getCount("A"), equalTo(1));
        tagMap.count("B");
        assertThat(tagMap.getCount("B"), equalTo(3));
        assertThat(tagMap.getCount("A"), equalTo(1));
        assertThat(tagMap.getCount("C"), equalTo(0));
        tagMap.count("D", 6);
        tagMap.count("E", 4);
        tagMap.count("F", 10);
        var sortedCounts = tagMap.getSortedCounts();
        assertThat(sortedCounts.get(0).getKey(), equalTo("F"));
        assertThat(sortedCounts.get(0).getValue().getValue(), equalTo(10));
        assertThat(sortedCounts.get(1).getKey(), equalTo("D"));
        assertThat(sortedCounts.get(1).getValue().getValue(), equalTo(6));
        assertThat(sortedCounts.get(2).getKey(), equalTo("E"));
        assertThat(sortedCounts.get(2).getValue().getValue(), equalTo(4));
        assertThat(sortedCounts.get(3).getKey(), equalTo("B"));
        assertThat(sortedCounts.get(3).getValue().getValue(), equalTo(3));
        assertThat(sortedCounts.get(4).getKey(), equalTo("A"));
        assertThat(sortedCounts.get(4).getValue().getValue(), equalTo(1));
        assertThat(tagMap.size(), equalTo(5));
        File saveFile = new File("data", "tagCounts.ser");
        tagMap.save(saveFile);
        tagMap.clear();
        assertThat(tagMap.size(), equalTo(0));
        assertThat(tagMap.getCount("F"), equalTo(0));
        tagMap = new TagCounts(saveFile);
        assertThat(tagMap.size(), equalTo(5));
        assertThat(tagMap.getCount("F"), equalTo(10));
        assertThat(tagMap.getCount("D"), equalTo(6));
        assertThat(tagMap.getCount("E"), equalTo(4));
        assertThat(tagMap.getCount("B"), equalTo(3));
        assertThat(tagMap.getCount("A"), equalTo(1));
        assertThat(tagMap.getCount("C"), equalTo(0));
    }

}
