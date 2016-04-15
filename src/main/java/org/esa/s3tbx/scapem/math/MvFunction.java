package org.esa.s3tbx.scapem.math;

/**
 * Interface providing a multi-variate function.
 *
 * @author Andreas Heckel (Swansea University), Olaf Danne
 */
public interface MvFunction {

    /**
     *  Multi-variate function definition
     *
     * @param  x - point at which function should be calculated
     * @return f - value of the function at x
     */
    double f(double[] x);

}