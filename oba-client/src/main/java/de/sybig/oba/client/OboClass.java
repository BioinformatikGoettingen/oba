/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

import de.sybig.oba.server.JsonAnnotation;
import de.sybig.oba.server.JsonCls;
import java.util.Set;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class OboClass extends OntologyClass
implements  Comparable<OboClass> {

    public OboClass() {
        super();
    }

    public OboClass(JsonCls c) {
        super(c);
    }

    public String getLabel() {
        if (getLabels() != null && getLabels().size() > 0) {
            return ((JsonAnnotation) super.getLabels().iterator().next()).getValue(); 
        }
        return null;
    }

    public String getDefinition() {
        Set<JsonAnnotation> defs = getAnnotationValues("def");
        if (defs != null && defs.size() > 0) {
            return defs.iterator().next().getValue();
        }
        return null;
    }

    public String getSubsets() {
        Set<JsonAnnotation> subsets = getAnnotationValues("subset");
        if (subsets != null && subsets.size() > 0) {
            return subsets.iterator().next().getValue();
        }
        return null;
    }

    public boolean isObsolete(){
        Set<JsonAnnotation> defs = getAnnotationValues("is_obsolete");
        if (defs != null &&  defs.size() > 0){
            if (defs.iterator().next().getValue().equals("true")){
                return true;
            }
        }        
        return false;
    }
    @Override
	protected OntologyClass createNewOntologyClass(JsonCls c) {
		return new OboClass(c);
	}

    public int compareTo(OboClass t) {
        if (this.getLabel() == null){
            return 1;
        }
        if (t.getLabel() == null){
            return -1;
        }
        return this.getLabel().compareTo(t.getLabel());
    }
}
