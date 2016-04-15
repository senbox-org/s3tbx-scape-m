package org.esa.s3tbx.scapem.util;

import java.util.Comparator;

/**
 * This comparator provides sorting of a CellSample value array, and also the indexes of the sorted array
 *
 * @author olafd
 */
public class CellSampleComparator implements Comparator<CellSample> {
    private boolean reverseOrder = false;

    public CellSampleComparator() {
    }

    public CellSampleComparator(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    @Override
    public int compare(CellSample o1, CellSample o2) {
        if (reverseOrder) {
            return o2.getValue().compareTo(o1.getValue());
        } else {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
}

