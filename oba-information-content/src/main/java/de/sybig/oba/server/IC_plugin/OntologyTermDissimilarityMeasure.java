/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;
import de.sybig.oba.server.ObaClass;
import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;


/**
 *
 * @author kconrads
 */
public class OntologyTermDissimilarityMeasure implements DissimilarityMeasure {

    public double computeDissimilarity(Experiment experiment, int observation1, int observation2) {
        ObaClass annotation1 = ((OntologyTermExperiment)experiment).getAnnotation(observation1);
        ObaClass annotation2 = ((OntologyTermExperiment)experiment).getAnnotation(observation2);
        InformationContentFunctions informationContentClass = new InformationContentFunctions();
        informationContentClass.setOntology(((OntologyTermExperiment)experiment).getOntology());
        double similarity = informationContentClass.getSemanticSimilarity(annotation1, annotation2);
        return 1-similarity;
    }

   
  
}
