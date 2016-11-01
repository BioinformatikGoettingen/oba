package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyResource;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignmentOntology extends ObaOntology {

    private OntologyResource ontoA;
    private OntologyResource ontoB;

    @Override
    public void init() throws OWLOntologyCreationException{

        System.out.println("----- root a " + ontoA.getOntology().getRoot());

    }

    /**
     * Gets the first ontology of the alignment. The
     * {@link de.sybig.oba.server.alignment.AlignmentOntologyLoader} is
     * responsible that both ontolgies are not <code>null</code>.
     *
     * @return The first ontology.
     */
    public OntologyResource getOntoA() {
        return ontoA;
    }

    /**
     * Sets the first ontology for the alignment. The ontology should not be
     * <code>null</code>
     *
     * @param ontoA The ontology to set.
     */
    public void setOntoA(OntologyResource ontoA) {
        this.ontoA = ontoA;
    }

    /**
     * Gets the second ontology of the alignment. The
     * {@link de.sybig.oba.server.alignment.AlignmentOntologyLoader} is
     * responsible that both ontolgies are not <code>null</code>.
     *
     * @return The second ontology.
     */
    public OntologyResource getOntoB() {
        return ontoB;
    }
    /**
     * Sets the second ontology for the alignment. The ontology should not be
     * <code>null</code>
     *
     * @param ontoB The ontology to set.
     */
    public void setOntoB(OntologyResource ontoB) {
        this.ontoB = ontoB;
    }

}
