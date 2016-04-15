package org.esa.s3tbx.scapem.operator;

import org.esa.snap.core.util.math.LookupTable;

/**
 * SCAPE-M lookup table object.
 *
 * @author Tonio Fincke, Olaf Danne
 */
public class ScapeMLut {
    private LookupTable atmParamLut;

    private double hsfMin;
    private double hsfMax;
    private double visMin;
    private double visMax;
    private double cwvMin;
    private double cwvMax;

    private double[] visArrayLUT;
    private double[] hsfArrayLUT;
    private double[] cwvArrayLUT;

    public ScapeMLut(LookupTable atmParamLut) {
        this.atmParamLut = atmParamLut;
        setHsf();
        setVis();
        setCwv();
    }

    public LookupTable getAtmParamLut() {
        return atmParamLut;
    }

    public double getHsfMin() {
        return hsfMin;
    }

    public double getHsfMax() {
        return hsfMax;
    }

    public double getVisMin() {
        return visMin;
    }

    public double getVisMax() {
        return visMax;
    }

    public double getCwvMin() {
        return cwvMin;
    }

    public double getCwvMax() {
        return cwvMax;
    }

    public double[] getCwvArrayLUT() {
        return cwvArrayLUT;
    }

    public double[] getVisArrayLUT() {
        return visArrayLUT;
    }

    public double[] getHsfArrayLUT() {
        return hsfArrayLUT;
    }

    private void setHsf() {
        hsfArrayLUT = atmParamLut.getDimension(3).getSequence();
        hsfMin = hsfArrayLUT[0] + 0.001;
        hsfMax = hsfArrayLUT[hsfArrayLUT.length - 1] - 0.001;
    }

    private void setVis() {
        visArrayLUT = atmParamLut.getDimension(4).getSequence();
        visMin = visArrayLUT[0] + 0.001;
        visMax = visArrayLUT[visArrayLUT.length - 1] - 0.001;
    }

    private void setCwv() {
        cwvArrayLUT = atmParamLut.getDimension(5).getSequence();
        cwvMin = cwvArrayLUT[0] + 0.001;
        cwvMax = cwvArrayLUT[cwvArrayLUT.length - 1] - 0.001;
    }

}
