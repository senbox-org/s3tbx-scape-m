package org.esa.s3tbx.scapem.io;

import junit.framework.TestCase;
import org.esa.snap.core.util.math.LookupTable;

import java.io.IOException;


public class AccessLutTest extends TestCase {

    public void testAtmParamLut() throws IOException {
        LookupTable lut = LutAccess.getAtmParmsLookupTable();
        assertNotNull(lut);

        assertEquals(8, lut.getDimensionCount());

        final double[] wvlArr = lut.getDimension(7).getSequence();
        final int nWvl = wvlArr.length;
        assertEquals(15, nWvl);
        assertEquals(412.545f, wvlArr[0], 1.E-3);
        assertEquals(442.401f, wvlArr[1], 1.E-3);
        assertEquals(899.86f, wvlArr[14], 1.E-3);

        final double[] paramArr = lut.getDimension(6).getSequence();
        final int nParameters = paramArr.length;
        assertEquals(7, nParameters);     //  Parameters
        assertEquals(1.0, paramArr[0], 1.E-4);
        assertEquals(2.0, paramArr[1], 1.E-4);
        assertEquals(3.0, paramArr[2], 1.E-4);
        assertEquals(5.0, paramArr[4], 1.E-4);
        assertEquals(6.0, paramArr[5], 1.E-4);
        assertEquals(7.0, paramArr[6], 1.E-4);

        final double[] cwvArr = lut.getDimension(5).getSequence();
        final int nCwv = cwvArr.length;
        assertEquals(6, nCwv);     //  CWV
        assertEquals(0.3, cwvArr[0], 1.E-4);
        assertEquals(1.0, cwvArr[1], 1.E-4);
        assertEquals(1.5, cwvArr[2], 1.E-4);
        assertEquals(2.0, cwvArr[3], 1.E-4);
        assertEquals(2.7, cwvArr[4], 1.E-4);
        assertEquals(5.0, cwvArr[5], 1.E-4);

        final double[] visArr = lut.getDimension(4).getSequence();
        final int nVis = visArr.length;
        assertEquals(7, nVis);
        assertEquals(10.0, visArr[0], 1.E-3);
        assertEquals(15.0, visArr[1], 1.E-3);
        assertEquals(23.0, visArr[2], 1.E-3);
        assertEquals(35.0, visArr[3], 1.E-3);
        assertEquals(60.0, visArr[4], 1.E-3);
        assertEquals(100.0, visArr[5], 1.E-3);
        assertEquals(180.0, visArr[6], 1.E-3);

        final double[] hsfArr = lut.getDimension(3).getSequence();
        final int nHsf = hsfArr.length;
        assertEquals(3, nHsf);     //  HSF
        assertEquals(0.0, hsfArr[0], 1.E-3);
        assertEquals(0.7, hsfArr[1], 1.E-3);
        assertEquals(2.5, hsfArr[2], 1.E-3);

        final double[] raaArr = lut.getDimension(2).getSequence();
        final int nRaa = raaArr.length;
        assertEquals(7, nRaa);     //  RAA
        assertEquals(0.0, raaArr[0], 1.E-4);
        assertEquals(25.0, raaArr[1], 1.E-4);
        assertEquals(50.0, raaArr[2], 1.E-4);
        assertEquals(85.0, raaArr[3], 1.E-4);
        assertEquals(120.0, raaArr[4], 1.E-4);
        assertEquals(155.0, raaArr[5], 1.E-4);
        assertEquals(180.0, raaArr[6], 1.E-4);

        final double[] szaArr = lut.getDimension(1).getSequence();
        final int nSza = szaArr.length;
        assertEquals(6, nSza);     //  SZA
        assertEquals(0.0, szaArr[0], 1.E-4);
        assertEquals(10.0, szaArr[1], 1.E-4);
        assertEquals(20.0, szaArr[2], 1.E-4);
        assertEquals(35.0, szaArr[3], 1.E-4);
        assertEquals(50.0, szaArr[4], 1.E-4);
        assertEquals(65.0, szaArr[5], 1.E-4);

        final double[] vzaArr = lut.getDimension(0).getSequence();
        final int nVza = vzaArr.length;
        assertEquals(6, nVza);     //  VZA
        assertEquals(0.0, vzaArr[0], 1.E-4);
        assertEquals(9.0, vzaArr[1], 1.E-4);
        assertEquals(18.0, vzaArr[2], 1.E-4);
        assertEquals(27.0, vzaArr[3], 1.E-4);
        assertEquals(36.0, vzaArr[4], 1.E-4);
        assertEquals(45.0, vzaArr[5], 1.E-4);

        // check some LUT values (taken from ncdump of SCAPEM_LUT_MERIS):

        // first values in LUT
        double[] coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[0], wvlArr[0]};
        double value = lut.getValue(coord);
        assertEquals(0.009594766, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[0], wvlArr[1]};
        value = lut.getValue(coord);
        assertEquals(0.008080291, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[0], wvlArr[2]};
        value = lut.getValue(coord);
        assertEquals(0.006640377, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[0], wvlArr[14]};
        value = lut.getValue(coord);
        assertEquals(0.0007679362, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[1], wvlArr[0]};
        value = lut.getValue(coord);
        assertEquals(0.03709091, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[1], wvlArr[1]};
        value = lut.getValue(coord);
        assertEquals(0.04535859, value, 1.E-4);

        coord = new double[]{vzaArr[0], szaArr[0], raaArr[0], hsfArr[0], visArr[0], cwvArr[0], paramArr[1], wvlArr[14]};
        value = lut.getValue(coord);
        assertEquals(0.05023599, value, 1.E-4);


        // values somewhere inside LUT:
        coord = new double[]{vzaArr[3], szaArr[4], raaArr[6], hsfArr[0], visArr[3], cwvArr[1], paramArr[2], wvlArr[3]};
        value = lut.getValue(coord);
        assertEquals(0.0308434, value, 1.E-4);

        coord = new double[]{vzaArr[1], szaArr[2], raaArr[4], hsfArr[2], visArr[4], cwvArr[5], paramArr[3], wvlArr[12]};
        value = lut.getValue(coord);
        assertEquals(0.0520935, value, 1.E-4);

        coord = new double[]{vzaArr[2], szaArr[5], raaArr[2], hsfArr[1], visArr[2], cwvArr[3], paramArr[4], wvlArr[8]};
        value = lut.getValue(coord);
        assertEquals(0.0849858, value, 1.E-4);


        // last values in LUT:
        coord = new double[]{vzaArr[5], szaArr[5], raaArr[6], hsfArr[2], visArr[6], cwvArr[5], paramArr[6], wvlArr[12]};
        value = lut.getValue(coord);
        assertEquals(0.02610051, value, 1.E-4);

        coord = new double[]{vzaArr[5], szaArr[5], raaArr[6], hsfArr[2], visArr[6], cwvArr[5], paramArr[6], wvlArr[13]};
        value = lut.getValue(coord);
        assertEquals(0.02497919, value, 1.E-4);

        coord = new double[]{vzaArr[5], szaArr[5], raaArr[6], hsfArr[2], visArr[6], cwvArr[5], paramArr[6], wvlArr[14]};
        value = lut.getValue(coord);
        assertEquals(0.02453506, value, 1.E-4);
    }

    public void testInterpolAtmParamLut() throws IOException {
        LookupTable lut = LutAccess.getAtmParmsLookupTable();
        assertNotNull(lut);

        double vza = 7.13599;
        double sza = 64.9990;
        double raa = 126.915;
        double hsf = 0.0215365;
        double vis = 10.0010;
        double cwv = 2.0;

        double[][] fInt = LutAccess.interpolAtmParamLut(lut, vza, sza, raa, hsf, vis, cwv);

        assertNotNull(fInt);
        assertEquals(15, fInt.length);
        assertEquals(7, fInt[0].length);

        // This is the IDL result what we expect here:
//        0.00581705   0.00713673    0.0242453      1.47368     0.298831     0.294287      1.19937
//        0.00512911    0.0105814    0.0265804      1.26005     0.267197     0.338121      1.12165
//        0.00446229    0.0172248    0.0301663      1.02738     0.231985     0.395629      1.01983
//        0.00409487    0.0194501    0.0301222     0.953107     0.219702     0.413540     0.975261
//        0.00310840    0.0232863    0.0276479     0.807294     0.193625     0.453004     0.887526
//        0.00232018    0.0279269    0.0247784     0.663187     0.169899     0.505088     0.786468
//        0.00202069    0.0325176    0.0240940     0.583628     0.155145     0.548419     0.720008
//        0.00190086    0.0336598    0.0235258     0.559865     0.150622     0.562908     0.696797
//        0.00160872    0.0325127    0.0203575     0.511673     0.139783     0.572461     0.662775
//        0.00134531    0.0360593    0.0191722     0.455402     0.130836     0.616591     0.607274
//        0.000329935   0.00682129   0.00178745     0.199349    0.0583220     0.268767     0.598516
//        0.00118702    0.0361172    0.0176225     0.425993     0.124744     0.631201     0.577571
//        0.000773434    0.0345798    0.0130204     0.346268     0.107084     0.677151     0.501291
//        0.000708372    0.0341382    0.0121623     0.330487     0.103152     0.683280     0.481810
//        0.000451036    0.0201203   0.00526832     0.262719    0.0695990     0.540305     0.474061

        // check a few of them:
        assertEquals(0.00581705, fInt[0][0], 1.E-4);
        assertEquals(0.00713673, fInt[0][1], 1.E-4);
        assertEquals(0.0242453, fInt[0][2], 1.E-4);
        assertEquals(1.19937, fInt[0][6], 1.E-4);
        assertEquals(0.00512911, fInt[1][0], 1.E-4);
        assertEquals(1.12165, fInt[1][6], 1.E-4);
        assertEquals(0.000451036, fInt[14][0], 1.E-4);
        assertEquals(0.540305, fInt[14][5], 1.E-4);
        assertEquals(0.474061, fInt[14][6], 1.E-4);
    }
}
