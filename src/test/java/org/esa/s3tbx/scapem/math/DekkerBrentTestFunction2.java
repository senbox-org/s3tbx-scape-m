package org.esa.s3tbx.scapem.math;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Implements simple test function:  f(x) = cos(x)
 *
 * @author olafd
 */
public class DekkerBrentTestFunction2 implements UnivariateFunction {
    @Override
    public double value(double x) {
        return Math.cos(x);
    }
}
