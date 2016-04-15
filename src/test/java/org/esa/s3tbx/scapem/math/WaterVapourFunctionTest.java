package org.esa.s3tbx.scapem.math;

import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.esa.s3tbx.scapem.operator.WaterVapourFunction;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for use of Water vapour function
 *
 * @author olafd
 */
public class WaterVapourFunctionTest {

    @Test
    public void testSolveWaterVapourFunction() throws Exception {

        // all numbers in this test taken from IDL test run, cellIndexX=1, cellIndexY=0
        WaterVapourFunction wvFunction = new WaterVapourFunction();
        wvFunction.setMerisRatio(0.64714217);
        wvFunction.setWvGr2(new double[]{0.301, 1.0, 1.5, 2.0, 2.7, 4.999});
        final double[][][] parAtmH = initParAtmH();
        wvFunction.setParAtmH(parAtmH);
        wvFunction.setReflPix(new double[]{0.358543, 0.365387});

        // Define the fractional tolerance:
        final double ftol = 1.0e-4;
        final int maxIter = 10000;

        final double wvLower = 0.302;
        final double wvUpper = 4.998;

        BrentSolver brentSolver = new BrentSolver(ftol);
        double result = brentSolver.solve(maxIter, wvFunction, wvLower, wvUpper);
        assertEquals(1.96476, result, ftol);
    }

    private double[][][] initParAtmH() {

        final double[][] lpwSp = new double[][]{
                {0.000347325, 0.000346807, 0.000346440, 0.000346088, 0.000343719, 0.000320908},
                {0.000304777, 0.000293110, 0.000288408, 0.000284697, 0.000279150, 0.000249003}
        };
        final double[][] etwSp = new double[][]{
                {0.0611599, 0.0608751, 0.0606844, 0.0605060, 0.0603419, 0.0604092},
                {0.0515248, 0.0439318, 0.0405391, 0.0378421, 0.0347929, 0.0286416}
        };
        final double[][] sabSp = new double[][]{
                {0.0400862, 0.0399702, 0.0398764, 0.0397741, 0.0395522, 0.0388895},
                {0.0355665, 0.0330387, 0.0316938, 0.0305411, 0.0291765, 0.0265667}
        };

        double[][][] parAtmH = new double[3][2][6];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 6; j++) {
                parAtmH[0][i][j] = lpwSp[i][j];
                parAtmH[1][i][j] = etwSp[i][j];
                parAtmH[2][i][j] = sabSp[i][j];
            }
        }

        return parAtmH;
    }

}
