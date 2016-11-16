package de.sybig.oba.server.alignment;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public enum Methods {

    LABEL_EQUAL(0),
    NEXT(1);

    private int pos;

    Methods(int pos) {
        this.pos = pos;
    }

    public int getPosition(){
        return pos;
    }
}
