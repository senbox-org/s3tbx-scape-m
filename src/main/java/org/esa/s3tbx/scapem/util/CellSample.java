package org.esa.s3tbx.scapem.util;

/**
 * Object holding a value in a Scape-M 'cell' together with its indices
 *
 * @author olafd
 */
public class CellSample {
    private int cellXIndex;
    private int cellYIndex;
    private Double value;

    public CellSample(int cellXIndex, int cellYIndex, Double value) {
        this.cellXIndex = cellXIndex;
        this.cellYIndex = cellYIndex;
        this.value = value;
    }

    public int getCellXIndex() {
        return cellXIndex;
    }

    public int getCellYIndex() {
        return cellYIndex;
    }

    public Double getValue() {
        return value;
    }

    void setValue(Double value) {
        this.value = value;
    }
}
