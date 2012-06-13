/*
 * Created on May 4, 2010
 *
 */
package de.sybig.oba.client;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.WebResource;

import de.sybig.oba.server.JsonCls;

/**
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class TriboliumConnector extends OboConnector // <OntologyClass,
// OntologyClassList,
// Ontology2DClassList>
{

    protected final String SUB_RESOURCE = "functions/tribolium";

    public TriboliumConnector() {
        super("tribolium");
    }

    public TriboliumConnector(String ontology) {
        super(ontology);
    }

    public OboClassList getConcreteClasses() {
        String path = String.format("%s/%s/concreteClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList concreteClasses = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            concreteClasses.setConnector(this);
            return concreteClasses;
        } catch (Exception ex) {
//            System.out.println("Exception while gettinge concrete classes " + ex);
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OboClassList getGenericClasses() {
        String path = String.format("%s/%s/genericClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList genericClasses = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            genericClasses.setConnector(this);
            return genericClasses;
        } catch (Exception ex) {
                        System.out.println("Exception while gettinge generic classes " + ex);
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OboClassList getDevelopmentalStages() {
        String path = String.format("%s/%s/devStages", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList devStages = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            devStages.setConnector(this);
            return devStages;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OboClassList searchGeneric(final String pattern) {
        String path = String.format("%s/%s/", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchGeneric");

        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }

    }

    public OboClassList searchConcrete(final String pattern) {
        String path = String.format("%s/%s/", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchConcrete");

        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }

    }

    public OboClassList searchConcreteFor(JsonCls cls) {
        String path = String.format("%s/%s/searchConcreteFor/%s",
                getOntology(), SUB_RESOURCE, cls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns", cls.getNamespace());
            OboClassList list = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OboClass getDevStageOfCls(JsonCls concreteCls) {
        String path = String.format("%s/%s/devStageOfCls/%s", getOntology(),
                SUB_RESOURCE, concreteCls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns",
                    concreteCls.getNamespace());
            OboClass cls = (OboClass) getResponse(webResource,
                    getOntologyClass());
            cls.setConnector(this);
            return cls;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    /**
     * @param genericClass
     * @param devStage
     * @return
     */
    public OboClassList concreteClassInDevStage(
            OntologyClass genericClass, OntologyClass devStage) {
        String path = String.format("%s/%s/concreteClassInDevStage/", getOntology(),
                SUB_RESOURCE);
        WebResource webResource = getWebResource();

        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment(genericClass.getName());
        uriBuilder = uriBuilder.matrixParam("ns", genericClass.getNamespace().replace("/", "$"));
        uriBuilder = uriBuilder.segment(devStage.getName());
        uriBuilder = uriBuilder.queryParam("ns", devStage.getNamespace());
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OboClassList checkClassLoops() {
        String path = String.format("%s/%s/clsLoops", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            //TODO catches to much, also method not found on server
            // an empty document results in errors in the json unmarshaller
//            System.out.println(ex);
            return null;
        }
    }

    public OboClassList checkRelationLoops(String relation) {
        String path = String.format("%s/%s/relationLoops/%s", getOntology(), SUB_RESOURCE, relation);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            //TODO catches to much, also method not found on server
            // an empty document results in errors in the json unmarshaller
//            System.out.println(ex);
            return null;
        }
    }

    public OboClassList getAllClasses() {
        String path = String.format("%s/%s/allClasses", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            System.out.println("exception whie getting all classes " +ex);
            //TODO catches to much, also method not found on server
            // an empty document results in errors in the json unmarshaller
//            System.out.println(ex);
            return null;
        }
    }

    public static String getVersion() {
        return "20120509";
    }

    public static void main(String[] args) {
        TriboliumConnector connector = new TriboliumConnector();
        OboClassList cc = connector.getConcreteClasses();
        System.out.println(cc.size() + " concrete classes found");
        OboClassList searchWing = connector.searchCls("wing");
        System.out.println(searchWing.size() + " hits found for 'wing'");
        OboClassList searchWing2 = connector.searchGeneric("wing");
        System.out.println(searchWing2.size()
                + " generic classes found for 'wing'");
        for (Object c : searchWing2.getEntities()) {
            System.out.print(c + " ");
        }
        System.out.println();
        OboClassList concreteWing = connector.searchConcreteFor((JsonCls) searchWing2.getEntities().get(1));
        System.out.println("search concrete classes below of "
                + searchWing2.getEntities().get(1));
        for (Object c : concreteWing.getEntities()) {
            System.out.print(c + " ");
        }
        System.out.println();
    }

   
}
