package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.s3tbx.scapem.math.MvFunction;

/**
 * Representation of TOA minimization function ('minim_TOA' from IDL breadboard)
 *
 * @author olafd
 */
public class ToaMinimization implements MvFunction {
    private double[] chiSquare;

    private double visLowerLim;
    private double[] visArrayLUT;
    private double[][] lpwArray;
    private double[][] etwArray;
    private double[][] sabArray;
    private double[][] refPixels;
    private double[] weight;
    private double visOld;
    private double[] rhoVeg;
    private double[] lpwInt;
    private double[] etwInt;
    private double[] sabInt;


    public ToaMinimization(double visLowerLim, double[] visArrayLUT,
                           double[][] lpwArray, double[][] etwArray, double[][] sabArray,
                           double visOld) {
        this.visLowerLim = visLowerLim;
        this.visArrayLUT = visArrayLUT;
        this.lpwArray = lpwArray;
        this.etwArray = etwArray;
        this.sabArray = sabArray;
        this.visOld = visOld;

        chiSquare = new double[ScapeMConstants.NUM_REF_PIXELS];
        lpwInt = new double[ScapeMConstants.L1_BAND_NUM];
        etwInt = new double[ScapeMConstants.L1_BAND_NUM];
        sabInt = new double[ScapeMConstants.L1_BAND_NUM];
    }

    @Override
    public double f(double[] x) {
        double[] surfRefl = new double[ScapeMConstants.L1_BAND_NUM];
        double[][] toa = new double[ScapeMConstants.L1_BAND_NUM][ScapeMConstants.NUM_REF_PIXELS];

        double vis = x[10];

        final double visUpperLim = visArrayLUT[visArrayLUT.length - 1];
        boolean xVectorInvalid = false;
        for (int i = 0; i < x.length; i++) {
            if (x[i] < 0.0) {
                xVectorInvalid = true;
                break;
            }
        }

        if (!xVectorInvalid && vis >= visLowerLim && vis < visUpperLim) {
            double toaMin = 0.0;
            if (vis != visOld) {
                int visInf = 0;
                for (int i =0; i < visArrayLUT.length; i++) {
                    if (vis >= visArrayLUT[i]) {
                        visInf = i;
                    }
                }

                final double delta = 1.0/(visArrayLUT[visInf + 1] - visArrayLUT[visInf]);

                for (int i = 0; i < ScapeMConstants.L1_BAND_NUM; i++) {
                    lpwInt[i] = ((((lpwArray[i][visInf + 1] - lpwArray[i][visInf]) * vis) +
                            (lpwArray[i][visInf] * visArrayLUT[visInf + 1])) -
                            (lpwArray[i][visInf + 1] * visArrayLUT[visInf])) * delta;

                    etwInt[i] = ((((etwArray[i][visInf + 1] - etwArray[i][visInf]) * vis) +
                            (etwArray[i][visInf] * visArrayLUT[visInf + 1])) -
                            (etwArray[i][visInf + 1] * visArrayLUT[visInf])) * delta;

                    sabInt[i] = ((((sabArray[i][visInf + 1] - sabArray[i][visInf]) * vis) +
                            (sabArray[i][visInf] * visArrayLUT[visInf + 1])) -
                            (sabArray[i][visInf + 1] * visArrayLUT[visInf])) * delta;
                }
            }
            for (int j = 0; j < ScapeMConstants.NUM_REF_PIXELS; j++) {
                chiSquare[j] = 0.0;
                for (int i = 0; i < ScapeMConstants.L1_BAND_NUM; i++) {
                    surfRefl[i] = x[2 * j] * rhoVeg[i] + x[2 * j + 1] * ScapeMConstants.RHO_SUE[i];
                    toa[i][j] = lpwInt[i] + surfRefl[i] * etwInt[i] / (Math.PI * (1.0 - sabInt[i] * surfRefl[i]));
                    chiSquare[j] += Math.pow(ScapeMConstants.WL_CENTER_INV[i] * (refPixels[i][j] - toa[i][j]), 2.0);
                }
                toaMin += weight[j] * chiSquare[j];
            }

            visOld = vis;
            StringBuffer buffer = new StringBuffer("toaMin, chisq = " + toaMin + "// ");
            for (int i = 0; i <chiSquare.length; i++) {
                buffer.append(chiSquare[i] + " ");
            }
//            System.out.println(buffer);
            return toaMin;

        } else {
            return 5.E+8; // todo: this is from breadboard, check more appropriate 'invalid' value
        }
    }

    public double[] getChiSquare() {
        return chiSquare;
    }

    public void setWeight(double[] weight) {
        this.weight = weight;
    }


    public void setRhoVeg(double[] rhoVeg) {
        this.rhoVeg = rhoVeg;
    }

    public void setRefPixels(double[][] refPixels) {
        this.refPixels = refPixels;
    }
}
