package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class ScapeMGapFillTest {

    @Test
    public void testGetMinimumDistanceToEdge() {
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(0, 0, 5, 5), 1e-8);
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(0, 1, 5, 5), 1e-8);
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(1, 0, 5, 5), 1e-8);
        assertEquals(1, ScapeMGapFill.getMinimumDistanceToEdge(1, 1, 5, 5), 1e-8);
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(4, 4, 5, 5), 1e-8);
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(3, 4, 5, 5), 1e-8);
        assertEquals(0, ScapeMGapFill.getMinimumDistanceToEdge(4, 3, 5, 5), 1e-8);
        assertEquals(1, ScapeMGapFill.getMinimumDistanceToEdge(3, 3, 5, 5), 1e-8);
        assertEquals(-1, ScapeMGapFill.getMinimumDistanceToEdge(-1, 3, 5, 5), 1e-8);
        assertEquals(-1, ScapeMGapFill.getMinimumDistanceToEdge(3, -1, 5, 5), 1e-8);
        assertEquals(-1, ScapeMGapFill.getMinimumDistanceToEdge(3, 5, 5, 5), 1e-8);
        assertEquals(-1, ScapeMGapFill.getMinimumDistanceToEdge(5, 3, 5, 5), 1e-8);
        assertEquals(2, ScapeMGapFill.getMinimumDistanceToEdge(2, 2, 5, 5), 1e-8);
        assertEquals(2, ScapeMGapFill.getMinimumDistanceToEdge(3, 3, 6, 6), 1e-8);
        assertEquals(3, ScapeMGapFill.getMinimumDistanceToEdge(3, 3, 7, 7), 1e-8);
    }

    @Test
    public void testScapeMInterpolateOverRegion() {
        double noDataValue = -1;
        float[][] cellSamples = {{1, 2, 3}, {4, -1, 6}, {7, 8, 9}};
        assertEquals(5, ScapeMGapFill.interpolateOverRegion(cellSamples, 1, 1, 1, noDataValue), 1e-8);

        cellSamples = new float[][]{{1, 2, 3}, {-1, -1, 6}, {6, 8, 9}};
        assertEquals(5, ScapeMGapFill.interpolateOverRegion(cellSamples, 1, 1, 1, noDataValue), 1e-8);

        cellSamples = new float[][]{{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, -1, 14, 15},
                {16, 17, 18, 19, 20}, {21, 22, 23, 24, 25}};
        assertEquals(13, ScapeMGapFill.interpolateOverRegion(cellSamples, 2, 2, 2, noDataValue), 1e-8);

        cellSamples = new float[][]{{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15},
                {16, 17, 18, 19, 20}, {21, 22, 23, 24, 25}};
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                final float temp = cellSamples[i][j];
                cellSamples[i][j] = -1;
                assertEquals(temp, ScapeMGapFill.interpolateOverRegion(cellSamples, i, j, 1, noDataValue), 1e-8);
                cellSamples[i][j] = temp;
            }
        }

        cellSamples = new float[][]{{1, 2, 3, 4, 5}, {6, 7, -1, 9, 10}, {11, 12, -1, 14, 15},
                {16, 17, 18, 19, 20}, {21, 22, 18, 24, 25}};
        assertEquals(13, ScapeMGapFill.interpolateOverRegion(cellSamples, 2, 2, 2, noDataValue), 1e-8);
    }

    @Test
    public void testScapeMInterpolationAtCorner() {
        double noDataValue = -1;
        float[][] cellSamples = {{-1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        assertEquals(3.4, ScapeMGapFill.interpolateAtCornerOrBorder(3, 3, cellSamples, 0, 0, noDataValue), 1e-7);
        cellSamples = new float[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, -1}};
        assertEquals(6.6, ScapeMGapFill.interpolateAtCornerOrBorder(3, 3, cellSamples, 2, 2, noDataValue), 1e-7);
    }

    @Test
    public void testScapeMInterpolationAtBorder() {
        double noDataValue = -1;
        float[][] cellSamples = {{1, -1, 3}, {4, 5, 6}, {7, 8, 9}};
        assertEquals((float) 23 / 7, ScapeMGapFill.interpolateAtCornerOrBorder(3, 3, cellSamples, 0, 1, noDataValue), 1e-8);
        cellSamples = new float[][]{{1, 2, 3}, {4, 5, -1}, {7, 8, 9}};
        assertEquals((float) 39 / 7, ScapeMGapFill.interpolateAtCornerOrBorder(3, 3, cellSamples, 1, 2, noDataValue), 1e-8);
    }

    @Test
    public void testScapeMGapFill() throws IOException {
        final int pixelsPerCell = 2;
        Product productToBeFilled = createUnFilledDummyRRProduct(pixelsPerCell, 0);
        Band unfilledProductBand = productToBeFilled.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);

        final int origWidth = productToBeFilled.getSceneRasterWidth();
        final int origHeight = productToBeFilled.getSceneRasterHeight();
        float[][] origImageData = new float[origWidth][origHeight];
        for (int y = 0; y < origHeight; y++) {
            for (int x = 0; x < origWidth; x++) {
                origImageData[x][y] = unfilledProductBand.getSampleFloat(x, y);
            }
        }

        Product filledProduct = ScapeMGapFill.gapFill(productToBeFilled);

        assertNotNull(filledProduct);
        int width = filledProduct.getSceneRasterWidth();
        assertEquals(productToBeFilled.getSceneRasterWidth(), width);
        int height = filledProduct.getSceneRasterHeight();
        assertEquals(productToBeFilled.getSceneRasterHeight(), height);
        final Band filledProductBand = filledProduct.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);
        assertNotNull(filledProductBand);
        int cellHeight = filledProduct.getSceneRasterHeight() / pixelsPerCell;
        for (int y = 0; y < height; y++) {
            double groundValue = cellHeight * (y / pixelsPerCell);
            for (int x = 0; x < width; x++) {
                double value = x / pixelsPerCell + groundValue + 1;
                assertEquals(value, filledProductBand.getSampleFloat(x, y), 1e-8);
            }
        }
    }

    @Test
    public void testScapeMGapFillWithProductWithUnevenlySizedTiles() throws IOException {
        final int pixelsPerCell = 2;
        Product productToBeFilled = createUnFilledDummyRRProduct(pixelsPerCell, 1);
        Band unfilledProductBand = productToBeFilled.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);

        final int origWidth = productToBeFilled.getSceneRasterWidth();
        final int origHeight = productToBeFilled.getSceneRasterHeight();
        float[][] origImageData = new float[origWidth][origHeight];
        for (int y = 0; y < origHeight; y++) {
            for (int x = 0; x < origWidth; x++) {
                origImageData[x][y] = unfilledProductBand.getSampleFloat(x, y);
            }
        }

        Product filledProduct = ScapeMGapFill.gapFill(productToBeFilled);

        assertNotNull(filledProduct);
        int width = filledProduct.getSceneRasterWidth();
        assertEquals(origWidth, width);
        int height = filledProduct.getSceneRasterHeight();
        assertEquals(origHeight, height);
        final Band filledProductBand = filledProduct.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);
        assertNotNull(filledProductBand);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isCellToBeFilled = (origImageData[x][y] == 1000.0);
                int cellIndexX = x / pixelsPerCell;
                int cellIndexY = y / pixelsPerCell;
                if (isCellToBeFilled) {
                    if (cellIndexX == 2 && cellIndexY == 1) {    // inside
                        assertEquals(6.0, filledProductBand.getSampleFloat(x, y), 1e-6);
                    }
                    if (cellIndexX == 3 && cellIndexY == 3) {   // right edge
                        assertEquals(13.8, filledProductBand.getSampleFloat(x, y), 1e-6);
                    }
                    if (cellIndexX == 0 && cellIndexY == 4) {   // lower left
                        assertEquals(15.2, filledProductBand.getSampleFloat(x, y), 1e-6);
                    }
                    if (cellIndexX == 3 && cellIndexY == 4) {   // lower right
                        assertEquals(17.666666, filledProductBand.getSampleFloat(x, y), 1e-7);
                    }
                } else {
                    assertEquals(origImageData[x][y], filledProductBand.getSampleFloat(x, y), 1e-8);
                }
            }
        }
    }

    private Product createUnFilledDummyRRProduct(int pixelsPerCell, int offset) throws IOException {
        final int productWidth = pixelsPerCell * 3 + offset;
        final int productHeight = pixelsPerCell * 4 + offset;
        Product product = new Product("dummyRRProduct", "doesntMatter", productWidth, productHeight);
        product.setPreferredTileSize(pixelsPerCell, pixelsPerCell);
        final String bandName = ScapeMConstants.VISIBILITY_BAND_NAME;
        final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, productWidth, productHeight);
        band.setNoDataValue(1000.0);
        product.addBand(band);
        ScapeMGapFilledTestImage image = new ScapeMGapFilledTestImage(DataBuffer.TYPE_FLOAT, productWidth, productHeight, new Dimension(pixelsPerCell, pixelsPerCell),
                null, ResolutionLevel.MAXRES);
        band.setSourceImage(image);
        return product;
    }


    private class ScapeMGapFilledTestImage extends SingleBandedOpImage {
        private final int cellHeight;
        private final int tileHeight;
        private final int tileWidth;

        /**
         * Used to construct an image.
         *
         * @param dataBufferType The data type.
         * @param sourceWidth    The width of the level 0 image.
         * @param sourceHeight   The height of the level 0 image.
         * @param tileSize       The tile size for this image.
         * @param configuration  The configuration map (can be null).
         * @param level          The resolution level.
         */
        protected ScapeMGapFilledTestImage(int dataBufferType, int sourceWidth, int sourceHeight, Dimension tileSize, Map configuration, ResolutionLevel level) {
            super(dataBufferType, sourceWidth, sourceHeight, tileSize, configuration, level);
            tileHeight = tileSize.height;
            tileWidth = tileSize.width;
            cellHeight = sourceHeight / tileHeight;
        }

        @Override
        protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
            float[] elems = new float[destRect.width * destRect.height];
            int index = 0;
            for (int y = destRect.y; y < destRect.height + destRect.y; y++) {
                float groundValue = cellHeight * (y / tileHeight);
                float value = 0;
                for (int x = destRect.x; x < destRect.width + destRect.x; x++) {
                    value = x / tileWidth + groundValue + 1;
                    if (value == 6.0) {       // hole inside
                        value = 1000f;
                    }
                    if (value == 16.0) {     // hole at right edge
                        value = 1000f;
                    }
                    if (value == 17.0) {     // hole at lower left corner
                        value = 1000f;
                    }
                    if (value == 20.0) {    // hole at lower right corner
                        value = 1000f;
                    }
                    elems[index++] = value;
                }
            }
            dest.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, elems);

        }
    }


}