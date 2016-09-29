/*
 * Created on May 4, 2010
 *
 */
package de.sybig.oba.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonObjectProperty;
import de.sybig.oba.server.JsonPropertyList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The generic connector can be embedded into Java applications to call the
 * REST endpoints of the OBA service using Java methods.
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * @param <C> The implementation of the ontology class.
 * @param <CL> The implementation of the ontology class list.
 * @param <C2L> The implementation of the two dimensional ontology class list.
 */
public class GenericConnector<C extends OntologyClass, CL extends AbstractOntologyClassList<C>, C2L extends AbstractOntology2DClassList<CL, C>> {

    private final Logger logger = LoggerFactory.getLogger(GenericConnector.class);
    protected String ontology;
    protected Client client;
    protected String baseURI;
    private Properties props;

    /**
     * Creates a new connector to work with the specified ontology. For a list
     * of the loaded ontologies and their names, please refer to the front page
     * of the oba server.
     *
     * @param onto The onotology to use on the OBA server
     */
    public GenericConnector(final String onto) {
        this.ontology = onto;
        init();
    }

    /**
     * Searches a class in the ontology. The pattern is searched in the class
     * name and the annotation fields indexed on the server during the loading
     * of the ontology. Using the property file for the ontology on the server,
     * it is possible to limit the fields to index to a subset. On client side
     * the method {@link #searchCls(String, List)} restrict the search to a set
     * of annotation fields.
     *
     * @param pattern The search pattern
     * @return Ontology classes matching the pattern.
     * @throws java.net.ConnectException when the connection to the server fails.
     */
    public CL searchCls(final String pattern) throws ConnectException {
        return searchCls(pattern, (String) null);
    }

    public CL searchCls(final String pattern, final String field) throws ConnectException {
        List<String> fieldList = new LinkedList<String>();
        return searchCls(pattern, fieldList, 0);
    }

    /**
     * Searches the ontology for classes matching the pattern. The search is
     * limited to the annotation fields listed in the second parameter. To
     * include the name of the ontology class in the search scope, 'classname'
     * has to be added to the list of fields.
     *
     * @param pattern The search pattern
     * @param annotationFields The annotation fields to search in
     * @throws java.net.ConnectException when the connection to the server fails.
     * @return A list of classes matching the search pattern.
     */
    public CL searchCls(final String pattern, List<String> annotationFields) throws ConnectException {
        return searchCls(pattern, annotationFields, 0);
    }

    public CL searchCls(final String pattern, List<String> annotationFields, int maxResults) throws ConnectException {
        String path = String.format("%s/functions/basic/", getOntology());
        WebResource webResource = getWebResource();
        // webResource.
        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment("searchCls");
        if (annotationFields != null && annotationFields.size() > 0) {
            StringBuilder paramValue = new StringBuilder();
            Iterator<String> it = annotationFields.iterator();
            while (it.hasNext()) {
                String field = it.next();
                paramValue.append(field);
                if (it.hasNext()) {
                    paramValue.append(',');
                }
            }
            uriBuilder = uriBuilder.matrixParam("field", paramValue.toString());
            if (maxResults > 0) {
                uriBuilder = uriBuilder.matrixParam("max", maxResults);
            }
        }
        uriBuilder = uriBuilder.segment(pattern);
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            CL list = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(
                    getOntologyClassList());
            list.setConnector(this);
            return list;
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 204) {
                    return null;
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
        return null;
    }

    /**
     * Get the root of the ontology, that will be owl:Thing
     * @throws java.net.ConnectException when the connection to the server fails.
     *
     * @return
     */
    public C getRoot() throws ConnectException {
        String path = String.format("%s/cls/", getOntology());
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        Class c = getOntologyClass();
        try {
            C response = (C) webResource.accept(MediaType.APPLICATION_JSON).get(c);
            response.setConnector(this);
            return response;
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
     * Gets the same class from the ontology as specified as parameter. A class
     * with all parents and children are returned also if the parameter object
     * is a shell for the class.
     *
     * <code>null</code> is returned when the class could not be retrieved from
     * the ontology.
     *
     * @param c The class to get the name and namespace from.
     * @throws java.net.ConnectException when the connection to the server fails.
     * @return The complete class from the ontology server, or
     * <code>null</code>.
     */
    public C getCls(final OntologyClass c) throws ConnectException {
        if (c == null) {
            throw new IllegalArgumentException("The query ontolgoy class may not be null");
        }
        C cls = getCls(c.getName(), c.getNamespace());
        if (cls == null) {
            logger.error("Could not get the class {} in namespace {} from the ontology", c.getName(), c.getNamespace());
            return null;
        }
        cls.setConnector(this);
        return cls;
    }

    /**
     * Get a class with the given name in the given namespace. The class should
     * exists on in the ontology, otherwise the HTTP status code 404 is
     * returned.
     *
     * @param name The name of the ontology class.
     * @param ns The namespace of the ontology class.
     * @throws java.net.ConnectException when the connection to the server fails.
     * @return The requested ontology class or <code>null</code>.
     */
    public C getCls(final String name, final String ns) throws ConnectException {
        String path;
        path = String.format("%s/cls/%s", getOntology(), name);
        WebResource webResource = getWebResource();
        if (ns != null && ns.trim().length() > 0) {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("ns", ns);
            webResource = webResource.queryParams(queryParams);
        }
        webResource = webResource.path(path);
        try {
            C response = (C) webResource.accept(MediaType.APPLICATION_JSON).get(getOntologyClass());
            response.setConnector(this);
            return response;
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

    public CL getDescendants(final C cls) throws ConnectException {
        return getDescendants(cls.getName(), cls.getNamespace());
    }

    public CL getDescendants(final String name, final String ns) throws ConnectException {
        String path = String.format("%s/functions/basic/descendants/%s", getOntology(), name);
        WebResource webResource = getWebResource();
        if (ns != null && ns.trim().length() > 0) {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("ns", ns);
            webResource = webResource.queryParams(queryParams);
        }
        webResource = webResource.path(path);
        try {
            CL response = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(getOntologyClassList());
            response.setConnector(this);
            return response;
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
     * Returns the paths between clsX and clsY. All shortest paths between the
     * two classes are returned, where clsX should be an ancestor of clsY. If no
     * path between the classes are found, or clsY is downstream of clsX no path
     * is returned.
     *
     * @param clsX The downstream class
     * @param clsY The upstream class
     * @throws java.net.ConnectException when the connection to the server fails.
     * @return The shortest paths between the two classes.
     */
    public C2L xDownstreamOfY(OntologyClass clsX, OntologyClass clsY) throws ConnectException {
        String path;
        path = String.format("%s/functions/basic/XdownstreamOfY", getOntology());
        WebResource webResource = getWebResource();

        UriBuilder uriBuilder = webResource.getUriBuilder();
        uriBuilder = uriBuilder.path(path);

        uriBuilder = uriBuilder.segment(clsX.getName());
        uriBuilder = uriBuilder.matrixParam("ns",
                clsX.getNamespace().replace("/", "$"));
        uriBuilder = uriBuilder.segment(clsY.getName());
        uriBuilder = uriBuilder.queryParam("ns", clsY.getNamespace());
        URI uri = uriBuilder.build();
        webResource = webResource.uri(uri);
        try {
            C2L list = (C2L) webResource.accept(MediaType.APPLICATION_JSON).get(getOntology2DClassList());
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

    /**
     * Stores a list of ontology classes on the server.
     *
     * @param partition The partition to store the list in
     * @param id the Name of the list
     * @throws java.net.ConnectException when the connection to the server fails.
     * @param list The list with the ontology classes
     */
    public void storeList(String partition, String id, JsonClsList list) throws ConnectException {
        String path;
        path = String.format("/storage/%s/%s", partition, id);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        JsonClsList putList;
        if (list instanceof AbstractOntologyClassList) {
            // reset the connector to hinder the marshaller to load more classes
            putList = ((AbstractOntologyClassList) list).clone();
            ((AbstractOntologyClassList) putList).setConnector(null);
        } else {
            putList = list;
        }
        if (putList != null && putList.getEntities() != null) {
            for (JsonCls jc : (List<JsonCls>) putList.getEntities()) {
                if (jc instanceof OntologyClass) {
                    System.out.println("resetting connector");
                    ((OntologyClass) jc).setConnector(null);
                }

            }
        }
        try {
            webResource.type(MediaType.APPLICATION_JSON).put(putList);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null && ex.getResponse().getClientResponseStatus() != null) {
                if (ex.getResponse().getClientResponseStatus().getStatusCode() == 404) {
                    //TODO throw exception
                }
            }
            logger.error("error while communicating the OBA server", ex);
        }
    }

    public JsonClsList getStoredList(String partition, String id) throws ConnectException {
        String path;
        path = String.format("/storage/%s/%s", partition, id);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            CL list = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(
                    getOntologyClassList());
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

    /**
     * Get all object properties from the ontology.
     *
     * @throws java.net.ConnectException when the connection to the server fails.
     * @return All object properties
     */
    public JsonPropertyList<JsonObjectProperty> getObjectProperties() throws ConnectException {
        String path = String.format("%s/objectProperty/", getOntology());
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        Class<JsonPropertyList> clazz = getPropertyListClass();
        try {
            JsonPropertyList response = (JsonPropertyList) webResource.accept(
                    MediaType.APPLICATION_JSON).get(clazz);
            return response;
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

    public CL reduceToLevel(int level, OntologyClass cls) throws ConnectException {
        return reduceToLevel(level, cls.getName(), cls.getNamespace());
    }

    public CL reduceToLevel(int level, String name, String ns) throws ConnectException {
        String path;
        path = String.format("%s/functions/basic/reduceToLevel/%d/%s",
                getOntology(), level, name);
        WebResource webResource = getWebResource();

        if (ns != null && ns.trim().length() > 0) {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("ns", ns);
            webResource = webResource.queryParams(queryParams);
        }
        webResource = webResource.path(path);
        try {
            CL response = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(getOntologyClassList());
            response.setConnector(this);
            return response;
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

    public C2L reduceStoredSetToLevel(int level, String partition, String set) throws ConnectException {
        String path;
        path = String.format("%s/functions/basic/reduceToLevel/%d/%s/%s",
                getOntology(), level, partition, set);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            C2L response = (C2L) webResource.accept(MediaType.APPLICATION_JSON).get(getOntology2DClassList());
            response.setConnector(this);
            return response;
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

    public CL reduceToLevelShortestPath(int level, OntologyClass cls) throws ConnectException {
        return reduceToLevelShortestPath(level, cls.getName(),
                cls.getNamespace());
    }

    public CL reduceToLevelShortestPath(int level, String name, String ns) throws ConnectException {
        String path;
        path = String.format(
                "%s/functions/basic/reduceToLevelShortestPath/%d/%s",
                getOntology(), level, name);
        WebResource webResource = getWebResource();

        if (ns != null && ns.trim().length() > 0) {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("ns", ns);
            webResource = webResource.queryParams(queryParams);
        }
        webResource = webResource.path(path);
        try {
            CL response = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(getOntologyClassList());
            response.setConnector(this);
            return response;
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

    public CL reduceStoredSetToLevelShortestPath(int level, String partition,
            String set) throws ConnectException {
        String path;
        path = String.format(
                "%s/functions/basic/reduceToLevelShortestPath/%d/%s/%s",
                getOntology(), level, partition, set);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            CL response = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(getOntologyClassList());
            response.setConnector(this);
            return response;
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

    public C2L reduceToClusterSize(int size, String partition, String set) throws ConnectException {
        String path;
        path = String.format("%s/functions/basic/reduceToClusterSize/%d/%s/%s",
                getOntology(), size, partition, set);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);

        try {
            C2L response = (C2L) webResource.accept(MediaType.APPLICATION_JSON).get(getOntology2DClassList());

            return response;
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

    protected void init() {
        ClientConfig cc = new DefaultClientConfig();
        cc.getClasses().add(JacksonJsonProvider.class);
        client = Client.create(cc);

        Properties defaultProps = new Properties();
        try {
            InputStream stream = getClass().getResourceAsStream(
                    "/oba-client.properties");
            defaultProps.load(stream);
            stream.close();
        } catch (IOException e) {
            logger.error("could not load properties for the client", e);
        }
        File userPropFile = new File(System.getProperty("user.home"),
                ".oba-client.properties");
        if (userPropFile.exists() && userPropFile.canRead()) {
            logger.info("found property file '{}' for the client", userPropFile);
            props = new Properties(defaultProps);
            try {
                FileReader fr = new FileReader(userPropFile);
                props.load(fr);
                fr.close();
            } catch (FileNotFoundException e) {
                logger.error(
                        "could not read property file '{}' for the client, using default values, error was {}",
                        userPropFile, e.getMessage());
                props = defaultProps;
            } catch (IOException e) {
                logger.error(
                        "could not read property file '{}' for the client, using default values, error was {}",
                        userPropFile, e.getMessage());
                props = defaultProps;
            }
        } else {
            logger.info(
                    "didn't found property file '{}' for the client, using default values",
                    userPropFile);
            props = defaultProps;
        }
        baseURI = props.getProperty("base_uri", "http://localhost:9998/");
    }

    /**
     * Gets the ontology this instance of the connector is working on.
     *
     * @return the ontology
     */
    public String getOntology() {
        return ontology;
    }

    /**
     * Sets the ontology this instance of the connector is working on.
     *
     * @param ontology the ontology to set
     */
    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    /**
     * Sets the base uri to communicte with the server.
     *
     * @return
     */
    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public static void main(String[] args) {
        try {
            GenericConnector c = new GenericConnector("go");
            AbstractOntology2DClassList result = c.reduceStoredSetToLevel(2, "tmp", "obaWebDemo");
            System.out.println("result " + result);
        } catch (ConnectException ex) {
            System.out.println("OBA server could not be reached.");
        }
    }

    @Override
    public String toString() {
        return "oba connector for " + ontology + " on " + getBaseURI();
    }

    protected <T> T getResponse(WebResource webResource, Class<T> cl) {

        T returnObject;
        try {
            returnObject = webResource.accept(MediaType.APPLICATION_JSON).get(
                    cl);
        } catch (UniformInterfaceException ex) {
            // logger.error("the request {}", ex.getResponse());
            int status = ex.getResponse().getStatus();
            if (status == 404) {
                logger.error("the method is not available on the server [status 404]");
            } else if (status == 406) {
                logger.error("the method is available on the server but can not return the response with the JSON media type [status 406]");
            }
            return null;
        } 
        return returnObject;
    }

    protected WebResource getWebResource() {
        WebResource resource = client.resource(baseURI);
        // resource.addFilter(new GZIPContentEncodingFilter(false));
        return resource;
    }

    // Helper methods to get the class objects for the unmarshaller
    protected Class getOntologyClassList() {
        return (Class) OntologyClassList.class;
    }

    protected Class getOntologyClass() {
        return (Class) OntologyClass.class;
    }

    protected Class getOntology2DClassList() {
        return Ontology2DClassList.class;
    }

    protected Class getPropertyListClass() {
        return JsonPropertyList.class;
    }

//    protected Ontology2DClassList convert2DClassList(Object o) {
//        Json2DClsList<JsonClsList<JsonCls>, JsonCls> list = (Json2DClsList<JsonClsList<JsonCls>, JsonCls>) o;
//        Ontology2DClassList ol = new Ontology2DClassList();
//        for (JsonClsList jl : list.getEntities()) {
////            ol.add(convertClassList(jl)); //FIXME
//        }
//        return ol;
//    }
    protected OntologyClassList convertClassList(JsonClsList<JsonCls> jl) {
        OntologyClassList ol = new OntologyClassList();
        for (JsonCls jc : jl.getEntities()) {
            ol.add(convertClass(jc));
        }
        return ol;
    }

    protected OntologyClass convertClass(JsonCls jc) {
        OntologyClass c = new OntologyClass();
        c.setConnector(this);
        return c;
    }
}
