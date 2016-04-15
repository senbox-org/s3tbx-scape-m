package org.esa.s3tbx.scapem.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.awt.*;

/**
 * Operator for visibility to AOT conversion.
 *
 * @author Tonio Fincke, Olaf Danne
 */
@OperatorMetadata(alias = "beam.scapeM.visibility.aot", version = "1.0-SNAPSHOT",
        authors = "Tonio Fincke, Olaf Danne",
        copyright = "(c) 2013 Brockmann Consult",
        internal = true,
        description = "Operator for visibility to AOT conversion.")
public class ScapeMVis2AotOp extends ScapeMMerisBasisOp {

    @Parameter(description = "If set, use GETASSE30 DEM, otherwise get altitudes from product TPGs",
            label = "Use GETASSE30 DEM",
            defaultValue = "false")
    private boolean useDEM;

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @SourceProduct(alias = "visibility")
    private Product visibilityProduct;

    //@Parameter(description = "ScapeM AOT Lookup table")
    private ScapeMLut scapeMLut;

    @TargetProduct
    private Product targetProduct;

    private String demName = ScapeMConstants.DEFAULT_DEM_NAME;

    private ElevationModel elevationModel;

    @Override
    public void initialize() throws OperatorException {

        if (useDEM) {
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(demName);
            if (demDescriptor == null || !demDescriptor.getDemInstallDir().isFile()) {
                throw new OperatorException("DEM not installed: " + demName + ". Please install with Module Manager.");
            }
            elevationModel = demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        }


        createTargetProduct();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetRect = targetTile.getRectangle();
        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();

        Tile altitudeTile = getAltitudeTile(targetRect, sourceProduct, useDEM);

        Band visibilityBand = visibilityProduct.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);
        Tile visibilityTile = getSourceTile(visibilityBand, targetRect);


        double[][] hsurfArrayCell;
        pm.beginTask("Processing frame...", targetRect.height + 1);
        try {
            if (useDEM && altitudeTile == null) {
                hsurfArrayCell = ScapeMAlgorithm.getHsurfArrayCell(targetRect, geoCoding, elevationModel, scapeMLut);
            } else {
                hsurfArrayCell = ScapeMAlgorithm.getHsurfArrayCell(targetRect, geoCoding, altitudeTile, scapeMLut);
            }

            for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
                for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                    final double visibility = visibilityTile.getSampleDouble(x, y);
                    if (visibility != ScapeMConstants.VISIBILITY_NODATA_VALUE) {
                        final double aot550 = ScapeMAlgorithm.getCellAot550(visibility,
                                hsurfArrayCell[x - targetRect.x][y - targetRect.y],
                                scapeMLut);
                        targetTile.setSample(x, y, aot550);
                    } else {
                        targetTile.setSample(x, y, ScapeMConstants.AOT_NODATA_VALUE);
                    }
                }
                pm.worked(1);
            }
        } catch (Exception e) {
            // todo
            e.printStackTrace();
        } finally {
            pm.done();
        }
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER", "MER_L2");

        Band aot550Band = targetProduct.addBand(ScapeMConstants.AOT550_BAND_NAME, ProductData.TYPE_FLOAT32);
        aot550Band.setNoDataValue(ScapeMConstants.AOT_NODATA_VALUE);
        aot550Band.setValidPixelExpression(ScapeMConstants.SCAPEM_VALID_EXPR);
    }

    public void setScapeMLut(ScapeMLut scapeMLut) {
        this.scapeMLut = scapeMLut;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScapeMVis2AotOp.class);
        }
    }
}
