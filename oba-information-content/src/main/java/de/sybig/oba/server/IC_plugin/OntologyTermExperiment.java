/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import java.util.List;
import org.opencompare.hac.experiment.Experiment;

/**
 *
 * @author kconrads
 */
public class OntologyTermExperiment implements Experiment{
    
    List<ObaClass> annotations;
    ObaOntology ontology;
    //InformationContentFunctions informationContentClass = new InformationContentFunctions();
    
    public OntologyTermExperiment(List<ObaClass> annotations, ObaOntology ontology){
        super();
        this.annotations = annotations;
        setOntology(ontology);
    }
    
    public void setOntology(ObaOntology ontology){
        this.ontology = ontology;
    }
    
    public ObaOntology getOntology(){
    return ontology;
    }

   public int getNumberOfObservations() {
        return annotations.size();  
    }
    
    public ObaClass getAnnotation(int index){
        return annotations.get(index);
    }
    
}
