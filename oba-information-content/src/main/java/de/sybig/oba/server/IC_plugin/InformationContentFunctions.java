package de.sybig.oba.server.IC_plugin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaAnnotation;
import de.sybig.oba.server.ObaOntology;
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
import org.semanticweb.owlapi.model.OWLClass;

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
