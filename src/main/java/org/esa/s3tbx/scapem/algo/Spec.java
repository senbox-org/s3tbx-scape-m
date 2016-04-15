package org.esa.s3tbx.scapem.algo;


import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.gpf.OperatorException;

public class Spec {
    public Spec() {
        BasisOp basisOp = new BasisOp() {
            @Override
            public void initialize() throws OperatorException {
                renameL1bMaskNames(getTargetProduct());
            }
        };

    }
}
