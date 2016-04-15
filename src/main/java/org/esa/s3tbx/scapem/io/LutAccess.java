package org.esa.s3tbx.scapem.io;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.util.math.FracIndex;
import org.esa.snap.core.util.math.LookupTable;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Access to LookUpTables
 *
 * @author Tonio Fincke, Olaf Danne
 */
public class LutAccess {
    private static final String atmParamLutPath = "SCAPEM_LUT_MERIS";    // currently we have only this one

    /**
     * reads an Atmospheric parameters LUT (IDL breadboard procedure 'read_lut')
     * * This LUT is equivalent to the original IDL LUT:
     * for i = 0, nm_avs - 1 do begin
     * for j = 0, nm_asl - 1 do $
     * for k = 0, nm_azm - 1 do $
     * for ii = 0, nm_hsf - 1 do $
     * for jj = 0, nm_vis - 1 do $
     * for kk = 0, nm_wv - 1 do $
     * readu, 1, aux
     * for ind = 0, nm_par - 1 do lut[ind, i, j, k, ii, jj, kk, *] = aux[*, ind]
     * A LUT value can be accessed with
     * lut.getValue(new double[]{vzaValue, szaValue, raaValue, hsfValue, visValue, cwvValue, wvlValue, parameterValue});
     *
     * @return LookupTable
     * @throws java.io.IOException when failing to real LUT data
     */
    public static LookupTable getAtmParmsLookupTable() throws IOException {
        ImageInputStream iis = LutAccess.getAtmParamLutData();
        try {
            // read LUT dimensions and values
            float[] vza = LutAccess.readDimension(iis);
            int nVza = vza.length;
            float[] sza = LutAccess.readDimension(iis);
            int nSza = sza.length;
            float[] raa = LutAccess.readDimension(iis);
            int nRaa = raa.length;
            float[] hsf = LutAccess.readDimension(iis);
            int nHsf = hsf.length;
            float[] vis = LutAccess.readDimension(iis);
            int nVis = vis.length;
            float[] cwv = LutAccess.readDimension(iis);
            int nCwv = cwv.length;

            float[] parameters = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
            int nParameters = parameters.length;
            float[] wvl = ScapeMConstants.MERIS_WAVELENGTHS;
            int nWvl = wvl.length;

            float[] lut = new float[nParameters * nVza * nSza * nRaa * nHsf * nVis * nCwv * nWvl];

            for (int iVza = 0; iVza < nVza; iVza++) {
                for (int iSza = 0; iSza < nSza; iSza++) {
                    for (int iRaa = 0; iRaa < nRaa; iRaa++) {
                        for (int iHsf = 0; iHsf < nHsf; iHsf++) {
                            for (int iVis = 0; iVis < nVis; iVis++) {
                                for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                                    for (int iParams = 0; iParams < nParameters; iParams++) {
                                        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                                            int i = iWvl + nWvl * (iParams + nParameters * (iCwv + nCwv * (iVis + nVis * (iHsf + nHsf * (iRaa + nRaa * (iSza + nSza * iVza))))));
                                            lut[i] = iis.readFloat();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return new LookupTable(lut, vza, sza, raa, hsf, vis, cwv, parameters, wvl);
        } finally {
            iis.close();
        }
    }

    /**
     * 6-D linear interpolation: returns 15x7 array ('f_ind' in IDL code)
     * as a function of [vza, sza, phi, hsf, aot] from the interpolation of the Atmospheric parameters LUT
     */
    public static double[][] interpolAtmParamLut(LookupTable atmParamLut,
                                                 double vza,
                                                 double sza,
                                                 double raa,
                                                 double hsf,
                                                 double vis,
                                                 double cwv) {
        final float[] wvl = ScapeMConstants.MERIS_WAVELENGTHS;
        final double[] params = atmParamLut.getDimension(6).getSequence();
        double[][] result = new double[wvl.length][7];

        int lutDimensionCount = atmParamLut.getDimensionCount();
        FracIndex[] fracIndices = FracIndex.createArray(lutDimensionCount);
        double[] v = new double[1 << lutDimensionCount];

        LookupTable.computeFracIndex(atmParamLut.getDimension(0), vza, fracIndices[0]);
        LookupTable.computeFracIndex(atmParamLut.getDimension(1), sza, fracIndices[1]);
        LookupTable.computeFracIndex(atmParamLut.getDimension(2), raa, fracIndices[2]);
        LookupTable.computeFracIndex(atmParamLut.getDimension(3), hsf, fracIndices[3]);
        LookupTable.computeFracIndex(atmParamLut.getDimension(4), vis, fracIndices[4]);
        LookupTable.computeFracIndex(atmParamLut.getDimension(5), cwv, fracIndices[5]);

        for (int i = 0; i < result.length; i++) {
            int index = 0;
            LookupTable.computeFracIndex(atmParamLut.getDimension(7), wvl[i], fracIndices[7]);
            for (double param : params) {
                LookupTable.computeFracIndex(atmParamLut.getDimension(6), param, fracIndices[6]);
                result[i][index++] = atmParamLut.getValue(fracIndices, v);
            }
        }
        return result;
    }

    public static ImageInputStream getAtmParamLutData() {
        return openStream(atmParamLutPath);
    }

    public static float[] readDimension(ImageInputStream iis) throws IOException {
        return readDimension(iis, iis.readInt());
    }

    public static float[] readDimension(ImageInputStream iis, int len) throws IOException {
        float[] dim = new float[len];
        iis.readFully(dim, 0, len);
        return dim;
    }


    private static ImageInputStream openStream(String path) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(openResource(path));
        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(bufferedInputStream);
        imageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return imageInputStream;
    }

    private static InputStream openResource(String path) {
        InputStream inputStream = LutAccess.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find resource: " + path);
        }
        return inputStream;
    }

}
