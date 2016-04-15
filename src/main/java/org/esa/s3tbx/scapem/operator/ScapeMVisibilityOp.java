package org.esa.s3tbx.scapem.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.brr.HelperFunctions;
import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.s3tbx.scapem.util.ClearPixelStrategy;
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
import org.esa.s3tbx.scapem.util.ClearLandAndWaterPixelStrategy;
import org.esa.s3tbx.scapem.util.ClearLandPixelStrategy;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Calendar;

/**
 * Operator for MERIS atmospheric correction with SCAPE-M algorithm: cell visibility retrieval part.
 *
 * @author Tonio Fincke, Olaf Danne
 */
@OperatorMetadata(alias = "beam.scapeM.visibility", version = "1.0-SNAPSHOT",
        authors = "Tonio Fincke, Olaf Danne",
        copyright = "(c) 2013 Brockmann Consult",
        internal = true,
        description = "Operator for MERIS atmospheric correction with SCAPE-M algorithm: cell visibility retrieval part.")
public class ScapeMVisibilityOp extends ScapeMMerisBasisOp {

    public static final String RADIANCE_BAND_PREFIX = "radiance";
    //@Parameter(description = "ScapeM AOT Lookup table")
    private ScapeMLut scapeMLut;
    @Parameter(description = "Compute over all water (not just over lakes)",
            label = "Compute over all water (not just over lakes)",
            defaultValue = "false")
    private boolean computeOverWater;
    @Parameter(description = "If set, use GETASSE30 DEM, otherwise get altitudes from product TPGs",
            label = "Use GETASSE30 DEM",
            defaultValue = "false")
    private boolean useDEM;
    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @SourceProduct(alias = "cloud")
    private Product cloudProduct;
    @TargetProduct
    private Product targetProduct;
    private ElevationModel elevationModel;

    @Override
    public void initialize() throws OperatorException {
        if (useDEM) {
            String demName = ScapeMConstants.DEFAULT_DEM_NAME;
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

        final Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME), targetRect);
        final Tile vzaTile = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME), targetRect);
        final Tile saaTile = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME), targetRect);
        final Tile vaaTile = getSourceTile(sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME), targetRect);

        Tile altitudeTile = getAltitudeTile(targetRect, sourceProduct, useDEM);

        ClearPixelStrategy clearPixelStrategy;
        if (computeOverWater) {
            clearPixelStrategy = new ClearLandAndWaterPixelStrategy();
        } else {
            clearPixelStrategy = new ClearLandPixelStrategy();
        }
        clearPixelStrategy.setTile(getSourceTile(cloudProduct.getBandAt(0), targetRect));

        Tile[] radianceTiles = new Tile[ScapeMConstants.L1_BAND_NUM];
        Band[] radianceBands = new Band[ScapeMConstants.L1_BAND_NUM];
        for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
            radianceBands[bandId] = sourceProduct.getBand(RADIANCE_BAND_PREFIX + "_" + (bandId + 1));
            radianceTiles[bandId] = getSourceTile(radianceBands[bandId], targetRect);
        }

        double[] toaMinCell = new double[ScapeMConstants.L1_BAND_NUM];

        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();

        final boolean cellIsClear35Percent =
                ScapeMAlgorithm.isCellClearLand(targetRect, clearPixelStrategy, 0.35);

        if (cellIsClear35Percent) {
            // compute visibility...

            final int centerX = targetRect.x + targetRect.width / 2;
            final int centerY = targetRect.y + targetRect.height / 2;

            final double vza = vzaTile.getSampleDouble(centerX, centerY);
            final double sza = szaTile.getSampleDouble(centerX, centerY);
            final double vaa = vaaTile.getSampleDouble(centerX, centerY);
            final double saa = saaTile.getSampleDouble(centerX, centerY);
            //todo 3 mba/** This is the only usage of brr module, inline method. 14.04.2016
            final double phi = HelperFunctions.computeAzimuthDifference(vaa, saa);

            try {
                double[][] hsurfArrayCell;
                if (useDEM && altitudeTile == null) {
                    hsurfArrayCell = ScapeMAlgorithm.getHsurfArrayCell(targetRect, geoCoding, elevationModel, scapeMLut);
                } else {
                    hsurfArrayCell = ScapeMAlgorithm.getHsurfArrayCell(targetRect, geoCoding, altitudeTile, scapeMLut);
                }

                final double hsurfMeanCell = ScapeMAlgorithm.getHsurfMeanCell(hsurfArrayCell, targetRect, clearPixelStrategy);

                final double[][] cosSzaArrayCell = ScapeMAlgorithm.getCosSzaArrayCell(targetRect, szaTile);
                final double cosSzaMeanCell = ScapeMAlgorithm.getCosSzaMeanCell(cosSzaArrayCell, targetRect, clearPixelStrategy);


                final int doy = sourceProduct.getStartTime().getAsCalendar().get(Calendar.DAY_OF_YEAR);
                double[][][] toaArrayCell = new double[ScapeMConstants.L1_BAND_NUM][targetRect.width][targetRect.height];
                for (int bandId = 0; bandId < ScapeMConstants.L1_BAND_NUM; bandId++) {
                    toaArrayCell[bandId] = ScapeMAlgorithm.getToaArrayCell(radianceTiles[bandId], targetRect, doy);
                    toaMinCell[bandId] = ScapeMAlgorithm.getToaMinCell(toaArrayCell[bandId]);
                }

                // now get visibility estimate...
                final boolean cellIsClear45Percent =
                        ScapeMAlgorithm.isCellClearLand(targetRect, clearPixelStrategy, 0.45);

                final double visibility = ScapeMAlgorithm.getCellVisibility(toaArrayCell,
                        toaMinCell, vza, sza, phi,
                        hsurfArrayCell,
                        hsurfMeanCell,
                        cosSzaArrayCell,
                        cosSzaMeanCell,
                        cellIsClear45Percent,
                        scapeMLut);

                setCellVisibilitySamples(targetTile, targetRect, visibility);
            } catch (Exception e) {
                // todo
                e.printStackTrace();
                setCellVisibilitySamples(targetTile, targetRect, ScapeMConstants.AOT_NODATA_VALUE);
            }
        } else {
            setCellVisibilitySamples(targetTile, targetRect, ScapeMConstants.AOT_NODATA_VALUE);
        }
    }

    private void setCellVisibilitySamples(Tile targetTile, Rectangle targetRect, double visibility) {
        for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
            for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                targetTile.setSample(x, y, visibility);
            }
        }
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "MER", "MER_L2");

        if (sourceProduct.getBand("dem_elevation") != null) {
            ProductUtils.copyBand("dem_elevation", sourceProduct, targetProduct, true);
        }
        Band visibilityBand = targetProduct.addBand(ScapeMConstants.VISIBILITY_BAND_NAME, ProductData.TYPE_FLOAT32);
        visibilityBand.setNoDataValue(ScapeMConstants.VISIBILITY_NODATA_VALUE);
        visibilityBand.setValidPixelExpression(ScapeMConstants.SCAPEM_VALID_EXPR);
    }

    public void setScapeMLut(ScapeMLut scapeMLut) {
        this.scapeMLut = scapeMLut;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScapeMVisibilityOp.class);
        }
    }
}
