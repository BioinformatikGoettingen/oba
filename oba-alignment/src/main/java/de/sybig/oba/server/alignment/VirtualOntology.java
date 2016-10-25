/**
 * Created on October 23, 2016
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.pluginManagment.OntologyLoader;
import java.util.Properties;

public class VirtualOntology implements OntologyLoader {

    public ObaOntology loadOntology(Properties p) {
        
        return new ObaOntology();
    }

}
