package org.esa.s3tbx.scapem.operator;

import org.esa.s3tbx.scapem.util.Varsol;
import org.junit.Test;

import java.util.Calendar;

import static junit.framework.Assert.assertEquals;

public class ScapeMVisibilityTest {

    @Test
    public void testVarSol() {
        int year = 2010;
        int day = 14;

        Calendar cal = Calendar.getInstance();
        cal.set(year, Calendar.MARCH, day);
        int doy = cal.get(Calendar.DAY_OF_YEAR);

        double varSol = Varsol.getVarSol(doy);
        assertEquals(0.993734, varSol, 1.E-5);
    }

}
