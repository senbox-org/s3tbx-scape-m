package org.esa.s3tbx.scapem.operator;


import Stats.LinFit;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.s3tbx.scapem.io.LutAccess;
import org.esa.s3tbx.scapem.math.Powell;
import org.esa.s3tbx.scapem.util.CellSample;
import org.esa.s3tbx.scapem.util.CellSampleComparator;
import org.esa.s3tbx.scapem.util.ClearPixelStrategy;
import org.esa.s3tbx.scapem.util.ScapeMUtils;
import org.esa.s3tbx.scapem.util.Varsol;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing SCAPE-M algorithm
 *
 * @author Tonio Fincke, Olaf Danne
 */
public class ScapeMAlgorithm {

    /**
     * Determines if cell is regarded as 'clear land' : > 35% must not be water or cloud
     *
     * @param rect               - cell rectangle
     * @param clearPixelStrategy - clearPixelStrategy
     * @return boolean - cell is clear land or not
     */
    static boolean isCellClearLand(Rectangle rect,
                                   ClearPixelStrategy clearPixelStrategy,
                                   double percentage) {
        int countClearLand = 0;
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                if (clearPixelStrategy.isValid(x, y)) {   // mask_land_all !!
                    countClearLand++;
                }
            }
        }
        return countClearLand / (rect.getWidth() * rect.getHeight()) > percentage;
    }

    /**
     * Returns the elevation mean value (in km) over all land pixels in a 30x30km cell
     *
     * @param hSurfCell          - hsurf single values
     * @param clearPixelStrategy - strategy how clear pixels are determined
     * @return double - the cell mean value
     */
    static double getHsurfMeanCell(double[][] hSurfCell,
                                   Rectangle rectangle,
                                   ClearPixelStrategy clearPixelStrategy) {

        double hsurfMean = 0.0;
        int hsurfCount = 0;
        for (int y = 0; y < hSurfCell[0].length; y++) {
            for (int x = 0; x < hSurfCell.length; x++) {
                if (!(Double.isNaN(hSurfCell[x][y]))) {
                    if (clearPixelStrategy.isValid(rectangle.x + x, rectangle.y + y)) {
                        hsurfMean += hSurfCell[x][y];
                        hsurfCount++;
                    }
                }
            }
        }

        return hsurfMean / hsurfCount;    // km
    }

    /**
     * Returns the elevation array in a 30x30km cell
     *
     * @param rect      - the tile rectangle
     * @param geoCoding - the geo coding
     * @param demTile   - the DEM tile
     * @param scapeMLut - the atmospheric look-up table
     * @return double[][] - the elevation array
     */
    static double[][] getHsurfArrayCell(Rectangle rect,
                                        GeoCoding geoCoding,
                                        Tile demTile,
                                        ScapeMLut scapeMLut) {

        double[][] hSurf = new double[rect.width][rect.height];
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                if (geoCoding.canGetGeoPos()) {
                    double demValue = demTile.getSampleDouble(x, y);
                    hSurf[x - rect.x][y - rect.y] = Math.max(scapeMLut.getHsfMin(), 0.001 * demValue);
                } else {
                    hSurf[x - rect.x][y - rect.y] = scapeMLut.getHsfMin();
                }
                hSurf[x - rect.x][y - rect.y] =
                        Math.max(scapeMLut.getHsfMin(), Math.min(scapeMLut.getHsfMax(), hSurf[x - rect.x][y - rect.y]));
            }
        }
        return hSurf;
    }

    /**
     * Returns the elevation array in a 30x30km cell
     *
     * @param rect           - the tile rectangle
     * @param geoCoding      - the geo coding
     * @param elevationModel - the elevation model
     * @param scapeMLut      - the atmospheric look-up table
     * @return double[][] - the elevation array
     */
    static double[][] getHsurfArrayCell(Rectangle rect,
                                        GeoCoding geoCoding,
                                        ElevationModel elevationModel,
                                        ScapeMLut scapeMLut) {

        double[][] hSurf = new double[rect.width][rect.height];
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                GeoPos geoPos;
                if (geoCoding.canGetGeoPos()) {
                    geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
                    try {
                        hSurf[x - rect.x][y - rect.y] = 0.001 * elevationModel.getElevation(geoPos);
                    } catch (Exception e) {
                        hSurf[x - rect.x][y - rect.y] = scapeMLut.getHsfMin();
                    }
                } else {
                    hSurf[x - rect.x][y - rect.y] = scapeMLut.getHsfMin();
                }
                hSurf[x - rect.x][y - rect.y] =
                        Math.max(scapeMLut.getHsfMin(), Math.min(scapeMLut.getHsfMax(), hSurf[x - rect.x][y - rect.y]));
            }
        }
        return hSurf;
    }

    /**
     * Returns the cos(SZA) mean value over all land pixels in a 30x30km cell
     *
     * @param cosSzaCell         - hsurf single values
     * @param clearPixelStrategy - strategy how clear pixels are determined
     * @return double - the cell mean value
     */
    static double getCosSzaMeanCell(double[][] cosSzaCell,
                                    Rectangle rect,
                                    ClearPixelStrategy clearPixelStrategy) {

        double cosSzaMean = 0.0;
        int cosSzaCount = 0;
        for (int y = 0; y < cosSzaCell[0].length; y++) {
            for (int x = 0; x < cosSzaCell.length; x++) {
                if (!(Double.isNaN(cosSzaCell[x][y]))) {
                    if (clearPixelStrategy.isValid(rect.x + x, rect.y + y)) {
                        cosSzaMean += cosSzaCell[x][y];
                        cosSzaCount++;
                    }
                }
            }
        }

        return cosSzaMean / cosSzaCount;
    }

    /**
     * Returns the cos(SZA) array in a 30x30km cell
     *
     * @param rect - the tile rectangle
     * @return double[][] - the cos(SZA) array
     */
    static double[][] getCosSzaArrayCell(Rectangle rect,
                                         Tile szaTile) {

        double[][] cosSza = new double[rect.width][rect.height];
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                final double sza = szaTile.getSampleDouble(x, y);
                cosSza[x - rect.x][y - rect.y] = Math.cos(sza * MathUtils.DTOR);
            }
        }

        return cosSza;
    }

    /**
     * Returns the TOA minimum value in a 30x30km cell
     *
     * @param toaArrayCell - the TOA array
     * @return double - the cell minimum value
     */
    static double getToaMinCell(double[][] toaArrayCell) {
        double toaMin = Double.MAX_VALUE;
        for (int y = 0; y < toaArrayCell[0].length; y++) {
            for (int x = 0; x < toaArrayCell.length; x++) {
                if (!(Double.isNaN(toaArrayCell[x][y])) && toaArrayCell[x][y] > 0.0) {
                    if (toaArrayCell[x][y] < toaMin) {
                        toaMin = toaArrayCell[x][y];
                    }
                }
            }
        }
        return toaMin;
    }

    /**
     * Returns the TOA array in a 30x30km cell
     *
     * @param radianceTile - the input radiances
     * @param rect         - the tile rectangle
     * @param doy          - the day of year
     * @return double[][] - the TOA array
     */
    static double[][] getToaArrayCell(Tile radianceTile,
                                      Rectangle rect,
                                      int doy) {

        double[][] toa = new double[rect.width][rect.height];
        double varSol = Varsol.getVarSol(doy);
        final double solFactor = varSol * varSol * 1.E-4;

        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                toa[x - rect.x][y - rect.y] = radianceTile.getSampleDouble(x, y) * solFactor;
            }
        }
        return toa;
    }

    /**
     * gets the visibility for a 30x30km cell
     *
     * @param toaArrayCell         - toa refl single values
     * @param toaMinCell           - toa min cell value
     * @param vza                  - vza
     * @param sza                  - sza
     * @param raa                  - raa
     * @param hsurfArrayCell       - hsurf single values
     * @param hsurfMeanCell        - hsurf mean cell value
     * @param cosSzaArrayCell      - cosSza singe values
     * @param cosSzaMeanCell       - cosSza mean cell value
     * @param cellIsClear45Percent - true if cell is > 45% clea land
     * @return double - the visibility
     */
    static double getCellVisibility(double[][][] toaArrayCell,
                                    double[] toaMinCell, double vza, double sza, double raa,
                                    double[][] hsurfArrayCell,
                                    double hsurfMeanCell,
                                    double[][] cosSzaArrayCell, // mus_il_sub
                                    double cosSzaMeanCell, // mus_il
                                    boolean cellIsClear45Percent,
                                    ScapeMLut scapeMLut) {

        final int nVis = scapeMLut.getVisArrayLUT().length;
        final double[] step = {1.0, 0.1};
        final double wvInit = 2.0;

        double vis = scapeMLut.getVisMin() - step[0];
        double[][] fInt;
        for (int i = 0; i <= 1; i++) {
            if (i == 1) {
                vis = Math.max(vis - step[0], scapeMLut.getVisMin());
            }
            boolean repeat = true;
            while (((vis + step[i]) < scapeMLut.getVisMax()) && repeat) {
                vis += step[i];
                fInt = LutAccess.interpolAtmParamLut(scapeMLut.getAtmParamLut(), vza, sza, raa, hsurfMeanCell, vis, wvInit);
                repeat = false;
                for (int j = 0; j < nVis; j++) {
                    if (toaMinCell[j] <= fInt[j][0]) {
                        repeat = true;
                    }
                }
            }
        }

        double visVal = vis - step[1];

        if (cellIsClear45Percent) {
            double[][] refPixelsBand0 =
                    extractRefPixels(0, hsurfArrayCell, hsurfMeanCell, cosSzaArrayCell, cosSzaMeanCell, toaArrayCell);
            if (refPixelsBand0 != null && refPixelsBand0.length > 0) {
                double[][][] refPixels = new double[ScapeMConstants.L1_BAND_NUM][refPixelsBand0.length][refPixelsBand0[0].length];
                refPixels[0] = refPixelsBand0;

                boolean invalid = false;
                for (int bandId = 1; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                    refPixels[bandId] =
                            extractRefPixels(bandId, hsurfArrayCell, hsurfMeanCell, cosSzaArrayCell, cosSzaMeanCell, toaArrayCell);
                    if (refPixels[bandId] == null && refPixels[bandId].length > 0) {
                        invalid = true; // we want valid pixels in ALL bands
                        break;
                    }
                }
                if (!invalid) {
                    visVal = computeRefinedVisibility(visVal, refPixels, vza, sza, raa, hsurfMeanCell, wvInit,
                            cosSzaMeanCell, scapeMLut);
                }
            } else {
                // nothing to do - keep visVal as it was before
            }
        } else {
            // nothing to do - keep visVal as it was before
        }

        visVal = Math.max(scapeMLut.getVisMin(), Math.min(scapeMLut.getVisMax(), visVal));

        return visVal;
    }

    /**
     * for given bandId, gives TOA for reference pixels selected from NDVI criteria
     *
     * @param bandId          - band ID
     * @param hsurfArrayCell  - hsurf single values
     * @param hsurfMeanCell   - hsurf mean cell value
     * @param cosSzaArrayCell - cosSza single values
     * @param cosSzaMeanCell  - cosSza mean cell values
     * @param toaArrayCell    - toa single values
     * @return double[][]     - the reference pixels, refPixels = double[selectedPixels][NUM_REF_PIXELS]
     */
    static double[][] extractRefPixels(int bandId, double[][] hsurfArrayCell, double hsurfMeanCell,
                                       double[][] cosSzaArrayCell, double cosSzaMeanCell, double[][][] toaArrayCell) {

        final int cellWidth = toaArrayCell[0].length;
        final int cellHeight = toaArrayCell[0][0].length;

        final double[] hsurfLim = new double[]{0.8 * hsurfMeanCell, 1.2 * hsurfMeanCell};
        final double[] cosSzaLim = new double[]{0.9 * cosSzaMeanCell, 1.1 * cosSzaMeanCell};

        double[][] ndvi = new double[cellWidth][cellHeight];
        List<CellSample> ndviHighList = new ArrayList<CellSample>();
        List<CellSample> ndviMediumList = new ArrayList<CellSample>();
        List<CellSample> ndviLowList = new ArrayList<CellSample>();
        for (int j = 0; j < cellHeight; j++) {
            for (int i = 0; i < cellWidth; i++) {
                final double toa7 = toaArrayCell[7][i][j] / ScapeMConstants.solIrr7;
                final double toa9 = toaArrayCell[9][i][j] / ScapeMConstants.solIrr9;
                ndvi[i][j] = (toa9 - toa7) / (toa9 + toa7);
                if (hsurfArrayCell[i][j] > hsurfLim[0] && hsurfArrayCell[i][j] < hsurfLim[1] &&
                        cosSzaArrayCell[i][j] > cosSzaLim[0] && cosSzaArrayCell[i][j] < cosSzaLim[1]) {
                    if (ndvi[i][j] >= 0.4 && ndvi[i][j] < 0.9) {
                        ndviHighList.add(new CellSample(i, j, ndvi[i][j]));
                    } else if (ndvi[i][j] >= 0.15 && ndvi[i][j] < 0.4) {
                        ndviMediumList.add(new CellSample(i, j, ndvi[i][j]));
                    } else if (ndvi[i][j] >= 0.09 && ndvi[i][j] < 0.15) {
                        ndviLowList.add(new CellSample(i, j, ndvi[i][j]));
                    }
                }
            }
        }

        // sort NDVIs...
        CellSampleComparator comparator = new CellSampleComparator(true);
        CellSample[] ndviHighSamples = ndviHighList.toArray(new CellSample[ndviHighList.size()]);
        Arrays.sort(ndviHighSamples, comparator);
        CellSample[] ndviMediumSamples = ndviMediumList.toArray(new CellSample[ndviMediumList.size()]);
        Arrays.sort(ndviMediumSamples, comparator);
        CellSample[] ndviLowSamples = ndviLowList.toArray(new CellSample[ndviLowList.size()]);
        Arrays.sort(ndviLowSamples, comparator);

        final int nLim = Math.min(ndviHighSamples.length / 2, ndviMediumSamples.length / 3);
        double[][] refPixels = new double[nLim][ScapeMConstants.NUM_REF_PIXELS];

        if (ndviMediumSamples.length + 2 >= ScapeMConstants.NUM_REF_PIXELS) {
            //                valid_flg = 1

            for (int i = 0; i < nLim; i++) {
                refPixels[i][0] =
                        toaArrayCell[bandId][ndviHighSamples[2 * i].getCellXIndex()][ndviHighSamples[2 * i].getCellYIndex()];
                refPixels[i][1] =
                        toaArrayCell[bandId][ndviHighSamples[2 * i + 1].getCellXIndex()][ndviHighSamples[2 * i + 1].getCellYIndex()];

                refPixels[i][2] =
                        toaArrayCell[bandId][ndviMediumSamples[2 * i].getCellXIndex()][ndviMediumSamples[2 * i].getCellYIndex()];
                refPixels[i][3] =
                        toaArrayCell[bandId][ndviMediumSamples[2 * i + 1].getCellXIndex()][ndviMediumSamples[2 * i + 1].getCellYIndex()];

                if (i < ndviLowSamples.length) {
                    refPixels[i][4] =
                            toaArrayCell[bandId][ndviLowSamples[i].getCellXIndex()][ndviLowSamples[i].getCellYIndex()];
                } else {
                    refPixels[i][4] =
                            toaArrayCell[bandId][ndviMediumSamples[2 * i + 2].getCellXIndex()][ndviMediumSamples[2 * i + 2].getCellYIndex()];
                }
            }
        } else {
            return null;
        }

        return refPixels;
    }

    /**
     * Returns the AOT at 550nm for a 30x30km cell
     *
     * @param visibility - the visibility
     * @param hsurf      - the elevation
     * @param scapeMLut  - the atmospheric look-up table
     * @return double - the AOT
     */
    static double getCellAot550(double visibility, double hsurf, ScapeMLut scapeMLut) {

        double aot550;

        double lnVis = Math.log(visibility);

        double[][] aCoeff = new double[scapeMLut.getHsfArrayLUT().length][2];
        double[] lnVisGr = new double[scapeMLut.getVisArrayLUT().length];
        double[] lnAotGr = new double[scapeMLut.getVisArrayLUT().length];
        for (int i = 0; i < lnVisGr.length; i++) {
            lnVisGr[i] = Math.log(scapeMLut.getVisArrayLUT()[i]);
        }

        for (int i = 0; i < scapeMLut.getHsfArrayLUT().length; i++) {
            for (int j = 0; j < lnVisGr.length; j++) {
                lnAotGr[j] = Math.log(ScapeMConstants.AOT_GRID[i][j]);
            }
            final LinFit linFit = new LinFit(lnVisGr, lnAotGr, lnVisGr.length);
            aCoeff[i][0] = linFit.getA();
            aCoeff[i][1] = linFit.getB();
        }

        int hsfIndexToUse = -1;
        for (int i = 0; i < scapeMLut.getHsfArrayLUT().length; i++) {
            if (hsurf >= scapeMLut.getHsfArrayLUT()[i]) {
                hsfIndexToUse = i;
            }
        }
        if (hsfIndexToUse >= 0) {
            double hsp = (hsurf - scapeMLut.getHsfArrayLUT()[hsfIndexToUse]) /
                    (scapeMLut.getHsfArrayLUT()[hsfIndexToUse + 1] - scapeMLut.getHsfArrayLUT()[hsfIndexToUse]);
            double aotTmp1 = Math.exp(aCoeff[hsfIndexToUse][0] + aCoeff[hsfIndexToUse][1] * lnVis);
            double aotTmp2 = Math.exp(aCoeff[hsfIndexToUse + 1][0] + aCoeff[hsfIndexToUse + 1][1] * lnVis);
            aot550 = aotTmp1 + (aotTmp2 - aotTmp1) * hsp;
        } else {
            aot550 = ScapeMConstants.AOT_NODATA_VALUE;
        }

        return aot550;

    }

    /**
     * Returns the 'reflectance images' used for atmospheric correction
     *
     * @param fInt            - the LUT output parameters
     * @param toaArray        - the TOA cell arrays for all wavelengths
     * @param cosSzaArrayCell - the cos(SZA) cell array
     * @return double[][][] - the 'reflectance images': cell arrays for all wavelengths
     */
    static double[][][] getReflImage(double[][] fInt,
                                     double[][][] toaArray,
                                     double[][] cosSzaArrayCell) {

        final double deltaX =
                1.0 / (ScapeMConstants.MERIS_WAVELENGTHS[13] - ScapeMConstants.MERIS_WAVELENGTHS[12]);

        double[][][] reflImage = new double[3][toaArray[0].length][toaArray[0][0].length];
        for (int i = 0; i < toaArray[0].length; i++) {
            for (int j = 0; j < toaArray[0][0].length; j++) {
                for (int k = 12; k <= 13; k++) {
                    final double xterm = Math.PI * (toaArray[k][i][j] - fInt[k][0]) /
                            (fInt[k][1] * cosSzaArrayCell[i][j] + fInt[k][2]);
                    reflImage[k - 12][i][j] = xterm / (1.0 + fInt[k][4] * xterm);
                }
                reflImage[2][i][j] =
                        ((reflImage[1][i][j] - reflImage[0][i][j]) * ScapeMConstants.MERIS_WAVELENGTHS[14] +
                                reflImage[0][i][j] * ScapeMConstants.MERIS_WAVELENGTHS[13] -
                                reflImage[1][i][j] * ScapeMConstants.MERIS_WAVELENGTHS[12]) * deltaX;
            }
        }

        return reflImage;
    }


    /**
     * @param rect               - the target rectangle
     * @param visibilityTile     - the visibility tile
     * @param clearPixelStrategy - strategy how clear pixels are determined
     * @param useConstantWv      - use constant Wv if set
     * @param toaArrayCell       - the TOA cell array
     * @param hsurfArray         - the elevation cell array
     * @param cosSzaArray        - the cos(SZA) cell array
     * @param cosSzaMeanCell     - the cos(SZA) cell mean value
     * @param reflImg            - the 'reflectance images' for all wavelengths
     * @param radianceTile13     - radiance tile at band 13
     * @param radianceTile14     - radiance tile at band 14
     * @param scapeMLut          - the atmospheric look-up table
     * @param lpw                - the 'lpw' term of radiative transfer equation
     * @param e0tw               - the 'e0tw' term of radiative transfer equation
     * @param ediftw             - the 'ediftw' term of radiative transfer equation
     * @param tDirD              - the 'tDirD' term of radiative transfer equation
     * @param sab                - the 'sab' term of radiative transfer equation
     * @return ScapeMResult: holding water vapour and atmospheric corrected reflectances (see {@link ScapeMResult})
     */
    static ScapeMResult computeAcResult(Rectangle rect,
                                        Tile visibilityTile,
                                        ClearPixelStrategy clearPixelStrategy,
                                        boolean useConstantWv,
                                        double[][][] toaArrayCell,
                                        double[][] hsurfArray,
                                        double[][] cosSzaArray,
                                        double cosSzaMeanCell,
                                        double[][][] reflImg,
                                        Tile radianceTile13,
                                        Tile radianceTile14,
                                        ScapeMLut scapeMLut,
                                        double[][][][] lpw,
                                        double[][][][] e0tw,
                                        double[][][][] ediftw,
                                        double[][][][] tDirD,
                                        double[][][][] sab) {

        final int dimWv = scapeMLut.getCwvArrayLUT().length;
        final int dimVis = scapeMLut.getVisArrayLUT().length;
        final int dimHsurf = scapeMLut.getHsfArrayLUT().length;

        ScapeMResult scapeMResult = new ScapeMResult(ScapeMConstants.L1_BAND_NUM, rect.width, rect.height);

        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {

                final double pix1 = reflImg[1][x - rect.x][y - rect.y];
                final double pix2 = reflImg[2][x - rect.x][y - rect.y];
                final double demPix = hsurfArray[x - rect.x][y - rect.y];
                final double visPix = visibilityTile.getSampleDouble(x, y);

                if (clearPixelStrategy.isValid(x, y)) {
                    final double ratioMeris =
                            radianceTile14.getSampleDouble(x, y) / radianceTile13.getSampleDouble(x, y);
                    final double[] reflPix = new double[]{pix1, pix2};

                    final double[] hsfArrayLUT = scapeMLut.getHsfArrayLUT();
                    int hsIndex = 0;
                    for (int i = 0; i < dimHsurf - 1; i++) {
                        if (demPix >= hsfArrayLUT[i]) {
                            hsIndex = i;
                        }
                    }
                    double hsP = 0.0;
                    if (hsIndex >= 0 && hsIndex < hsfArrayLUT.length - 1) {
                        hsP = (demPix - hsfArrayLUT[hsIndex]) /
                                (hsfArrayLUT[hsIndex + 1] - hsfArrayLUT[hsIndex]);
                    }

                    int visIndex = 0;
                    final double[] visArrayLUT = scapeMLut.getVisArrayLUT();
                    for (int i = 0; i < dimVis - 1; i++) {
                        if (visPix >= visArrayLUT[i]) {
                            visIndex = i;
                        }
                    }

                    double visP = 0.0;
                    if (visIndex >= 0 && visIndex < visArrayLUT.length - 1) {
                        visP = (visPix - visArrayLUT[visIndex]) /
                                (visArrayLUT[visIndex + 1] - visArrayLUT[visIndex]);
                    }

                    double[][] lpwSp = new double[ScapeMConstants.L1_BAND_NUM][dimWv];
                    for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                        for (int i = 0; i < dimWv; i++) {
                            lpwSp[bandId][i] = (1.0 - visP) * (1.0 - hsP) * lpw[bandId][i][visIndex][hsIndex] +
                                    hsP * (1.0 - visP) * lpw[bandId][i][visIndex][hsIndex + 1] +
                                    (1.0 - hsP) * visP * lpw[bandId][i][visIndex + 1][hsIndex] +
                                    visP * hsP * lpw[bandId][i][visIndex + 1][hsIndex + 1];
                        }

                    }

                    // adjust etw:
                    double[][][][] etw = new double[ScapeMConstants.L1_BAND_NUM][dimWv][dimVis][dimHsurf];
                    final double cosSza = cosSzaArray[x - rect.x][y - rect.y];
                    for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                        for (int i = 0; i < scapeMLut.getCwvArrayLUT().length; i++) {
                            for (int j = 0; j < visArrayLUT.length; j++) {
                                for (int k = 0; k < hsfArrayLUT.length; k++) {
                                    // (1.- tdir_d * mus) * mun_term_arr[ind]:
                                    final double sum1 = (1.0 - tDirD[bandId][i][j][k] * cosSzaMeanCell) * 1.0;   // this is less precise, but follows IDL
//                                    final double sum1 = (1.0 - tDirD[bandId][i][j][k] * cosSza) * 1.0;
                                    // tdir_d * mus_il_arr[ind] :
                                    final double sum2 = tDirD[bandId][i][j][k] * cosSza;
                                    // e0tw * mus_il_arr[ind] :
                                    final double sum3 = e0tw[bandId][i][j][k] * cosSza;

                                    etw[bandId][i][j][k] = sum3 + ediftw[bandId][i][j][k] * (sum2 + sum1);
                                }
                            }
                        }
                    }

                    double[][] etwSp = new double[ScapeMConstants.L1_BAND_NUM][dimWv];
                    double[][] sabSp = new double[ScapeMConstants.L1_BAND_NUM][dimWv];
                    for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                        for (int i = 0; i < dimWv; i++) {
                            etwSp[bandId][i] = (1.0 - visP) * (1.0 - hsP) * etw[bandId][i][visIndex][hsIndex] +
                                    hsP * (1.0 - visP) * etw[bandId][i][visIndex][hsIndex + 1] +
                                    (1.0 - hsP) * visP * etw[bandId][i][visIndex + 1][hsIndex] +
                                    visP * hsP * etw[bandId][i][visIndex + 1][hsIndex + 1];
                            sabSp[bandId][i] = (1.0 - visP) * (1.0 - hsP) * sab[bandId][i][visIndex][hsIndex] +
                                    hsP * (1.0 - visP) * sab[bandId][i][visIndex][hsIndex + 1] +
                                    (1.0 - hsP) * visP * sab[bandId][i][visIndex + 1][hsIndex] +
                                    visP * hsP * sab[bandId][i][visIndex + 1][hsIndex + 1];
                        }

                    }

                    double[][][] parAtmH = new double[3][2][dimWv];
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < dimWv; j++) {
                            parAtmH[0][i][j] = lpwSp[i + 13][j];
                            parAtmH[1][i][j] = etwSp[i + 13][j];
                            parAtmH[2][i][j] = sabSp[i + 13][j];
                        }
                    }

                    // now water vapour:
                    // all numbers in this test taken from IDL test run, cellIndexX=1, cellIndexY=0
                    double wvResult = ScapeMConstants.WV_INIT;
                    double wvP = ScapeMConstants.WV_INIT;
                    int wvInf = dimWv / 2;
                    if (!useConstantWv) {
                        WaterVapourFunction wvFunction = new WaterVapourFunction();
                        wvFunction.setMerisRatio(ratioMeris);
                        wvFunction.setWvGr2(scapeMLut.getCwvArrayLUT());
                        wvFunction.setParAtmH(parAtmH);
                        wvFunction.setReflPix(reflPix);

                        final double wvLower = scapeMLut.getCwvMin();
                        final double wvUpper = scapeMLut.getCwvMax();

                        BrentSolver brentSolver = new BrentSolver(ScapeMConstants.FTOL);
                        try {
                            wvResult = brentSolver.solve(ScapeMConstants.MAXITER, wvFunction,
                                    wvLower, wvUpper);
                            wvInf = wvFunction.getWvInf();
                            wvP = wvFunction.getWvP();
                        } catch (NoBracketingException e) {
                            // retrieval outside valid range, set to default value
                            wvResult = ScapeMConstants.WV_INIT;
                            wvP = ScapeMConstants.WV_INIT;
                            wvInf = dimWv / 2;
                            // todo: check if flag should be raised
//                    e.printStackTrace();
                        }
                    }
                    scapeMResult.setWvPixel(x - rect.x, y - rect.y, wvResult);

                    double[] lpwAc = new double[ScapeMConstants.L1_BAND_NUM];
                    double[] etwAc = new double[ScapeMConstants.L1_BAND_NUM];
                    double[] sabAc = new double[ScapeMConstants.L1_BAND_NUM];
                    for (int i = 0; i < ScapeMConstants.L1_BAND_NUM; i++) {
                        if (i != 10 && i != 14) {
                            lpwAc[i] = lpwSp[i][wvInf] + wvP * (lpwSp[i][wvInf + 1] - lpwSp[i][wvInf]);
                            etwAc[i] = etwSp[i][wvInf] + wvP * (etwSp[i][wvInf + 1] - etwSp[i][wvInf]);
                            sabAc[i] = sabSp[i][wvInf] + wvP * (sabSp[i][wvInf + 1] - sabSp[i][wvInf]);

                            final double xTerm =
                                    Math.PI * (toaArrayCell[i][x - rect.x][y - rect.y] - lpwAc[i]) / etwAc[i];
                            final double refl = xTerm / (1.0 + sabAc[i] * xTerm);
                            scapeMResult.setReflPixel(i, x - rect.x, y - rect.y, refl);
                        }
                    }
                } else {
                    // invalid due to one or more of the above cases
                    for (int i = 0; i < ScapeMConstants.L1_BAND_NUM; i++) {
                        scapeMResult.setReflPixel(i, x - rect.x, y - rect.y, ScapeMConstants.AC_NODATA);
                    }
                    scapeMResult.setWvPixel(x - rect.x, y - rect.y, ScapeMConstants.AC_NODATA);
                }
            }

        }
        return scapeMResult;
    }

    // computes the 'refined' visibility value for the given cell:
    private static double computeRefinedVisibility(double visLim,
                                                   double[][][] refPixels,
                                                   double vza, double sza, double raa,
                                                   double hsurfMeanCell,
                                                   double wvInit,
                                                   double cosSzaMeanCell,
                                                   ScapeMLut scapeMLut) {

        final int numSpec = 2;
        final int numX = numSpec * ScapeMConstants.NUM_REF_PIXELS + 1;

        double[] powellInputInit = new double[numX];

        double visRefined;

        double[][] lpw = new double[ScapeMConstants.L1_BAND_NUM][scapeMLut.getVisArrayLUT().length];
        double[][] etw = new double[ScapeMConstants.L1_BAND_NUM][scapeMLut.getVisArrayLUT().length];
        double[][] sab = new double[ScapeMConstants.L1_BAND_NUM][scapeMLut.getVisArrayLUT().length];

        for (int i = 0; i < scapeMLut.getVisArrayLUT().length; i++) {
            double visArrayVal = Math.max(scapeMLut.getVisMin(), Math.min(scapeMLut.getVisMax(), scapeMLut.getVisArrayLUT()[i]));
            double[][] fInt = LutAccess.interpolAtmParamLut(scapeMLut.getAtmParamLut(), vza, sza, raa, hsurfMeanCell, visArrayVal, wvInit);
            for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                lpw[bandId][i] = fInt[bandId][0];
                etw[bandId][i] = fInt[bandId][1] * cosSzaMeanCell + fInt[bandId][2];
                sab[bandId][i] = fInt[bandId][4];
            }
        }

        for (int j = 0; j < ScapeMConstants.NUM_REF_PIXELS; j++) {
            final double ndvi = (refPixels[12][0][j] - refPixels[7][0][j]) / (refPixels[12][0][j] + refPixels[7][0][j]);
            final double ndviMod = 1.3 * ndvi + 0.25;
            powellInputInit[numSpec * j] = Math.max(ndviMod, 0.0);
            powellInputInit[numSpec * j + 1] = Math.max(1.0 - ndviMod, 0.0);
        }
        powellInputInit[numX - 1] = 23.0;

        double[][] xi = new double[numX][numX];
        for (int i = 0; i < numX; i++) {
            xi[i][i] = 1.0;
        }

        final int limRefSets = 1;    // for AOT_time_flg eq 1, see .inp file
        final int nEMVeg = 3;    // for AOT_time_flg eq 1, see .inp file

        final int nRefSets = Math.min(refPixels[0].length, limRefSets);

        double[] visArr = new double[nRefSets];
        double[] fminArr = new double[nEMVeg];
        double[] visArrAux = new double[nEMVeg];

        ToaMinimization toaMinimization = new ToaMinimization(visLim, scapeMLut.getVisArrayLUT(), lpw, etw, sab, 0.0);
        final double[][] xiInput = xi.clone();
        for (int i = 0; i < nRefSets; i++) {
            double[][] refSetPixels = new double[ScapeMConstants.L1_BAND_NUM][ScapeMConstants.NUM_REF_PIXELS];
            for (int j = 0; j < ScapeMConstants.L1_BAND_NUM; j++) {
                System.arraycopy(refPixels[j][i], 0, refSetPixels[j], 0, ScapeMConstants.NUM_REF_PIXELS);
            }
            toaMinimization.setRefPixels(refSetPixels);

            for (int j = 0; j < nEMVeg; j++) {
                double[] xVector = powellInputInit.clone();
                xVector[10] = visLim + 0.01;

                double[] weight = new double[]{2., 2., 1.5, 1.5, 1.};
                toaMinimization.setWeight(weight);
                toaMinimization.setRhoVeg(ScapeMConstants.RHO_VEG_ALL[j]);

                // 'minim_TOA' is the function to be minimized by Powell!
                // we have to  use this kind of interface:
                // PowellTestFunction_1 function1 = new PowellTestFunction_1();
                // double fmin = Powell.fmin(xVector, xi, ftol, function1);
                double fmin = Powell.fmin(xVector,
                        xiInput,
                        ScapeMConstants.POWELL_FTOL,
                        toaMinimization);
                double[] chiSqr = toaMinimization.getChiSquare();
                double chiSqrMean = ScapeMUtils.getMeanDouble1D(chiSqr);

                int chiSqrOutsideRangeCount = 0;
                for (double aChiSqrVal : chiSqr) {
                    if (aChiSqrVal > 2.0 * chiSqrMean) {
                        chiSqrOutsideRangeCount++;
                    }
                }
                if (chiSqrOutsideRangeCount > 0) {
                    for (int k = 0; k < chiSqrOutsideRangeCount; k++) {
                        weight[k] = 0.0;
                    }
                    toaMinimization.setWeight(weight);
                    fmin = Powell.fmin(xVector,
                            xiInput,
                            ScapeMConstants.POWELL_FTOL,
                            toaMinimization);
                }
                visArrAux[j] = xVector[numX - 1];
                fminArr[j] = fmin / (5.0 - chiSqrOutsideRangeCount);
            }
            final int fMinIndex = ScapeMUtils.getMinimumIndexDouble1D(fminArr);
            visArr[i] = visArrAux[fMinIndex];
        }

        if (nRefSets > 1) {
            double visMean = ScapeMUtils.getMeanDouble1D(visArr);
            double visStdev = ScapeMUtils.getStdevDouble1D(visArr);

            List<Double> visArrInsideStdevList = new ArrayList<Double>();
            for (int i = 0; i < nRefSets; i++) {
                if (Math.abs(visArr[i] - visMean) <= 1.5 * visStdev) {
                    visArrInsideStdevList.add(visArr[i]);
                }
            }
            Double[] visArrInsideStdev = visArrInsideStdevList.toArray(new Double[visArrInsideStdevList.size()]);
            if (visArrInsideStdev.length > 0) {
                visRefined = ScapeMUtils.getMeanDouble1D(visArrInsideStdev);
            } else {
                visRefined = visMean;
            }
        } else {
            visRefined = visArr[0];
        }
        return visRefined;
    }

}
