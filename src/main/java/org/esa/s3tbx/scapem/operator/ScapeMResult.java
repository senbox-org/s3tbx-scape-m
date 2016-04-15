package org.esa.s3tbx.scapem.operator;

/**
 * Container holding a water vapour result from Brent computation in AC part.
 *
 * @author Tonio Fincke, Olaf Danne
 */
public class ScapeMResult {
    double[][] wv;
    private double[][][] refl;

    /**
     * ScapeMResult constructor, setting up the result arrays
     *
     * @param numBands - number of bands
     * @param width - cell width
     * @param height - cell height
     */
    public ScapeMResult(int numBands, int width, int height) {
        refl = new double[numBands][width][height];
        wv = new double[width][height];

        for (int i = 0; i < numBands; i++) {
            refl[i] = new double[width][height];
            for (int j = 0; j < width; j++) {
                refl[i][j] = new double[height];
            }
        }

        for (int j = 0; j < width; j++) {
            wv[j] = new double[height];
        }
    }

    public double getWvPixel(int x, int y) {
        return wv[x][y];
    }

    public void setWvPixel(int x, int y, double wvValue) {
        wv[x][y] = wvValue;
    }

    public double getReflPixel(int bandId, int x, int y) {
        return refl[bandId][x][y];
    }

    public void setReflPixel(int bandId, int x, int y, double reflValue) {
        refl[bandId][x][y] = reflValue;
    }
}
