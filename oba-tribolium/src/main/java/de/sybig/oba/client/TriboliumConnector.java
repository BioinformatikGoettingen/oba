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
public class TriboliumConnector extends GenericConnector // <OntologyClass,
// OntologyClassList,
// Ontology2DClassList>
{

    protected final String SUB_RESOURCE = "functions/tribolium";

    public TriboliumConnector() {
        super("tribolium");
        System.out.println("new tribolium connector");
    }

    public OntologyClassList getConcreteClasses() {
        String path = String.format("%s/%s/concreteClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OntologyClassList concreteClasses = (OntologyClassList) getResponse(
                    webResource, OntologyClassList.class);
            concreteClasses.setConnector(this);
            return concreteClasses;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OntologyClassList getGenericClasses() {
        String path = String.format("%s/%s/genericClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OntologyClassList genericClasses = (OntologyClassList) getResponse(
                    webResource, OntologyClassList.class);
            genericClasses.setConnector(this);
            return genericClasses;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OntologyClassList getDevelopmentalStages() {
        String path = String.format("%s/%s/devStages", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OntologyClassList devStages = (OntologyClassList) getResponse(
                    webResource, OntologyClassList.class);
            devStages.setConnector(this);
            return devStages;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OntologyClassList searchGeneric(final String pattern) {
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
            OntologyClassList list = (OntologyClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }

    }

    public OntologyClassList searchConcrete(final String pattern) {
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
            OntologyClassList list = (OntologyClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }

    }

    public OntologyClassList searchConcreteFor(JsonCls cls) {
        String path = String.format("%s/%s/searchConcreteFor/%s",
                getOntology(), SUB_RESOURCE, cls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns", cls.getNamespace());
            OntologyClassList list = (OntologyClassList) getResponse(
                    webResource, OntologyClassList.class);
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public OntologyClass getDevStageOfCls(JsonCls concreteCls) {
        String path = String.format("%s/%s/devStageOfCls/%s", getOntology(),
                SUB_RESOURCE, concreteCls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns",
                    concreteCls.getNamespace());
            OntologyClass cls = (OntologyClass) getResponse(webResource,
                    OntologyClass.class);
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
    public OntologyClassList concreteClassInDevStage(
            OntologyClass genericClass, OntologyClass devStage) {
        String path;
        path = String.format("%s/%s/concreteClassInDevStage/", getOntology(),
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
            OntologyClassList list = (OntologyClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (Exception ex) {
            // an empty document results in errors in the json unmarshaller
            return null;
        }
    }

    public static void main(String[] args) {
        TriboliumConnector connector = new TriboliumConnector();
        OntologyClassList cc = connector.getConcreteClasses();
        System.out.println(cc.size() + " concrete classes found");
        OntologyClassList searchWing = connector.searchCls("wing");
        System.out.println(searchWing.size() + " hits found for 'wing'");
        OntologyClassList searchWing2 = connector.searchGeneric("wing");
        System.out.println(searchWing2.size()
                + " generic classes found for 'wing'");
        for (Object c : searchWing2.getEntities()) {
            System.out.print(c + " ");
        }
        System.out.println();
        OntologyClassList concreteWing = connector.searchConcreteFor((JsonCls) searchWing2.getEntities().get(1)); // TODO
        System.out.println("search concrete classes below of "
                + searchWing2.getEntities().get(1));
        for (Object c : concreteWing.getEntities()) {
            System.out.print(c + " ");
        }
        System.out.println();
    }

    public static String getVersion() {
        return "20111204";
    }

    @Override
    protected Class getOntologyClass() {
        return (Class) OntologyClass.class;
    }

    protected Class getOntologyClassList() {
        return (Class) OntologyClassList.class;
    }

    protected Class getOntology2DClassList() {
        return Ontology2DClassList.class;
    }
}
