package org.esa.s3tbx.scapem.util;

import org.esa.s3tbx.scapem.ScapeMConstants;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.BitSetter;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 09.12.13
 * Time: 17:19
 *
 * @author olafd
 */
public class ClearLandAndWaterPixelStrategy implements ClearPixelStrategy {

    private Tile tile;

    @Override
    public boolean isValid(int x, int y) {
        int sampleInt = tile.getSampleInt(x, y);
        boolean isInvalid = BitSetter.isFlagSet(sampleInt, ScapeMConstants.CLOUD_INVALID_BIT);
        boolean isCloud = BitSetter.isFlagSet(sampleInt, ScapeMConstants.CLOUD_CERTAIN_BIT);
        return !isCloud && !isInvalid;
    }

    @Override
    public void setTile(Tile tile) {
        this.tile = tile;
    }

}
