package org.esa.s3tbx.scapem.math;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Implements simple test function:  f(x) = x - 1.0
 *
 * @author olafd
 */
public class DekkerBrentTestFunction1 implements UnivariateFunction {
    @Override
    public double value(double x) {
        return x - 1.0;
    }
}
