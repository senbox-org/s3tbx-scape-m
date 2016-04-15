package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * A copy of MerisBasisOp, but suppressing the 'copyAllTiePoints' option
 *
 * @author olafd
 */
public abstract class ScapeMMerisBasisOp extends Operator {

    /**
     * creates a new product with the same size
     *
     * @param sourceProduct - the source product
     * @param name - product name
     * @param type - product type
     * @return targetProduct
     */
    public Product createCompatibleProduct(Product sourceProduct, String name, String type) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        copyProductTrunk(sourceProduct, targetProduct);

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        if (sourceProduct.getProductType().contains("_RR")) {
            targetProduct.setPreferredTileSize(ScapeMConstants.RR_PIXELS_PER_CELL, ScapeMConstants.RR_PIXELS_PER_CELL);
        } else {
            targetProduct.setPreferredTileSize(ScapeMConstants.FR_PIXELS_PER_CELL, ScapeMConstants.FR_PIXELS_PER_CELL);
        }

        return targetProduct;
    }

    /**
     * Copies basic information for a MERIS product to the target product
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    public void copyProductTrunk(Product sourceProduct,
                                 Product targetProduct) {
        copyTiePoints(sourceProduct, targetProduct);
        copyBaseGeoInfo(sourceProduct, targetProduct);
    }

    /**
     * Provides the altitude tile from band or TPG
     *
     * @param targetRect - the target rectangle
     * @param sourceProduct - the source product
     * @param useDEM - if set, altitude is taken from 'dem_elevation' band
     * @return the altitude tile (may be null)
     */
    Tile getAltitudeTile(Rectangle targetRect, Product sourceProduct, boolean useDEM) {
        Tile altitudeTile = null;
        Band demBand;
        if (useDEM) {
            demBand = sourceProduct.getBand("dem_elevation");
            if (demBand != null) {
                altitudeTile = getSourceTile(demBand, targetRect);
            }
        } else {
            Band frAltitudeBand = sourceProduct.getBand("altitude");
            if (frAltitudeBand != null) {
                // FR, FSG
                altitudeTile = getSourceTile(frAltitudeBand, targetRect);
            } else {
                // RR
                TiePointGrid rrAltitudeTpg = sourceProduct.getTiePointGrid("dem_alt");
                if (rrAltitudeTpg != null) {
                    altitudeTile = getSourceTile(rrAltitudeTpg, targetRect);
                } else {
                    throw new OperatorException
                            ("Cannot attach altitude information from given input and configuration - please check!");
                }
            }
        }
        return altitudeTile;
    }

    /**
     * Copies the tie point data.
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    private void copyTiePoints(Product sourceProduct,
                               Product targetProduct) {
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
    }

    /**
     * Copies geocoding and the start and stop time.
     *
     * @param sourceProduct - the source product
     * @param targetProduct - the target product
     */
    private void copyBaseGeoInfo(Product sourceProduct,
                                 Product targetProduct) {
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }

}
