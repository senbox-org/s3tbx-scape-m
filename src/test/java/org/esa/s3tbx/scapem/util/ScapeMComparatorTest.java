package org.esa.s3tbx.scapem.util;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;


public class ScapeMComparatorTest {

    @Test
    public void testCellSampleComparator() throws Exception {

        CellSample[] cellSamples = new CellSample[9];

        cellSamples[0] = new CellSample(0, 0, 4.0);
        cellSamples[1] = new CellSample(0, 1, 3.1);
        cellSamples[2] = new CellSample(0, 2, 6.9);
        cellSamples[3] = new CellSample(1, 0, 5.5);
        cellSamples[4] = new CellSample(1, 1, 2.8);
        cellSamples[5] = new CellSample(1, 2, 0.9);
        cellSamples[6] = new CellSample(2, 0, 5.2);
        cellSamples[7] = new CellSample(2, 1, 4.4);
        cellSamples[8] = new CellSample(2, 2, 3.7);

        // ascending order:
        CellSampleComparator comparator = new CellSampleComparator();
        Arrays.sort(cellSamples, comparator);
        assertEquals(1, cellSamples[0].getCellXIndex());
        assertEquals(2, cellSamples[0].getCellYIndex());
        assertEquals(0.9, cellSamples[0].getValue());
        assertEquals(0, cellSamples[8].getCellXIndex());
        assertEquals(2, cellSamples[8].getCellYIndex());
        assertEquals(6.9, cellSamples[8].getValue());

        // descending order:
        comparator = new CellSampleComparator(true);
        Arrays.sort(cellSamples, comparator);
        assertEquals(1, cellSamples[8].getCellXIndex());
        assertEquals(2, cellSamples[8].getCellYIndex());
        assertEquals(0.9, cellSamples[8].getValue());
        assertEquals(0, cellSamples[0].getCellXIndex());
        assertEquals(2, cellSamples[0].getCellYIndex());
        assertEquals(6.9, cellSamples[0].getValue());

    }
}
