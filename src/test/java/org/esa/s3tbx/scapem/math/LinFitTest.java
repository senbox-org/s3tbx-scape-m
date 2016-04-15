package org.esa.s3tbx.scapem.math;

import Stats.LinFit;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for use of Powell algorithm
 *
 * @author olafd
 */
public class LinFitTest {

    @Test
    public void testLinfit() throws Exception {

        // This test follows LINFIT example at http://www.physics.nyu.edu/grierlab/idl_html_help/L33.html#wp53763 :
        // For Scape-M, we use the LinFit implementation from the Pamguard open source
        // software (www.pamguard.org)

        // Define two n-element vectors of paired data:
        double[] xVector = new double[]{-3.20, 4.49, -1.66, 0.64, -2.43, -0.89,
                -0.12, 1.41, 2.95, 2.18, 3.72, 5.26};
        double[] yVector = new double[]{-7.14, -1.30, -4.26, -1.90, -6.19, -3.98,
                -2.87, -1.66, -0.78, -2.61, 0.31, 1.74};

        // Define an n-element vector of Poisson measurement errors:
        double[] measErrors = new double[xVector.length];
        for (int i = 0; i < measErrors.length; i++) {
            measErrors[i] = Math.sqrt(Math.abs(yVector[i]));
        }

        //  Compute the model parameters, A and B, and print the result:
        final LinFit linFit = new LinFit(xVector, yVector, 12, measErrors);
        final double a = linFit.getA();
        final double b = linFit.getB();

        assertEquals(-3.16574, a, 1.E-5);
        assertEquals(0.829856, b, 1.E-5);
    }
}
