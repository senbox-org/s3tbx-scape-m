package org.esa.s3tbx.scapem.util;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.OperatorException;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MeanDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * SCAPE-M utility class
 *
 * @author olafd
 */
public class ScapeMUtils {

    /**
     * Provides the sum of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the sum
     */
    public static double getSumDouble1D(Double[] src) {
        double sum = 0.0;
        for (Double d : src) {
            sum += d;
        }
        return sum;
    }

    /**
     * Provides the mean of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the mean
     */
    public static double getMeanDouble1D(double[] src) {
        return getSumDouble1D(src) / src.length;
    }

    /**
     * Provides the mean of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the mean
     */
    public static double getMeanDouble1D(Double[] src) {
        return getSumDouble1D(src) / src.length;
    }

    /**
     * Provides the minimum of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the minimum
     */
    public static double getMinimumDouble1D(double[] src) {
        double min = Double.MAX_VALUE;
        for (double d : src) {
            if (d < min) {
                min = d;
            }
        }
        return min;
    }

    /**
     * Provides the index of the minimum of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the array index
     */
    public static int getMinimumIndexDouble1D(double[] src) {
        double min = Double.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < src.length; i++) {
            if (src[i] < min) {
                min = src[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * Provides the standard deviation of all doubles of an 1D array
     *
     * @param src - the source array
     * @return the standard deviation
     */
    public static double getStdevDouble1D(double[] src) {
        double diffSqr = 0.0;
        double mean = getMeanDouble1D(src);
        for (double d : src) {
            diffSqr += Math.pow(d - mean, 2.0);
        }
        return Math.sqrt(diffSqr / (src.length - 1));
    }

    // todo: check if still needed
    public static double getImageMeanValue(RenderedImage image) {
        // retrieve mean of source image of given band
        final RenderedOp meanOp = MeanDescriptor.create(image, null, 1, 1, null);
        final double[] mean = (double[]) meanOp.getProperty("mean");
        return mean[0];
    }

    public static RenderedOp getImagesDifference(RenderedImage image1, RenderedImage image2) {
        // retrieve pixelwise differences of two images
        return SubtractDescriptor.create(image1, image2, null);
    }

    public static RenderedOp getImagesAbsolute(RenderedImage image1) {
        // retrieve new image with absolute of pixels of source image

        // Create a ParameterBlock with the source image.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image1);
        return JAI.create("absolute", pb);
    }

    private static double getSumDouble1D(double[] src) {
        double sum = 0.0;
        for (double d : src) {
            sum += d;
        }
        return sum;
    }

    public static ElevationModel getElevationModel(boolean useDEM) {
        if (useDEM) {
            String demName = ScapeMConstants.DEFAULT_DEM_NAME;
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(demName);
            if (demDescriptor == null || !demDescriptor.getDemInstallDir().isFile()) {
                throw new OperatorException("DEM not installed: " + demName + ". Please install with Module Manager.");
            }
            return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        }
        return null;
    }

}