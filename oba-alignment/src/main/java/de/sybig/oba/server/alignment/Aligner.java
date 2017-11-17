package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHelper;
import de.sybig.oba.server.OntologyResource;
import java.util.ArrayList;
import java.util.Arrays;
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

        ScoreWithSource score = lexCompare.compareLabels(
                getLabel(clsA, ontoA.getOntology()),
                getLabel(clsB, ontoB.getOntology()));
        return score;
    }

    public Map<String, Map<ObaClass, List<ScoreWithSource>>> mapTermsToOntology(String[] terms) {
        List<ObaClass> classesA = ontoA.getClasses();
        //Arrays.asList(terms).parallelStream().forEach(a -> classesA.parallelStream().forEach(b -> compareTerms(a, b)));
        Map<String, Map<ObaClass, List<ScoreWithSource>>> result = new HashMap<>();
        for (String t : terms){
            for (ObaClass c : classesA){
                
                ScoreWithSource scores = compareTerms(t, c);
                if (scores.getScore() == 0){
                    continue;
                }
                System.out.println(t + " &  " + c +" -> " + scores.getScore());
                if (! result.containsKey(t)){
                    result.put(t, new HashMap<ObaClass, List<ScoreWithSource>>());
                }
                Map<ObaClass, List<ScoreWithSource>> termMap = result.get(t);
                if (!termMap.containsKey(c)){
                    termMap.put(c, new ArrayList<ScoreWithSource>());
                }
                termMap.get(c).add(scores);
            }
        }
        return result;
    }

    private String getLabel(ObaClass clsA, OWLOntology ontology) {
        return OntologyHelper.getAnnotation(clsA, ontology, "label");
    }

    private ScoreWithSource compareTerms(String term, ObaClass a) {
        ScoreWithSource score = lexCompare.compareLabels(
                term, getLabel(a, ontoA.getOntology()));
        return score;
    }
}
