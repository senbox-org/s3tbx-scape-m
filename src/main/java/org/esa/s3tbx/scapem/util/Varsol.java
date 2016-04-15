package org.esa.s3tbx.scapem.util;


import org.esa.snap.core.util.math.MathUtils;

/**
 * Provides the variability of the solar constant during the year.
 *
 * @author olafd
 */
public class Varsol {

    /**
     * Calculation of the variability of the solar constant during the year.
     * (Java implementation of 6S Fortran routine 'varsol').
     *
     * @param doy - day of year
     * @return dSol - multiplicative factor to apply to the mean value of the solar constant
     */
    public static double getVarSol(int doy) {
        double om = 0.9856 * (doy - 4) * MathUtils.DTOR;
        return Math.sqrt(Math.pow(1. - 0.01673 * Math.cos(om), 2.0));
    }

}
