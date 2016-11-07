package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHelper;
import de.sybig.oba.server.OntologyResource;
import java.util.List;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignmentOntology extends ObaOntology {

    private OntologyResource ontoA;
    private OntologyResource ontoB;

    @Override
    public void init() throws OWLOntologyCreationException {
        long start = System.currentTimeMillis();
        List<ObaClass> classesA = ontoA.getOntology().getClasses();
        List<ObaClass> classesB = ontoB.getOntology().getClasses();

        for (ObaClass clsA : classesA) {
            for (ObaClass clsB : classesB) {
                compareClasses(clsA, clsB);
            }
        }
        System.out.println("init alignment in (ms) " + (System.currentTimeMillis() - start));
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

    private void compareClasses(ObaClass clsA, ObaClass clsB) {
        String labelA = getLabel(clsA, ontoA.getOntology().getOntology());
        String labelB = getLabel(clsB, ontoB.getOntology().getOntology());
        if (labelA == null || labelB == null){
            return;
        }
        labelA = labelA.replace('_', ' ');
        labelB = labelB.replace('_', ' ');
        if (labelA.equals(labelB)){
            System.out.println("match " + labelA);
        }

    }

    private String getLabel(ObaClass clsA, OWLOntology ontology) {
        return OntologyHelper.getAnnotation(clsA, ontology, "label");
    }

}
