package de.sybig.oba.server.IC_plugin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaAnnotation;
import de.sybig.oba.server.OntologyFunctions;
import de.sybig.oba.server.OntologyHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import org.semanticweb.owlapi.model.IRI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.opencompare.hac.dendrogram.Dendrogram;
import org.opencompare.hac.dendrogram.DendrogramNode;
import org.semanticweb.owlapi.model.OWLAnnotation;

public class InformationContentFunctions extends OntologyFunctions {

    private Set<Double> parentMuList;
    private Set<ObaClass> childrenSet;
    private HashMap<ObaClass, MuChildrenSet> muMap;

    @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
        StringBuffer out = new StringBuffer();
        out.append("<h1>Available functions</h1>\n");
        out.append("<dl>");
        out.append("<dt>/getIcValue/{cls}</dt><dd>Gets the intrinsic information content for this class, cls should be replaced with the desired class</dd>");
        out.append("<dt>/semanticSimilarity/{inA}/{inB}</dt><dd>Determins the semantic similarity between two ontology terms (inA and inB)</dd>");
        out.append("</dl>");
        return out.toString();
    }

    @GET
    @Path("/getIcValue/{cls}")
    @Produces(ALL_TYPES)
    public ObaClass getIcValue(
            @PathParam("cls") PathSegment cls,
            @QueryParam("ns") String ns) {
        ObaClass startClass = getClassFromPathSegement(cls, ns);
        ObaAnnotation icAnnotation = new ObaAnnotation();
        IRI transientIRI = IRI.create("http://oba.sybig.de/transientAnnotation#intrinsicIC");
        icAnnotation.setIri(transientIRI);
        icAnnotation.setValue(String.valueOf(getICFromNode(startClass)));

        startClass.addTransientAnnotation(icAnnotation);
        return startClass;
    }

    @GET
    @Path("/semanticSimilarity/{inA}/{inB}")
    @Produces(ALL_TYPES)
    public double semanticSimilarity(
            @PathParam("inA") PathSegment inA, 
            @PathParam("inB") PathSegment inB, 
            @QueryParam("ns") String ns) {   
        ObaClass clsA = getClassFromPathSegement(inA);
        ObaClass clsB = getClassFromPathSegement(inB, ns);
        return getSemanticSimilarity(clsA, clsB);
    }
    
    @GET
    @Path("/{pairwiseSemanticSimilarity:pairwiseSemanticSimilarity}/{testList}")
    @Produces(ALL_TYPES)
    public Map<ObaClass, List<ObaClass>> pairwiseSemanticSimilarity(
            @PathParam("pairwiseSemanticSimilarity") PathSegment functionName,
            @PathParam ("testList") String testList) {
       //This part will go away when the input changes
       List<String> myStringList = new ArrayList<String>(Arrays.asList(testList.split(",")));
        Double threshold = null;
        if (functionName.getMatrixParameters().get("threshold") != null) {
            threshold = Double.valueOf(functionName.getMatrixParameters().get("threshold").get(0));
        } else {
            threshold = 0.8;
        }

       //This section is to convert the string tronIDs to ObaClass, to remove
       List<ObaClass> termList = new ArrayList();
       for (String term : myStringList) {
           termList.add(ontology.getOntologyClass(term, null));
       }
       
       /*
       This section is a filter to test for obsolete terms
       if a term is obsolete, it is removed from both termList and myStringList
       */
       Iterator<ObaClass> it = termList.iterator();
       while (it.hasNext()){
           ObaClass term = it.next();
           Set<OWLAnnotation> myAnnotations = term.getAnnotations(term.getOntology());
           for (OWLAnnotation annotation : myAnnotations){
               String propertyName = annotation.getProperty().toString();
               if (propertyName.indexOf("is_obsolete") != -1) {
                   it.remove();
                   myStringList.remove(term.getReal().toString().split("#")[1].replace(">", ""));
               }
           }
       }
       
       //This is the actual start, initializing class
       OntologyTermCluster myClass = new OntologyTermCluster();
       //Output the dendrogram from OntologyTermCluster
       Dendrogram results = myClass.cluster(termList, ontology);
       //Getting the root of the dendrogram in order to process tree, automate this
       DendrogramNode myRoot = results.getRoot();
       
       //This is to get the average dissimilarity of the input clusters
       List<Double> disList = myClass.getDissimilarityValues(myRoot);
       double disAverage = myClass.calculateAverage(disList);
       
       //This is the list of dendrogram nodes created through dissimilarity cutoff
       List<DendrogramNode> myList = myClass.getDissimilarityNodes(myRoot, threshold);
       
       //This is to get the clustered tron terms
       //myResults is a map that contains the merge or observation node and the
       //corresponding terms
       HashMap<DendrogramNode, List<String>> myResults = myClass.getClusteredTerms(myList, myStringList);
      
       Map<ObaClass, List<ObaClass>> termResults = getRepTerms(myResults);

       return termResults;
    }    
 
///////////////////////////////////////////////////////////////////////////////   
    public HashMap<ObaClass, List<ObaClass>> getRepTerms(HashMap<DendrogramNode, List<String>> myResults) {
        //Initializing classes that will be necessary for this method
        //InformationContentFunctions icFunctions = new InformationContentFunctions();
        //OntologyFunctions ontologyFunctions = new OntologyFunctions();

        //This is the list that will contain our final results
        HashMap<String, List<String>> repTerms = new HashMap();
        HashMap<ObaClass, List<ObaClass>> newRepTerms = new HashMap();

        //Start looping through input results...
        for (DendrogramNode node : myResults.keySet()) {
            //List to contain...
            List<ObaClass> finalAncestorList = new ArrayList();

            for (String tronID : myResults.get(node)) {
                ObaClass cls = ontology.getOntologyClass(tronID, null);
                //Will retrieve most ancestors for class
                List<List<ObaClass>> ancestorList = getShortestPathsToRoot(cls);

                /*Since ancestor list can have duplicate ancestors
                 * creating a set to contain all ancestors and reduce to non-redundant
                 */
                Set<ObaClass> ancestorFlatSet = new HashSet();

                /*This adds the ancestors from ancestorList (which is a list of lists)
                 * to the ancestor set
                 */
                for (List<ObaClass> ancestor : ancestorList) {
                    ancestorFlatSet.addAll(ancestor);
                }

                //Converting back to list for later use in counting frequencies
                List<ObaClass> targetList = new ArrayList(ancestorFlatSet);
                finalAncestorList.addAll(targetList);
            }
            //This is the set of unique ancestors used to find the frequencies
            Set<ObaClass> ancestorCounter = new HashSet();
            ancestorCounter.addAll(finalAncestorList);

            /* Finds common ancestors for set of terms and the IC of the common ancestors
             * returns the ancestor with the highest IC, known as MICA (most informative
             * common ancestor)
             */
            //This map contains the ancestors and their IC values
            HashMap<ObaClass, Double> micaMap = new HashMap();
            for (ObaClass ancestor : ancestorCounter) {
                int freq = Collections.frequency(finalAncestorList, ancestor);
                int total = myResults.get(node).size();
                if (freq == total) {
                    double icValue = getICFromNode(ancestor);
                    micaMap.put(ancestor, icValue);
                }
            }
            //counter contains the highest IC value
            double counter = 0.0;
            //micaName contains the ancestor with the highest IC value (MICA)
            List<String> micaName = new ArrayList();
            for (ObaClass mica : micaMap.keySet()) {
                if (micaMap.get(mica) > counter) {
                    micaName.clear();
                    counter = micaMap.get(mica);
                    micaName.add(mica.toStringID().split("#")[1]);
                }
            }
            //Adding the repTerms and their clusters to the repTerms map
            repTerms.put(micaName.get(0), myResults.get(node));
        }
        
        for (String repTerm : repTerms.keySet()){
            ObaClass myKey = ontology.getOntologyClass(repTerm, null);
            List<ObaClass> obaValueList = new ArrayList();
            for (String clusteredTerm : repTerms.get(repTerm)){
                obaValueList.add(ontology.getOntologyClass(clusteredTerm, null));
            }
            newRepTerms.put(myKey, obaValueList);
        }
        
        return newRepTerms;
    }
    
    public double getSemanticSimilarity(ObaClass clsA, ObaClass clsB) {
        double classAIC = getICFromNode(clsA);
        double classBIC = getICFromNode(clsB);
        List<List<ObaClass>> classAParents = getShortestPathsToRoot(clsA);
        List<List<ObaClass>> classBParents = getShortestPathsToRoot(clsB);
        
        Set<ObaClass> classAParentsFlat = new HashSet<ObaClass>();
        
        for (List<ObaClass> ancestor : classAParents) {
            classAParentsFlat.addAll(ancestor);
        }
        
        Set<ObaClass> classBParentsFlat = new HashSet<ObaClass>();
        
        for (List<ObaClass> ancestor : classBParents) {
            classBParentsFlat.addAll(ancestor);
        }

        // finding the common ancestors
        Set<ObaClass> commonAncestors = new HashSet<ObaClass>(classAParentsFlat);
        commonAncestors.retainAll(classBParentsFlat);

        /* determinining the IC value for the common ancestors, this will be
            used to find the MICA*/
        Set<Double> ancestorICSet = new HashSet();
        for (ObaClass parent : commonAncestors) {
            ancestorICSet.add(getICFromNode(parent));            
        }
        
        double mica = -1;
        
        for (double ancestorICs : ancestorICSet) {
            if (ancestorICs > mica) {
                mica = ancestorICs;
            } 
        }

        // Which class has the higher IC
        double semanticSimilarity = 1;
        double maxIC = 1;
        
        if (classAIC > classBIC) {
            maxIC = classAIC;
            semanticSimilarity = mica / maxIC;
        } else {
            maxIC = classBIC;
            semanticSimilarity = mica / maxIC;
        }
    return semanticSimilarity;
    }
    //Just changed access, check this out....   
    public double getICFromNode(ObaClass startClass) {
        double my_mu = getMuFromNode(startClass).getMu();
        double my_IC = -Math.log10(my_mu);
        return my_IC;
    }
    
    private MuChildrenSet getMuFromNode(ObaClass startClass) {
        parentMuList = new HashSet();

        if (getMuMap().containsKey(startClass)) {
            return getMuMap().get(startClass);
        }

        double result = 1;
        for (ObaClass parent : OntologyHelper.getParents(startClass)) {
            MuChildrenSet parentMu = getMuFromNode(parent);
            parentMuList.add(parentMu.getTermValue());
        }
        for (Double parentMuValue : parentMuList) {
            result = result * parentMuValue;
        }

        getMuMap().put(startClass, new MuChildrenSet(result, OntologyHelper.getChildren(startClass).size()));
        return getMuMap().get(startClass);
    }

    private HashMap<ObaClass, MuChildrenSet> getMuMap() {
        if (muMap == null) {
            muMap = new HashMap<ObaClass, MuChildrenSet>();
            ObaClass root = ontology.getRoot();
            muMap.put(root, new MuChildrenSet(1, OntologyHelper.getChildren(root).size()));
        }
        return muMap;
    }
}
