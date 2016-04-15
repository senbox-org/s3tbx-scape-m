package org.esa.s3tbx.scapem.math;

/**
 * Interface providing a uni-variate function.
 *
 * @author Andreas Heckel (Swansea University), Olaf Danne
 */
public interface Function {

    /**
     *  Uni-variate function definition
     *
     * @param  x  - input value
     * @return f -  return value
     */
    double f(double x);
}
