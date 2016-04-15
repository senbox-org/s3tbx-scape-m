package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.idepix_algo_copy.scapem.FubScapeMOp;
import org.esa.s3tbx.scapem.io.LutAccess;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
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
@OperatorMetadata(alias = "beam.scapeM", version = "1.1.2-SNAPSHOT",
                  authors = "Luis Guanter, Olaf Danne",
                  copyright = "(c) 2013 University of Valencia, Brockmann Consult",
                  description = "Operator for MERIS atmospheric correction with SCAPE-M algorithm.")
public class ScapeMOp extends ScapeMMerisBasisOp {
    public static final String VERSION = "1.1.2-SNAPSHOT";

    //    @Parameter(description = "Compute over all water (not just over lakes)",
//               label = "Compute over all water (not just over lakes)",
//               defaultValue = "true")
    private boolean computeOverWater = true;    // this is even faster, as we do not need to compute the coast/lakes mask

    //    @Parameter(description = "Use constant water vapour value of 2.0 g/cm^2 to save processing time",
//               label = "Use constant water vapour value of 2.0 g/cm^2",
//               defaultValue = "false")
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

    protected ScapeMLut scapeMLut;


    @Override
    public void initialize() throws OperatorException {
        System.out.println("using SCAPE-M VERSION = " + VERSION);

        checkProductStartStopTimes();
        readAuxdata();

        // get the cloud product from Idepix...
        Map<String, Product> idepixInput = new HashMap<String, Product>(4);
        idepixInput.put("source", sourceProduct);
        Map<String, Object> cloudParams = new HashMap<String, Object>(1);
        cloudParams.put("calculateLakes", !computeOverWater);
        Product cloudProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(FubScapeMOp.class), cloudParams, idepixInput);

        // get the cell visibility/AOT product...
        // this is a product with grid resolution, but having equal visibility values over a cell (30x30km)
        // (follows the IDL implementation)
        final ScapeMVisibilityOp scapeMVisibilityOp = new ScapeMVisibilityOp();
        scapeMVisibilityOp.setSourceProduct("source", sourceProduct);
        scapeMVisibilityOp.setSourceProduct("cloud", cloudProduct);
        scapeMVisibilityOp.setParameter("computeOverWater", computeOverWater);
        scapeMVisibilityOp.setParameter("useDEM", useDEM);
        scapeMVisibilityOp.setScapeMLut(scapeMLut);
        Product cellVisibilityProduct = scapeMVisibilityOp.getTargetProduct();

        // fill gaps...
        Product gapFilledVisibilityProduct;
        if (skipGapFilling) {
            gapFilledVisibilityProduct = cellVisibilityProduct;
        } else {
            // todo: improve gap filling performance!
            final ScapeMGapFillOp scapeMGapFillOp = new ScapeMGapFillOp();
            scapeMGapFillOp.setSourceProduct("source", sourceProduct);
            scapeMGapFillOp.setSourceProduct("gap", cellVisibilityProduct);
            gapFilledVisibilityProduct = scapeMGapFillOp.getTargetProduct();
        }

        Product smoothedVisibilityProduct;
        if (skipVisibilitySmoothing) {
            smoothedVisibilityProduct = gapFilledVisibilityProduct;
        } else {
            Map<String, Product> smoothInput = new HashMap<String, Product>(4);
            smoothInput.put("source", gapFilledVisibilityProduct);
            smoothedVisibilityProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ScapeMSmoothSimpleKernelOp.class), GPF.NO_PARAMS, smoothInput);
        }

        // convert visibility to AOT
        final ScapeMVis2AotOp scapeMVis2AotOp = new ScapeMVis2AotOp();
        scapeMVis2AotOp.setSourceProduct("source", sourceProduct);
        scapeMVis2AotOp.setSourceProduct("visibility", smoothedVisibilityProduct);
        scapeMVis2AotOp.setScapeMLut(scapeMLut);
        Product aotProduct = scapeMVis2AotOp.getTargetProduct();

        // derive CWV...
        // derive reflectance...
        final ScapeMAtmosCorrOp scapeMAtmosCorrOp = new ScapeMAtmosCorrOp();
        scapeMAtmosCorrOp.setSourceProduct("source", sourceProduct);
        scapeMAtmosCorrOp.setSourceProduct("cloud", cloudProduct);
        scapeMAtmosCorrOp.setSourceProduct("visibility", smoothedVisibilityProduct);
        scapeMAtmosCorrOp.setParameter("computeOverWater", computeOverWater);
        scapeMAtmosCorrOp.setParameter("useDEM", useDEM);
        scapeMAtmosCorrOp.setParameter("useConstantWv", useConstantWv);
        scapeMAtmosCorrOp.setParameter("outputRhoToa", outputRhoToa);
        scapeMAtmosCorrOp.setParameter("outputReflBand2", outputReflBand2);
        scapeMAtmosCorrOp.setScapeMLut(scapeMLut);
        Product atmosCorrProduct = scapeMAtmosCorrOp.getTargetProduct();

        targetProduct = atmosCorrProduct;
        ProductUtils.copyFlagBands(cloudProduct, targetProduct, true);
        ProductUtils.copyMasks(cloudProduct, targetProduct);
        ProductUtils.copyBand(ScapeMConstants.AOT550_BAND_NAME, aotProduct, atmosCorrProduct, true);
    }

    private void checkProductStartStopTimes() {
        try {
            if (sourceProduct.getStartTime() == null || sourceProduct.getEndTime() == null) {
                // we assume a regular L1b product name such as
                // MER_RR__1PNBCM20060819_073317_000000542050_00264_23364_0735.N1:
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
