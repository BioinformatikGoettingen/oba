package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class Aligner {

    private final LexCompare lexCompare;
    private final ObaOntology ontoA;
    private ObaOntology ontoB = null;

    public Aligner(Properties properties, ObaOntology ontoA) {
        lexCompare = new LexCompare(properties);
        this.ontoA = ontoA;
    }

    public Aligner(Properties properties, ObaOntology ontoA, ObaOntology ontoB) {
        lexCompare = new LexCompare(properties);
        this.ontoA = ontoA;
        this.ontoB = ontoB;
    }

    public ScoreWithSource compareLabels(ObaClass clsA, ObaClass clsB) {
        ScoreWithSource bestScore = new ScoreWithSource(0);
        Collection<String> labelsA = getLabels(clsA, ontoA.getOntology());
        Collection<String> labelsB = getLabels(clsB, ontoA.getOntology());
        for (String labelA : labelsA) {
            for (String labelB : labelsB) {
                ScoreWithSource score = lexCompare.compareLabels(labelA, labelB);
                if (score.getScore() > bestScore.getScore()) {
                    bestScore = score;
                }
            }
        }
        return bestScore;
    }

    public Map<String, Map<ObaClass, List<ScoreWithSource>>> mapTermsToOntology(String[] terms) {
        List<ObaClass> classesA = ontoA.getClasses();
        //Arrays.asList(terms).parallelStream().forEach(a -> classesA.parallelStream().forEach(b -> compareTerms(a, b)));
        Map<String, Map<ObaClass, List<ScoreWithSource>>> result = new HashMap<>();
        for (String t : terms) {
            for (ObaClass c : classesA) {

                ScoreWithSource scores = compareTerms(t, c);
                if (scores.getScore() == 0) {
                    continue;
                }
                //System.out.println(t + " &  " + c +" -> " + scores.getScore());
                if (!result.containsKey(t)) {
                    result.put(t, new HashMap<ObaClass, List<ScoreWithSource>>());
                }
                Map<ObaClass, List<ScoreWithSource>> termMap = result.get(t);
                if (!termMap.containsKey(c)) {
                    termMap.put(c, new ArrayList<ScoreWithSource>());
                }
                termMap.get(c).add(scores);
            }
        }
        return result;
    }

    private Collection<String> getLabels(ObaClass clsA, OWLOntology ontology) {
        return OntologyHelper.getAnnotations(clsA, ontology, "label");
    }

    private ScoreWithSource compareTerms(String term, ObaClass a) {
        ScoreWithSource bestScore = new ScoreWithSource(0);
        for (String label : getLabels(a, ontoA.getOntology())) {
            ScoreWithSource score = lexCompare.compareLabels(
                    term, label);
            if (score.getScore() > bestScore.getScore()) {
                bestScore = score;
            }
        }
        return bestScore;
    }
}
