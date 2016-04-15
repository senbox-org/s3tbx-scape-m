package org.esa.s3tbx.scapem.math;

/**
 * Implements simple 2D test function:  f(x0, x1) = (x0 + 2.0*x1) * EXP(-x0*x0 -x1*x1)
 *
 * @author olafd
 */
public class PowellTestFunction2D implements MvFunction {

    @Override
    public double f(double[] x) {
        if (x.length != 2) {
            return -1;
        } else {
            return (x[0] + 2.0*x[1]) * Math.exp(-x[0]*x[0] -x[1]*x[1]);
        }
    }
}
