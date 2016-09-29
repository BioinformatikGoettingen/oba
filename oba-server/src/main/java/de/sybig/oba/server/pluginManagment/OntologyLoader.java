package de.sybig.oba.server.pluginManagment;

import de.sybig.oba.server.ObaOntology;
import java.util.Properties;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public interface OntologyLoader {

     ObaOntology loadOntology(Properties p);

}
