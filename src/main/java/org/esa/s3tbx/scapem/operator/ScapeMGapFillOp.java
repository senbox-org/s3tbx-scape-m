package org.esa.s3tbx.scapem.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Operator providing the visibility gap filling as used in IDL breadboard
 *
 * @author Tonio Fincke, Olaf Danne
 */
@OperatorMetadata(alias = "beam.scapeM.gapfill", version = "1.0-SNAPSHOT",
        authors = "Tonio Fincke, Olaf Danne",
        copyright = "(c) 2013 Brockmann Consult",
        internal = true,
        description = "Provides the visibility gap filling as used in IDL breadboard.")
public class ScapeMGapFillOp extends ScapeMMerisBasisOp {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @SourceProduct(alias = "gap")
    private Product gapProduct;

    @TargetProduct
    private Product targetProduct;

    private RectangleExtender rectCalculator;

    private int pixelsPerCell;
    private Band gapVisibilityBand;
    private double visImageMean;
    private int tileWidth;
    private int tileHeight;
    private int numberOfCellColumns;
    private int numberOfCellRows;


    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getProductType().contains("_RR")) {
            pixelsPerCell = ScapeMConstants.RR_PIXELS_PER_CELL;
        } else {
            pixelsPerCell = ScapeMConstants.FR_PIXELS_PER_CELL;
        }

        rectCalculator = new RectangleExtender(
                new Rectangle(sourceProduct.getSceneRasterWidth(),
                        sourceProduct.getSceneRasterHeight()),
                3 * pixelsPerCell,
                3 * pixelsPerCell);

        tileWidth = (int) gapProduct.getPreferredTileSize().getWidth();
        tileHeight = (int) gapProduct.getPreferredTileSize().getHeight();
        numberOfCellColumns = (int) Math.ceil(gapProduct.getSceneRasterWidth() * 1.0 / tileWidth);
        numberOfCellRows = (int) Math.ceil(gapProduct.getSceneRasterHeight() * 1.0 / tileHeight);

        createTargetProduct();

        gapVisibilityBand = gapProduct.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);
        visImageMean = 0.0;
//        visImageMean = getVisibilityImageMean();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final double noDataValue = gapVisibilityBand.getNoDataValue();
        final Rectangle targetRect = targetTile.getRectangle();
        final Rectangle sourceRect = rectCalculator.extend(targetRect);

        final Tile sourceVisibilityTile = getSourceTile(gapVisibilityBand, sourceRect);

        final int tileIndexX = targetRect.x / pixelsPerCell;
        final int tileIndexY = targetRect.y / pixelsPerCell;

        final double visCellOrigValue = sourceVisibilityTile.getSampleDouble(targetRect.x, targetRect.y);

        if (isVisibilityValid(visCellOrigValue)) {
            setCellVisibilitySamples(targetTile, targetRect, visCellOrigValue);
        } else {
            // do gap filling by interpolation
            final int minimumDistanceToEdge = getMinimumDistanceToEdge(tileIndexX,
                    tileIndexY,
                    numberOfCellColumns,
                    numberOfCellRows);

            double visInterpolValue;
            if (minimumDistanceToEdge >= 2) {
                visInterpolValue = interpolateOverRegion(sourceVisibilityTile, tileIndexX, tileIndexY, 2, noDataValue);
            } else if (minimumDistanceToEdge == 1) {
                visInterpolValue = interpolateOverRegion(sourceVisibilityTile, tileIndexX, tileIndexY, 1, noDataValue);
            } else {
                visInterpolValue = interpolateAtCornerOrBorder(numberOfCellColumns, numberOfCellRows,
                        sourceVisibilityTile, tileIndexX, tileIndexY, noDataValue);
            }
            if (visInterpolValue == 0 && minimumDistanceToEdge >= 3) {
                visInterpolValue = interpolateOverRegion(sourceVisibilityTile, tileIndexX, tileIndexY, 3, noDataValue);
            }
            if (visInterpolValue == 0) {
                synchronized (this) {
                    if (visImageMean == 0.0) {
                        visImageMean = getVisibilityImageMean();
                    }
                }
                visInterpolValue = visImageMean;
            }

            setCellVisibilitySamples(targetTile, targetRect, visInterpolValue);
        }
    }

     /* package local for testing*/
    static int getMinimumDistanceToEdge(int x, int y, int numberOfCellColumns, int numberOfCellRows) {
        return Math.min(x, Math.min(y, Math.min(numberOfCellColumns - 1 - x, numberOfCellRows - 1 - y)));
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER", "MER_L2");

        Band visibilityBand = targetProduct.addBand(ScapeMConstants.VISIBILITY_BAND_NAME, ProductData.TYPE_FLOAT32);
        visibilityBand.setNoDataValue(ScapeMConstants.VISIBILITY_NODATA_VALUE);
        visibilityBand.setValidPixelExpression(ScapeMConstants.SCAPEM_VALID_EXPR);
    }

    private double getVisibilityImageMean() {
        double areaMean = 0.0;
        int numberOfValidCells = 0;
        for (int y = 0; y < numberOfCellRows; y++) {
            for (int x = 0; x < numberOfCellColumns; x++) {
                final double cellValue = gapVisibilityBand.getSampleFloat(x * tileWidth, y * tileHeight);
                if (isVisibilityValid(cellValue)) {
                    areaMean += cellValue;
                    numberOfValidCells++;
                }
            }
        }
        return areaMean / numberOfValidCells;
    }

    private boolean isVisibilityValid(double visValue) {
        return !Double.isNaN(visValue) && visValue != ScapeMConstants.VISIBILITY_NODATA_VALUE;
    }

    private void setCellVisibilitySamples(Tile targetTile, Rectangle targetRect, double visibility) {
        for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
            for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                targetTile.setSample(x, y, visibility);
            }
        }
    }

    private float interpolateOverRegion(Tile visSourceTile, int x, int y, int neighboringDistance, double noDataValue) {
        float meanValue = 0;
        int validNeighboringCellsCounter = 0;
        for (int i = -neighboringDistance; i <= neighboringDistance; i++) {
            for (int j = -neighboringDistance; j <= neighboringDistance; j++) {
                final int xAssign = x + i;
                final int yAssign = y + j;
                final double visValue = visSourceTile.getSampleDouble(xAssign * tileWidth, yAssign * tileHeight);
                if (visValue != noDataValue) {
                    meanValue += visValue;
                    validNeighboringCellsCounter++;
                }
            }
        }
        if (validNeighboringCellsCounter > 0) {
            meanValue /= validNeighboringCellsCounter;
        }
        return meanValue;
    }

    private float interpolateAtCornerOrBorder(int numberOfCellColumns, int numberOfCellRows, Tile visSourceTile,
                                              int x, int y, double noDataValue) {
        float mean = 0;
        int validCellsCounter = 0;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final int xAssign = x + i;
                final int yAssign = y + j;
                final int minimumDistanceToEdgeAssign
                        = getMinimumDistanceToEdge(xAssign, yAssign, numberOfCellColumns, numberOfCellRows);
                if (minimumDistanceToEdgeAssign >= 0) {
                    final double visValue = visSourceTile.getSampleDouble(xAssign * tileWidth, yAssign * tileHeight);
                    if (visValue != noDataValue) {
                        mean += visValue;
                        validCellsCounter++;
                        if (minimumDistanceToEdgeAssign == 0 && (i == 0 || j == 0)) {
                            mean += visValue;
                            validCellsCounter++;
                        }
                    }
                }
            }
        }
        if (validCellsCounter > 0) {
            mean /= validCellsCounter;
        }
        return mean;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScapeMGapFillOp.class);
        }
    }
}
