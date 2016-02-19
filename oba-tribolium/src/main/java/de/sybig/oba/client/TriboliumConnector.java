/*
 * Created on May 4, 2010
 *
 */
package de.sybig.oba.client;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.WebResource;

import de.sybig.oba.server.JsonCls;
import java.io.EOFException;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class TriboliumConnector extends OboConnector {
    private static Logger logger = LoggerFactory.getLogger(TriboliumConnector.class);
    protected final String SUB_RESOURCE = "functions/tribolium";

    public TriboliumConnector() {
        super("tribolium");
    }

    public TriboliumConnector(String ontology) {
        super(ontology);
    }
 public static String getVersion() {
        return "20130516";
    }
    public OboClassList getConcreteClasses() throws ConnectException {
        String path = String.format("%s/%s/concreteClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList concreteClasses = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            concreteClasses.setConnector(this);
            return concreteClasses;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList getGenericClasses() throws ConnectException {
        String path = String.format("%s/%s/genericClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList genericClasses = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            if (genericClasses == null){
                logger.error("No generic classes found in the ontology. The suggestion tree is not available.");
                return null;
            }
            genericClasses.setConnector(this);
            return genericClasses;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList getMixedClasses() throws ConnectException {
        String path = String.format("%s/%s/mixedClasses", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList genericClasses = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            genericClasses.setConnector(this);
            return genericClasses;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList getDevelopmentalStages() throws ConnectException {
        String path = String.format("%s/%s/devStages", getOntology(),
                SUB_RESOURCE);

        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList devStages = (OboClassList) getResponse(
                    webResource, OboClassList.class);
           
            devStages.setConnector(this);
            return devStages;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList searchInGeneric(final String pattern) throws ConnectException {
        String path = String.format("%s/%s/", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchInGeneric");

        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        }catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }
public OboClassList searchInGenericAndMixed(final String pattern) throws ConnectException {
        String path = String.format("%s/%s/", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchInGenericAndMixed");

        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }
    public OboClassList searchInConcrete(final String pattern) throws ConnectException {
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
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList searchInConcreteAndMixed(String pattern) throws ConnectException {
        String path = String.format("%s/%s/", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchInConcreteAndMixed");

        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList searchConcreteFor(JsonCls cls) throws ConnectException {
        String path = String.format("%s/%s/searchConcreteFor/%s",
                getOntology(), SUB_RESOURCE, cls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns", cls.getNamespace());
            OboClassList list = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClass getDevStageOfCls(JsonCls concreteCls) throws ConnectException {
        String path = String.format("%s/%s/devStageOfCls/%s", getOntology(),
                SUB_RESOURCE, concreteCls.getName());
        WebResource webResource = getWebResource().path(path);
        try {
            webResource = webResource.queryParam("ns",
                    concreteCls.getNamespace());
            OboClass cls = (OboClass) getResponse(webResource,
                    getOntologyClass());
            if (cls == null){
                logger.warn("could not get dev stage of class {}", concreteCls);
                return null;
            }
            cls.setConnector(this);
            return cls;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    /**
     * @param genericClass
     * @param devStage
     * @return
     */
    public OboClassList concreteClassInDevStage(
            OntologyClass genericClass, OntologyClass devStage) throws ConnectException {
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
//            System.out.println(genericClass + " in " + devStage);

        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }else if (ex.getCause() instanceof EOFException){
                //ok
                return null;
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList checkClassLoops() throws ConnectException {
        String path = String.format("%s/%s/clsLoops", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList checkRelationLoops(String relation) throws ConnectException {
        String path = String.format("%s/%s/relationLoops/%s", getOntology(), SUB_RESOURCE, relation);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    public OboClassList getAllClasses() throws ConnectException {
        String path = String.format("%s/%s/allClasses", getOntology(), SUB_RESOURCE);
        WebResource webResource = getWebResource().path(path);
        try {
            OboClassList list = (OboClassList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

   

    public static void main(String[] args)  {
        try {
            TriboliumConnector connector = new TriboliumConnector();
            OboClassList cc = connector.getConcreteClasses();
            System.out.println(cc.size() + " concrete classes found");
            OboClassList searchWing = connector.searchCls("wing");
            System.out.println(searchWing.size() + " hits found for 'wing'");
            OboClassList searchWing2 = connector.searchInGeneric("wing");
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
        } catch (ConnectException ex) {
           logger.error("Error ", ex);
        }
    }
}
