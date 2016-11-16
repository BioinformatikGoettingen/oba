package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHelper;
import de.sybig.oba.server.OntologyResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignmentOntology extends ObaOntology {

    private OntologyResource ontoA;
    private OntologyResource ontoB;
    private Map<ObaClass, Map<ObaClass, double[]>> scores = new HashMap<>();
    private LexCompare lexCompare;

    @Override
    public void init() throws OWLOntologyCreationException {
        long start = System.currentTimeMillis();
        List<ObaClass> classesA = ontoA.getOntology().getClasses();
        List<ObaClass> classesB = ontoB.getOntology().getClasses();
        lexCompare = new LexCompare(getProperties());

        classesA.parallelStream().forEach(a -> classesB.parallelStream().forEach(b -> compareClasses(a, b)));

        System.out.println(scores.size() + " init alignment in (ms) " + (System.currentTimeMillis() - start));
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
        compareLabels(clsA, clsB);
    }

    private void compareLabels(ObaClass clsA, ObaClass clsB) {
        String labelA = getLabel(clsA, ontoA.getOntology().getOntology());
        String labelB = getLabel(clsB, ontoB.getOntology().getOntology());
        safeScore(Methods.LABEL_EQUAL.getPosition(), clsA, clsB, lexCompare.compareLabels(labelA, labelB));
    }

    private synchronized void safeScore(int method, ObaClass clsA, ObaClass clsB, double score) {

        if (!scores.containsKey(clsA)) {
            scores.put(clsA, new HashMap<>());
        }
        Map<ObaClass, double[]> map1 = scores.get(clsA);
        if (!map1.containsKey(clsB)) {
            map1.put(clsB, new double[1]);
        }
        map1.get(clsB)[0] = score;
    }

    private String getLabel(ObaClass clsA, OWLOntology ontology) {
        return OntologyHelper.getAnnotation(clsA, ontology, "label");
    }

    public Map<ObaClass, Map<ObaClass, double[]>> getScores() {
        return scores;
    }
}
