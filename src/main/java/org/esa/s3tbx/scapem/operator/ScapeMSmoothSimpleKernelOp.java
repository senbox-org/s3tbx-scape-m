package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;


import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConvolveDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;

/**
 * Operator for smoothing 30km-cell visibility onto target grid, using a JAI convolution with simple constant kernel.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "snap.scapeM.smooth.simple", version = "1.0-SNAPSHOT",
                  authors = "Olaf Danne",
                  copyright = "(c) 2014 Brockmann Consult",
                  internal = true,
                  description = "Operator for smoothing 30km-cell visibility onto target grid, using a JAI convolution " +
                          "with simple constant kernel. This replaces the weird stuff which was build following the breadboard.")
public class ScapeMSmoothSimpleKernelOp extends ScapeMMerisBasisOp {
    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private int pixelsPerCell;

    @Override
    public void initialize() throws OperatorException {

        if (sourceProduct.getProductType().contains("_RR")) {
            pixelsPerCell = ScapeMConstants.RR_PIXELS_PER_CELL;
        } else {
            pixelsPerCell = ScapeMConstants.FR_PIXELS_PER_CELL;
        }

        createTargetProduct();
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        targetProduct.setPreferredTileSize(pixelsPerCell, pixelsPerCell);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        Band b = sourceProduct.getBand(ScapeMConstants.VISIBILITY_BAND_NAME);
        RenderedImage sourceImage = b.getSourceImage();

        final int kernelSize = pixelsPerCell;
        float[] kernelMatrix = new float[kernelSize * kernelSize];
        for (int k = 0; k < kernelMatrix.length; k++) {
            kernelMatrix[k] = 1.0f / (kernelSize * kernelSize);
        }
        KernelJAI kernel = new KernelJAI(kernelSize, kernelSize, kernelMatrix);
        final BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderingHints testHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender);
        RenderedOp targetImage = ConvolveDescriptor.create(sourceImage, kernel, testHints);

        Band targetBand = ProductUtils.copyBand(ScapeMConstants.VISIBILITY_BAND_NAME, sourceProduct, targetProduct, false);
        targetBand.setSourceImage(targetImage);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScapeMSmoothSimpleKernelOp.class);
        }
    }
}
