/**
 * Created on October 23, 2016
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHandler;
import de.sybig.oba.server.OntologyResource;
import de.sybig.oba.server.pluginManagment.OntologyLoader;
import java.util.Properties;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class inits the alignment ontology and loads the two source
 * ontologies into the alignment ontology.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignmentOntologyLoader implements OntologyLoader {

    private static final Logger log = LoggerFactory.getLogger(AlignmentOntologyLoader.class);
    private static final String ONTOLOGIES = "alignment_ontologies";
    private Properties properties;

    private AlignmentOntology ontology;

    @Override
    public ObaOntology loadOntology(Properties p) {
        if (p == null) {
            throw new IllegalArgumentException("The properties for the alignment ontology are null");
        }
        properties = p;
        ontology = new AlignmentOntology();
        ontology.setProperties(properties);
        ontology.setOwlURI(IRI.create(properties.getProperty("alignment_IRI", "alignment")));
        loadSourceOntologies();
        return ontology;
    }

    private void loadSourceOntologies() {
        OntologyHandler oh = OntologyHandler.getInstance();
        String[] ontologies = properties.getProperty(ONTOLOGIES).split(";");
        if (ontologies.length != 2) {
            log.error("The alignemnt ontologies need 2 source ontologies, "
                    + "removing ontology there are {} source ontologies", ontologies.length);
            oh.deleteOntology(properties.getProperty("identifier"));
            return;
        }
        OntologyResource ontoA = oh.getOntology(ontologies[0]);
        OntologyResource ontoB = oh.getOntology(ontologies[1]);
        if (ontoA == null || ontoB == null){
            log.error("One or both of the source ontologies for the alignment "
                    + "are missing. Loaded ontologies {}, {}. Removing alignment ontology",
                    ontoA, ontoB);
             oh.deleteOntology(properties.getProperty("identifier"));
        }
        ontology.setOntoA(ontoA);
        ontology.setOntoB(ontoB);
    }

}
