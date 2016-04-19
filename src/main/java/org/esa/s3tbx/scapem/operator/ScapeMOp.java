/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.algo.FubScapeMClassificationOp;
import org.esa.s3tbx.scapem.io.LutAccess;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for MERIS atmospheric correction with SCAPE-M algorithm.
 *
 * @author Tonio Fincke, Olaf Danne
 */
@OperatorMetadata(alias = "snap.scapeM", version = "1.1.2-SNAPSHOT",
                  authors = "Luis Guanter, Olaf Danne",
                  copyright = "(c) 2013 University of Valencia, Brockmann Consult",
                  description = "Operator for MERIS atmospheric correction with SCAPE-M algorithm.")
public class ScapeMOp extends ScapeMMerisBasisOp {
    public static final String VERSION = "1.1.2-SNAPSHOT";
    public static final String SOURCE_PRODUCT = "source";

    private boolean computeOverWater = true;    // this is even faster, as we do not need to compute the coast/lakes mask


    private boolean useConstantWv = false;  // performance gain is negligible, so do not use as option for the moment

    @Parameter(description = "If set, use GETASSE30 DEM, otherwise get altitudes from product TPGs",
               label = "Use GETASSE30 DEM",
               defaultValue = "false")
    private boolean useDEM;

    @Parameter(description = "If set, gap filling will not be applied (may be unnecessary in certain cases)",
               label = "Skip gap filling",
               defaultValue = "false")
    private boolean skipGapFilling;

    @Parameter(description = "If set, visibility smoothing will not be applied",
               label = "Skip visibility smoothing",
               defaultValue = "false")
    private boolean skipVisibilitySmoothing;

    @Parameter(description = "If set, TOA reflectances are written to output product",
               label = "Write rhoTOA",
               defaultValue = "false")
    private boolean outputRhoToa;

    @Parameter(description = "If set, AC corrected reflectance band 2 (443nm) is written to output product",
               label = "Write 443nm reflectance band",
               defaultValue = "false")
    private boolean outputReflBand2;

    @SourceProduct(alias = "MERIS_L1b", description = "MERIS L1B product")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;


    @Parameter(description = "Reflectance Threshold for reflectance 12", defaultValue = "0.08")
    private float reflectance_water_threshold;

    @Parameter(description = "The thickness of the coastline in kilometers.", defaultValue = "20")
    private float thicknessOfCoast;

    @Parameter(description = "The minimal size for a water region to be acknowledged as an ocean in km².",
            defaultValue = "1600")
    private float minimumOceanSize;

    @Parameter(description = "Whether or not to calculate a lake mask", defaultValue = "true")
    private boolean calculateLakes;


    protected ScapeMLut scapeMLut;


    @Override
    public void initialize() throws OperatorException {
        System.out.println("using SCAPE-M VERSION = " + VERSION);
        checkProductStartStopTimes();
        readAuxdata();
        final Product cloudProduct = getCloudProduct();
        // get the cell visibility/AOT product...
        // this is a product with grid resolution, but having equal visibility values over a cell (30x30km)
        // (follows the IDL implementation)
        final Product cellVisibilityProduct = getCellVisibilityProduct(cloudProduct);

        // fill gaps...
        final Product gapFilledVisibilityProduct;
        if (skipGapFilling) {
            gapFilledVisibilityProduct = cellVisibilityProduct;
        } else {
            // todo: improve gap filling performance!
            final ScapeMGapFillOp scapeMGapFillOp = new ScapeMGapFillOp();
            scapeMGapFillOp.setSourceProduct(SOURCE_PRODUCT, sourceProduct);
            scapeMGapFillOp.setSourceProduct("gap", cellVisibilityProduct);
            gapFilledVisibilityProduct = scapeMGapFillOp.getTargetProduct();
        }

        final Product smoothedVisibilityProduct;
        if (skipVisibilitySmoothing) {
            smoothedVisibilityProduct = gapFilledVisibilityProduct;
        } else {
            Map<String, Product> smoothInput = new HashMap<>(4);
            smoothInput.put(SOURCE_PRODUCT, gapFilledVisibilityProduct);
            smoothedVisibilityProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ScapeMSmoothSimpleKernelOp.class), GPF.NO_PARAMS, smoothInput);
        }

        // convert visibility to AOT
        final ScapeMVis2AotOp scapeMVis2AotOp = new ScapeMVis2AotOp();
        scapeMVis2AotOp.setSourceProduct(SOURCE_PRODUCT, sourceProduct);
        scapeMVis2AotOp.setSourceProduct("visibility", smoothedVisibilityProduct);
        scapeMVis2AotOp.setScapeMLut(scapeMLut);
        Product aotProduct = scapeMVis2AotOp.getTargetProduct();

        // derive CWV...
        // derive reflectance...
        Product atmosCorrProduct = getAtmosphaseCorrectionProduct(cloudProduct, smoothedVisibilityProduct);
        targetProduct = atmosCorrProduct;
        ProductUtils.copyFlagBands(cloudProduct, targetProduct, true);
        ProductUtils.copyMasks(cloudProduct, targetProduct);
        ProductUtils.copyBand(ScapeMConstants.AOT550_BAND_NAME, aotProduct, atmosCorrProduct, true);
    }

    private Product getAtmosphaseCorrectionProduct(Product cloudProduct, Product smoothedVisibilityProduct) {
        final ScapeMAtmosCorrOp scapeMAtmosCorrOp = new ScapeMAtmosCorrOp();
        scapeMAtmosCorrOp.setSourceProduct(SOURCE_PRODUCT, sourceProduct);
        scapeMAtmosCorrOp.setSourceProduct("cloud", cloudProduct);
        scapeMAtmosCorrOp.setSourceProduct("visibility", smoothedVisibilityProduct);
        scapeMAtmosCorrOp.setParameter("computeOverWater", computeOverWater);
        scapeMAtmosCorrOp.setParameter("useDEM", useDEM);
        scapeMAtmosCorrOp.setParameter("useConstantWv", useConstantWv);
        scapeMAtmosCorrOp.setParameter("outputRhoToa", outputRhoToa);
        scapeMAtmosCorrOp.setParameter("outputReflBand2", outputReflBand2);
        scapeMAtmosCorrOp.setScapeMLut(scapeMLut);
        return scapeMAtmosCorrOp.getTargetProduct();
    }

    private Product getCellVisibilityProduct(Product cloudProduct) {
        final ScapeMVisibilityOp scapeMVisibilityOp = new ScapeMVisibilityOp();
        scapeMVisibilityOp.setSourceProduct(SOURCE_PRODUCT, sourceProduct);
        scapeMVisibilityOp.setSourceProduct("cloud", cloudProduct);
        scapeMVisibilityOp.setParameter("computeOverWater", computeOverWater);
        scapeMVisibilityOp.setParameter("useDEM", useDEM);
        scapeMVisibilityOp.setScapeMLut(scapeMLut);
        return scapeMVisibilityOp.getTargetProduct();
    }

    private Product getCloudProduct() {
        Operator operator = new FubScapeMClassificationOp();

        operator.setSourceProduct(sourceProduct);
        operator.setParameter("reflectance_water_threshold", reflectance_water_threshold);
        operator.setParameter("thicknessOfCoast", thicknessOfCoast);
        operator.setParameter("minimumOceanSize", minimumOceanSize);
        operator.setParameter("calculateLakes", calculateLakes);
        return operator.getTargetProduct();
    }

    private void checkProductStartStopTimes() {
        try {
            if (sourceProduct.getStartTime() == null || sourceProduct.getEndTime() == null) {
                final String ymd = sourceProduct.getName().substring(14, 22);
                final String hms = sourceProduct.getName().substring(23, 29);
                sourceProduct.setStartTime(ProductData.UTC.parse(ymd + " " + hms, "yyyyMMdd HHmmss"));
                sourceProduct.setEndTime(ProductData.UTC.parse(ymd + " " + hms, "yyyyMMdd HHmmss"));
            }
        } catch (Exception e) {
            throw new OperatorException("could not add missing product start/end times: ", e);
        }
    }

    private void readAuxdata() {
        try {
            scapeMLut = new ScapeMLut(LutAccess.getAtmParmsLookupTable());
        } catch (IOException e) {
            throw new OperatorException("Cannot read atmospheric LUT: ", e);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScapeMOp.class);
        }
    }
}
