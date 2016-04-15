package org.esa.s3tbx.scapem.math;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Tests for use of Powell algorithm
 *
 * @author olafd
 */
public class PowellTest {

    @Test
    public void testSolveFunction_1() throws Exception {

        // This test follows Powell example at http://www.physics.nyu.edu/grierlab/idl_html_help/P24.html#wp51436 :

        // Define the fractional tolerance:
        final double ftol = 1.0e-4;

        // Define the starting point:
        double[] xVector = new double[]{0.5, -0.25d};

        // Define the starting directional vectors in column format:
        final double[][] xi = new double[][]{{1.0, 0.0}, {0.0, 1.0}};

        PowellTestFunction2D function1 = new PowellTestFunction2D();
        double fmin = Powell.fmin(xVector, xi, ftol, function1);

        assertNotNull(xVector);
        assertEquals(2, xVector.length);
        assertEquals(-0.31622777, xVector[0], 1.E-5);
        assertEquals(-0.63245552, xVector[1], 1.E-5);
        assertEquals(-0.95900918, fmin, 1.E-5);
    }

    @Test
    public void testSolveMinimToa() throws Exception {
        // todo
    }
}
