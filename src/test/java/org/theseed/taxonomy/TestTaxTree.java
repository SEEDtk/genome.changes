/**
 *
 */
package org.theseed.taxonomy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestTaxTree {

    @Test
    void testTaxTree() throws IOException {
        // The test tree is
        //  r1    r3   r5     r6
        //	99 --> 2 --> 4, 5, 6
        //		   3 --> 7 --> 8, 9
        //		 	    10 --> 11 --> 12
        File saveFile = new File("data", "taxTree.ser");
        if (saveFile.canRead())
            FileUtils.forceDelete(saveFile);
        TaxTree testTree = new TaxTree(saveFile);
        assertThat(testTree.isEmpty(), equalTo(true));
        // This is a level-skip that tests the merging.
        testTree.addLink(9, 99, 1);
        // Now the normal tree.  These have been shuffled randomly.
        testTree.addLink(7, 3, 3);
        testTree.addLink(4, 2, 3);
        testTree.addLink(11, 10, 5);
        testTree.addLink(6, 2, 3);
        testTree.addLink(2, 99, 1);
        testTree.addLink(9, 7, 5);
        testTree.addLink(10, 3, 3);
        testTree.addLink(3, 99, 1);
        testTree.addLink(12, 11, 6);
        testTree.addLink(6, 2, 3);
        testTree.addLink(5, 2, 3);
        testTree.addLink(8, 7, 5);
        // Validate the tree.
        this.validateTree(testTree);
        // Test save and load.
        testTree.save();
        assertThat(saveFile.canRead(), equalTo(true));
        TaxTree testTree2 = new TaxTree(saveFile);
        validateTree(testTree2);
    }

    /**
     * Validate the structure of the tree.
     *
     * @param testTree		tree to validate
     */
    private void validateTree(TaxTree testTree) {
        var actualTree = testTree.getTree();
        Set<Integer> children = actualTree.get(TaxTree.ROOT_GROUP);
        assertThat(children, containsInAnyOrder(99));
        children = actualTree.get(99);
        assertThat(children, containsInAnyOrder(2, 3));
        children = actualTree.get(2);
        assertThat(children, containsInAnyOrder(4, 5, 6));
        children = actualTree.get(3);
        assertThat(children, containsInAnyOrder(7, 10));
        children = actualTree.get(7);
        assertThat(children, containsInAnyOrder(8, 9));
        children = actualTree.get(10);
        assertThat(children, containsInAnyOrder(11));
        children = actualTree.get(11);
        assertThat(children, containsInAnyOrder(12));
    }

}
