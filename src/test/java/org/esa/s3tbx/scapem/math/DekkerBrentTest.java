package org.esa.s3tbx.scapem.math;

import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for use of Dekker-Brent Solver (taken from apache-commons, just to make sure...)
 *
 * @author olafd
 */
public class DekkerBrentTest {

    @Test
    public void testDekkerBrentFunction() throws Exception {
        // Define the fractional tolerance:
        final double ftol = 1.0e-4;
        final int maxIter = 10000;

        // f(x) = x - 1
        DekkerBrentTestFunction1 testFunction = new DekkerBrentTestFunction1();

        double a = 0.5;
        double b = 2.0;

        BrentSolver brentSolver = new BrentSolver(ftol);
        double result = brentSolver.solve(maxIter, testFunction, a, b);
        assertEquals(1.0, result, 1.E-4);

        // f(x) = cos(x)
        DekkerBrentTestFunction2 testFunction2 = new DekkerBrentTestFunction2();

        a = 1.0;
        b = 2.0;

        result = brentSolver.solve(maxIter, testFunction2, a, b);
        assertEquals(Math.PI/2.0, result, 1.E-4);
    }

}
