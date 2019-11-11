package de.sybig.oba.server.IC_plugin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaAnnotation;
import de.sybig.oba.server.OntologyFunctions;
import de.sybig.oba.server.OntologyHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.semanticweb.owlapi.model.IRI;

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
        out.append("<dt>/getIcValue</dt><dd>Gets the intrinsic information content for this class</dd>");
        out.append("</dl>");
        return out.toString();
    }

    @GET
    @Path("/getIcValue/{cls}")
    @Produces(ALL_TYPES)
    public ObaClass getIcValue(
            @PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
            ObaClass startClass = ontology.getOntologyClass(cls, ns);
        //System.out.println(getICFromNode(startClass) + " ICvalue");
        ObaAnnotation icAnnotation = new ObaAnnotation();
        IRI transientIRI = IRI.create("http://oba.sybig.de/transientAnnotation#intrinsicIC");
        icAnnotation.setIri(transientIRI);
        icAnnotation.setValue(String.valueOf(getICFromNode(startClass)));
        startClass.addTransientAnnotation(icAnnotation);
        return startClass;
    }
       
    private double getICFromNode(ObaClass startClass) {
        double my_mu = getMuFromNode(startClass).getMu();
        double my_IC = -Math.log(my_mu);
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
