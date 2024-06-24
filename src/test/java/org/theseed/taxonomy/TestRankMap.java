/**
 *
 */
package org.theseed.taxonomy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.theseed.genome.TaxItem;

/**
 * @author Bruce Parrello
 *
 */
public class TestRankMap {

    @Test
    void testRankMapSaveAndLoad() throws IOException {
        RankMap genusMap = new RankMap();
        TaxItem t1 = new TaxItem(100, "tax100", "genus");
        TaxItem t2 = new TaxItem(200, "tax200", "genus");
        TaxItem t3 = new TaxItem(300, "tax300", "genus");
        TaxItem t4 = new TaxItem(400, "tax400", "genus");
        genusMap.add("100.1", t1);
        genusMap.add("100.2", t1);
        genusMap.add("100.3", t1);
        genusMap.add("200.1", t2);
        genusMap.add("200.2", t2);
        genusMap.add("300.1", t3);
        genusMap.add("400.1", t4);
        genusMap.add("400.2", t4);
        genusMap.add("400.3", t4);
        genusMap.add("400.4", t4);
        genusMap.add("400.5", t4);
        validateMap(genusMap);
        File saveFile = new File("data", "genusMap.ser");
        genusMap.save(saveFile);
        assertThat(saveFile.canRead(), equalTo(true));
        RankMap newGenusMap = new RankMap(saveFile);
        validateMap(newGenusMap);
    }

    /**
     * Insure that the specified rank map contains what we think it should.
     *
     * @param genusMap	rank map to validate
     */
    public static void validateMap(RankMap genusMap) {
        assertThat(genusMap.size(), equalTo(4));
        int[] taxes = genusMap.getTaxIds();
        assertThat(taxes.length, equalTo(4));
        assertThat(taxes[0], equalTo(100));
        assertThat(taxes[1], equalTo(200));
        assertThat(taxes[2], equalTo(300));
        assertThat(taxes[3], equalTo(400));
        RankMap.Taxon taxon = genusMap.getTaxData(100);
        assertThat(taxon.getId(), equalTo(100));
        assertThat(taxon.getName(), equalTo("tax100"));
        assertThat(taxon.getGenomes(), contains("100.1", "100.2", "100.3"));
        taxon = genusMap.getTaxData(200);
        assertThat(taxon.getId(), equalTo(200));
        assertThat(taxon.getName(), equalTo("tax200"));
        assertThat(taxon.getGenomes(), contains("200.1", "200.2"));
        taxon = genusMap.getTaxData(300);
        assertThat(taxon.getId(), equalTo(300));
        assertThat(taxon.getName(), equalTo("tax300"));
        assertThat(taxon.getGenomes(), contains("300.1"));
        taxon = genusMap.getTaxData(400);
        assertThat(taxon.getId(), equalTo(400));
        assertThat(taxon.getName(), equalTo("tax400"));
        assertThat(taxon.getGenomes(), contains("400.1", "400.2", "400.3", "400.4", "400.5"));
    }

}
