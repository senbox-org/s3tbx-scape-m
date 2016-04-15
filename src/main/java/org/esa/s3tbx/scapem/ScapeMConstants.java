package org.esa.s3tbx.scapem;

/**
 * Scape-M Constants
 *
 * @author Tonio Fincke, Olaf Danne
 */
public class ScapeMConstants {

    public static final int RR_PIXELS_PER_CELL = 30;
    public static final int FR_PIXELS_PER_CELL = 120;

    public final static float[] MERIS_WAVELENGTHS = {
            412.545f, 442.401f, 489.744f, 509.7f, 559.634f,
            619.62f, 664.64f, 680.902f, 708.426f, 753.472f,
            761.606f, 778.498f, 864.833f, 884.849f, 899.86f
    };

    public static final int L1_BAND_NUM  = 15;
    public static final int BAD_VALUE  = -1;

    public final static double solIrr7 = 1424.7742;
    public final static double solIrr9 = 1225.6102;

    public final static int NUM_REF_PIXELS = 5;

    public final static double[][] RHO_VEG_ALL = {
            {0.0235, 0.0382, 0.0319, 0.0342, 0.0526, 0.0425, 0.0371, 0.0369, 0.0789, 0.3561, 0.3698, 0.3983, 0.4248, 0.4252, 0.4254},
            {0.0206, 0.04120, 0.0445, 0.0498, 0.0728, 0.0821, 0.0847, 0.0870, 0.1301, 0.1994, 0.2020, 0.2074, 0.2365, 0.2419, 0.2459},
            {0.0138, 0.0158, 0.0188, 0.021, 0.0395, 0.0279, 0.0211, 0.0206, 0.0825, 0.2579, 0.2643, 0.2775, 0.3201, 0.3261, 0.3307}
    };

    public final static double[] RHO_SUE = {
            0.0490, 0.0860, 0.1071, 0.1199, 0.1679, 0.2425, 0.2763, 0.2868, 0.3148, 0.3470, 0.3498, 0.3558, 0.3984, 0.4062, 0.4120
    };

    public static final double POWELL_FTOL = 1.E-4;

    public final static double[] WL_CENTER_INV = {
            14.2274, 11.5368, 8.50600, 1.96148, 1.78669, 1.61394, 1.50473, 4.65445, 1.41177, 3.10430,
            0.0, 1.28467, 1.15624, 1.13002, 0.0
    };

    public final static double[][] AOT_GRID = {
            {0.673345, 0.472727, 0.324623, 0.220397, 0.136966, 0.0900341, 0.0586890},
            {0.597473, 0.420376, 0.289417, 0.197476, 0.123751, 0.0822952, 0.0545618},
            {0.402420, 0.285551, 0.199061, 0.138343, 0.089596, 0.0623010, 0.0439519}
    };

    public static final double WV_INIT = 2.0;
    public static final double VIS_INIT = 23.0;

    public static final double FTOL = 1.E-4;
    public static final int MAXITER = 10000;

    public static final String DEFAULT_DEM_NAME = "GETASSE30";
    public static final double VISIBILITY_NODATA_VALUE = 0.0;

    public static final double AOT_NODATA_VALUE = 0.0;
    public static final String SCAPEM_VALID_EXPR = "!l1_flags.INVALID";

    public static final String VISIBILITY_BAND_NAME = "cell_visibility";

    public static final String AOT550_BAND_NAME = "AOT_550";
    public static final double AC_NODATA = -1.0;
    public static final String WATER_VAPOUR_BAND_NAME = "water_vapour";

    public static final double WATER_VAPOUR_NODATA_VALUE = 0.0;

    public static final int CLOUD_INVALID_BIT = 0;
    public static final int CLOUD_CERTAIN_BIT = 1;
//    public static final int CLOUD_PRESUMABLY_BIT = 2;
    public static final int CLOUD_OCEAN_BIT = 3;

}
