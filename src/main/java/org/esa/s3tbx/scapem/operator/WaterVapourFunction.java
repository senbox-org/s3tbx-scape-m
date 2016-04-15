package org.esa.s3tbx.scapem.operator;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Implements water vapour retrieval function ('chisq_merisWV' in IDL breadboard)
 *
 * @author olafd
 */
public class WaterVapourFunction implements UnivariateFunction {

    int wvInf;
    double[] reflPix;        // [2]
    double[] wvGr2;          // [6]
    double[][][] parAtmH;    // [3][2][6]
    double merisRatio;
    private double wvP;

    @Override
    public double value(double wv) {

        double chiSqrResult = 0.0;

        wvInf = -1;
        for (int i = 0; i < wvGr2.length; i++) {
            if (wv > wvGr2[i]) {
                wvInf = i;
            }
        }

        if (wvInf >= 0) {
            wvP = (wv - wvGr2[wvInf]) / (wvGr2[wvInf + 1] - wvGr2[wvInf]);
            double[][] parAtmInit = new double[parAtmH.length][parAtmH[0].length];
            for (int i = 0; i < parAtmH.length; i++) {
                for (int j = 0; j < parAtmH[0].length; j++) {
                    parAtmInit[i][j] = parAtmH[i][j][wvInf] +
                            wvP * (parAtmH[i][j][wvInf + 1] - parAtmH[i][j][wvInf]);
                }
            }
            double[] lToa0 = new double[2];
            for (int i = 0; i < lToa0.length; i++) {
                lToa0[i] = parAtmInit[0][i] +
                        reflPix[i] * parAtmInit[1][i] / (Math.PI * (1.0 - reflPix[i] * parAtmInit[2][i]));
            }
            chiSqrResult = merisRatio - lToa0[1] / lToa0[0];
        }

        return chiSqrResult;
    }

    public int getWvInf() {
        return wvInf;
    }

    public double getWvP() {
        return wvP;
    }

    public void setReflPix(double[] reflPix) {
        this.reflPix = reflPix;
    }

    public void setWvGr2(double[] wvGr2) {
        this.wvGr2 = wvGr2;
    }

    public void setParAtmH(double[][][] parAtmH) {
        this.parAtmH = parAtmH;
    }

    public void setMerisRatio(double merisRatio) {
        this.merisRatio = merisRatio;
    }
}
