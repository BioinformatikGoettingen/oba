package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHelper;
import de.sybig.oba.server.OntologyResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignmentOntology extends ObaOntology {

    private OntologyResource ontoA;
    private OntologyResource ontoB;
    private OWLOntology resultOnto;
    private Map<ObaClass, Map<ObaClass, double[]>> scores = new HashMap<>();
    private LexCompare lexCompare;

    @Override
    public void init() throws OWLOntologyCreationException {
        long start = System.currentTimeMillis();
        List<ObaClass> classesA = ontoA.getOntology().getClasses();
        List<ObaClass> classesB = ontoB.getOntology().getClasses();
        resultOnto = new OWLOntologyImpl(new OWLOntologyManagerImpl(new OWLDataFactoryImpl()), new OWLOntologyID(IRI.create("alignment")));
        lexCompare = new LexCompare(getProperties());

        classesA.parallelStream().forEach(a -> classesB.parallelStream().forEach(b -> compareClasses(a, b)));

        System.out.println(scores.size() + " init alignment in (ms) " + (System.currentTimeMillis() - start));
    }

       public ObaClass getRoot() {
//           dataFactory
           return new ObaClass(ontoA.getClsRoot(), ontoA.getOntology().getOntology());  //working
//        OWLClass owlRoot = getDataFactory().getOWLThing();  // not working
//        ObaClass root = new ObaClass(owlRoot, resultOnto);
//        return root;
    }
    
    /**
     * Gets the first ontology of the alignment. The
     * {@link de.sybig.oba.server.alignment.AlignmentOntologyLoader} is
     * responsible that both ontologies are not <code>null</code>.
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
     * responsible that both ontologies are not <code>null</code>.
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

        ScoreWithSource score = lexCompare.compareLabels(
                getLabel(clsA, ontoA.getOntology().getOntology()),
                getLabel(clsB, ontoB.getOntology().getOntology()));
        if (score.getScore() > 0) {
            safeScore(score.getSource().getPosition(), clsA, clsB, score.getScore());
        }
    }

    private synchronized void safeScore(int method,
            ObaClass clsA, ObaClass clsB,
            double score) {

        if (!scores.containsKey(clsA)) {
            scores.put(clsA, new HashMap<>());
        }
        Map<ObaClass, double[]> map1 = scores.get(clsA);
        if (!map1.containsKey(clsB)) {
            map1.put(clsB, new double[Methods.values().length]);
        }
        map1.get(clsB)[method] = score;
    }

    private String getLabel(ObaClass clsA, OWLOntology ontology) {
        return OntologyHelper.getAnnotation(clsA, ontology, "label");
    }

    /**
     * Get a scores of the alignment in a 3D matrix. The first two indicies are
     * the ontology classes of the two ontologies. The last level is a an array
     * with a score for each comparison method leading to a value != 0. The
     * index for the method in the array is defined by the enum
     * {@link de.sybig.oba.server.alignment.Methods}
     *
     * @return a matrix with the scores of the alignment.
     */
    public Map<ObaClass, Map<ObaClass, double[]>> getScores() {
        return scores;
    }
    @Override
    public OWLOntology getOntology(){
        return resultOnto;
    }
}
