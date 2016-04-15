package org.esa.s3tbx.scapem.util;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ScapeMUtilsTest {

    double[] x;

    @Before
    public void setUp() throws Exception {
        x = new double[]{65., 63., 67., 64., 68., 62., 70., 66., 68., 67., 69., 71., 66., 65., 70.};
    }


    @Test
    public void testGetMeanDouble1D() {
        double mean = ScapeMUtils.getMeanDouble1D(x);
        assertEquals(66.7333, mean, 1.E-4);
    }

    @Test
    public void testGetMinimumDouble1D() {
       double min = ScapeMUtils.getMinimumDouble1D(x);
       assertEquals(62.0, min, 1.E-4);
    }

    @Test
    public void testGetMinimumIndexDouble1D() {
        int minIndex = ScapeMUtils.getMinimumIndexDouble1D(x);
        assertEquals(5, minIndex);
    }

    @Test
    public void testGetStdevDouble1D() {
        double stdev = ScapeMUtils.getStdevDouble1D(x);
        assertEquals(2.65832, stdev, 1.E-4);
    }

}
