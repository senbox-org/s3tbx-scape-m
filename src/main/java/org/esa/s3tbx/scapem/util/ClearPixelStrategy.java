package org.esa.s3tbx.scapem.util;


import org.esa.snap.core.gpf.Tile;

/**
 * Strategy to determine how clear pixels shall be defined in given tile
 * (i.e., over land only or both over land and water)
 *
 * @author Tonio Fincke, Olaf Danne
 */
public interface ClearPixelStrategy {

    /**
     * determines whether pixel is a valid clear pixel
     *
     * @param x - the x coord
     * @param y - the y coord
     * @return  boolean
     */
    boolean isValid(int x, int y);

    /**
     * sets the underlying tile
     *
     * @param tile - the tile
     */
    void setTile(Tile tile);

}
