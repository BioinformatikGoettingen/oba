/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;
import org.opencompare.hac.agglomeration.*;
import org.opencompare.hac.dendrogram.*;
import org.opencompare.hac.experiment.*;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opencompare.hac.HierarchicalAgglomerativeClusterer;
import org.opencompare.hac.agglomeration.AverageLinkage;
import de.sybig.oba.server.IC_plugin.InformationContentFunctions;
import de.sybig.oba.server.OntologyFunctions;

/**
 *
 * @author kconrads
 */
public class OntologyTermCluster /*extends InformationContentFunctions*/{
    
    public List<DendrogramNode> mergeNodeList;
    public List<DendrogramNode> observationNodeList;
    public List<Integer> indexList;
    public List<Double> dissimilarityValuesList;
    public List<DendrogramNode> dissimilarityNodeList;
    public List<DendrogramNode> dissObservationNodeLis;
    
    public Dendrogram cluster(final List<ObaClass> termList, ObaOntology ontology) {
        final Experiment experiment = new OntologyTermExperiment(termList, ontology);
        final DissimilarityMeasure dissimilarityMeasure = new OntologyTermDissimilarityMeasure();
        final AgglomerationMethod agglomerationMethod = new AverageLinkage();
        final DendrogramBuilder dendrogramBuilder = new DendrogramBuilder(experiment.getNumberOfObservations());
        final HierarchicalAgglomerativeClusterer clusterer = new HierarchicalAgglomerativeClusterer(experiment, dissimilarityMeasure, agglomerationMethod);
        clusterer.cluster(dendrogramBuilder);
        final Dendrogram dendrogram = dendrogramBuilder.getDendrogram();
        return dendrogram; 
    }
        
    // Retrieve the index of the observations nodes
    public List<Integer> getIndexList(DendrogramNode node){
        if (indexList == null){
            fillList(node);
        }
        return indexList;
    }
    
    // Retrieve the observation nodes
    public List<DendrogramNode> getObservationNodes(DendrogramNode node){
        if ( observationNodeList == null){
            fillList(node);
        }
        return observationNodeList;
    }
    
    // Retrieve the merge nodes
    public List<DendrogramNode> getMergeNodes(DendrogramNode node) {
        if (mergeNodeList == null) {
            fillList(node);
        }
        return mergeNodeList;
    }
    
    // Retrieve the dissimilarity from the merge nodes
    // This is the dissimilarity between the two l/r children of node
    public List<Double> getDissimilarityValues(DendrogramNode node) {
        if (dissimilarityValuesList == null) {
            fillList(node);
        }
        return dissimilarityValuesList;
    }
    
    //This is how to retrieve clusters based on certain dissimilarity
    public List<DendrogramNode> getDissimilarityNodes(DendrogramNode node, double dissimilarityCutOff) {
        if (dissimilarityNodeList == null) {
           dissimilarityNodeFillList(node, dissimilarityCutOff);
        }
        return dissimilarityNodeList;
    }
    
    // Split list initialization and fill, this is to initialize lists
    private void initList(){
        mergeNodeList = new ArrayList();
        observationNodeList = new ArrayList();
        indexList = new ArrayList();
        dissimilarityValuesList = new ArrayList();
    }
    
    //This is the initilization for the dissimilarityNode output
    private void initDissList(){
        dissimilarityNodeList = new ArrayList();
    }
    
    // This is to fill lists
    private void fillList(DendrogramNode node) {
        initList();
        processTree(node);
    }
    
    //This is the fill list for the dissimilarityNode output
    private void dissimilarityNodeFillList(DendrogramNode node, double disAverage){
        initDissList();
        dissimilarityTraversal(node, disAverage);
    }
    
    // This is used to traverse and process the results
    private void processTree(DendrogramNode node) {
       if (node instanceof ObservationNode) {
            observationNodeList.add(node);
            indexList.add(((ObservationNode)node).getObservation());
        } else if (node instanceof MergeNode) {
            mergeNodeList.add(node);
            dissimilarityValuesList.add(((MergeNode)node).getDissimilarity());
            processTree(((MergeNode) node).getLeft());
            processTree(((MergeNode) node).getRight());
        }
    }
    
    //This is to traverse the tree to certain dissimilarity depth
    private void dissimilarityTraversal(DendrogramNode node, double dissimilarityAverage) {
        double disAverage = dissimilarityAverage;
        if (node instanceof ObservationNode){
            dissimilarityNodeList.add(node);
        }
        else if (((MergeNode)node).getDissimilarity() < disAverage){
            dissimilarityNodeList.add(node);
        }
        else {
            dissimilarityTraversal(((MergeNode) node).getLeft(), disAverage);
            dissimilarityTraversal(((MergeNode) node).getRight(), disAverage);
        }
    }
    
//    public HashMap<String, List<String>> getRepTerms(HashMap<DendrogramNode, List<String>> myResults, ObaOntology ontology) {
//        //Initializing classes that will be necessary for this method
//        //InformationContentFunctions icFunctions = new InformationContentFunctions();
//        //OntologyFunctions ontologyFunctions = new OntologyFunctions();
//        
//        //This is the list that will contain our final results
//        HashMap<String, List<String>> repTerms = new HashMap();
//        
//        //Start looping through input results...
//        for (DendrogramNode node : myResults.keySet()) {
//            //List to contain...
//            List<ObaClass> finalAncestorList = new ArrayList();
//
//            for (String tronID : myResults.get(node)) {
//                ObaClass cls = ontology.getOntologyClass(tronID, null);
//                //Will retrieve most ancestors for class
//                List<List<ObaClass>> ancestorList = getShortestPathsToRoot(cls);
//
//                /*Since ancestor list can have duplicate ancestors
//                 * creating a set to contain all ancestors and reduce to non-redundant
//                 */
//                Set<ObaClass> ancestorFlatSet = new HashSet();
//
//                /*This adds the ancestors from ancestorList (which is a list of lists)
//                 * to the ancestor set
//                 */
//                for (List<ObaClass> ancestor : ancestorList) {
//                    ancestorFlatSet.addAll(ancestor);
//                }
//
//                //Converting back to list for later use in counting frequencies
//                List<ObaClass> targetList = new ArrayList(ancestorFlatSet);
//                finalAncestorList.addAll(targetList);
//            }
//            //This is the set of unique ancestors used to find the frequencies
//            Set<ObaClass> ancestorCounter = new HashSet();
//            ancestorCounter.addAll(finalAncestorList);
//
//            /* Finds common ancestors for set of terms and the IC of the common ancestors
//             * returns the ancestor with the highest IC, known as MICA (most informative
//             * common ancestor)
//             */
//            //This map contains the ancestors and their IC values
//            HashMap<ObaClass, Double> micaMap = new HashMap();
//            for (ObaClass ancestor : ancestorCounter) {
//                int freq = Collections.frequency(finalAncestorList, ancestor);
//                int total = myResults.get(node).size();
//                if (freq == total) {
//                    double icValue = getICFromNode(ancestor);
//                    micaMap.put(ancestor, icValue);
//                }
//            }
//            //counter contains the highest IC value
//            double counter = 0.0;
//            //micaName contains the ancestor with the highest IC value (MICA)
//            List<String> micaName = new ArrayList();
//            for (ObaClass mica : micaMap.keySet()) {
//                if (micaMap.get(mica) > counter) {
//                    micaName.clear();
//                    counter = micaMap.get(mica);
//                    micaName.add(mica.toStringID());
//                }
//            }
//            //Adding the repTerms and their clusters to the repTerms map
//            repTerms.put(micaName.get(0), myResults.get(node));
//        }
//        
//        return repTerms;
//    }
    
    //This is how to retrieve the clustered terms, the input is the list of
    //dendrogram nodes and a list of their string tron terms
    //myList is the list of dendrogram nodes generated after choosing dis cutoff
    //idList are the names of the terms that were clustered
    public HashMap getClusteredTerms(List<DendrogramNode> myList, List<String> idList){
        HashMap<DendrogramNode, List<DendrogramNode>> nodeList = new HashMap();
        HashMap<DendrogramNode, List<String>> tronList = new HashMap();
        for (DendrogramNode node : myList) {
            observationNodeList = null;
            if (node instanceof ObservationNode) {
                tronList.put(node, Arrays.asList(idList.get(((ObservationNode) node).getObservation())));
            }
            nodeList.put(node, getObservationNodes(node));
        }
//       for (DendrogramNode node : nodeList.keySet()){
//           System.out.println(nodeList.get(node));
//       }
        for (DendrogramNode node : nodeList.keySet()) {
            observationNodeList = null;
            List<DendrogramNode> newList = nodeList.get(node);
            List<String> tronString = new ArrayList();
            for (DendrogramNode obsNode : newList) {
                observationNodeList = null;
                tronString.add(idList.get(((ObservationNode) obsNode).getObservation()));
//               tronString.add(ontology.getOntologyClass(myStringList.get(((ObservationNode)obsNode).getObservation()), null).toStringID());
            }
            tronList.put(node, tronString);
        }
        
        return tronList;
    }
    
    //This is how to get the MICA, to be used for representative terms
    public List<String> getMICA (HashMap<ObaClass, Double> commonAncestors){
        double counter = 0.0;
        List<String> micaName = new ArrayList();
        for (ObaClass mica : commonAncestors.keySet()) {
            if (commonAncestors.get(mica) > counter) {
                micaName.clear();
                counter = commonAncestors.get(mica);
                micaName.add(mica.toString());
            }
        }
        return micaName;
    }
    
    // Used to calculate the average dissimilarity of the merge nodes
    public double calculateAverage(List<Double> dissimilarityValues) {
        Double sum = 0.0;
        if (!dissimilarityValues.isEmpty()) {
            for (Double dissimilarityValue : dissimilarityValues) {
                sum += dissimilarityValue;
            }
            return sum / dissimilarityValues.size();
        }
        return sum;
    }
}
