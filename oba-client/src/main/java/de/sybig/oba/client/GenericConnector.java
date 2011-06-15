/*
 * Created on May 4, 2010
 *
 */
package de.sybig.oba.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.sybig.oba.server.Json2DClsList;
import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonObjectProperty;
import de.sybig.oba.server.JsonObjectPropertyExpression;
import de.sybig.oba.server.JsonPropertyList;

public class GenericConnector<C extends OntologyClass, CL extends OntologyClassList<C>, C2L extends Json2DClsList<CL, C>> {

	private Logger logger = LoggerFactory.getLogger(GenericConnector.class);
	protected String ontology;
	Client client = Client.create();
	String BASE_URI;
	private Properties props;

	/**
	 * Creates a new connector to work with the specified ontology. For a list
	 * of the loaded ontologies and their names, please refer to the front page
	 * of the oba server.
	 * 
	 * @param ontology
	 */
	public GenericConnector(String ontology) {
		this.ontology = ontology;
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
	 * @see de.sybig.oba.server.OntologyFunctions#serachCls(String)
	 * @param pattern
	 *            The search pattern
	 * @return Ontology classes matching the pattern.
	 */
	public CL searchCls(final String pattern) {
		return searchCls(pattern, null);
	}

	/**
	 * Searches the ontology for classes matching the pattern. The search is
	 * limited to the annotation fields listed in the second parameter. To
	 * include the name of the ontology class in the search scope, 'classname'
	 * has to be added to the list of fields.
	 * 
	 * @param pattern
	 *            The search pattern
	 * @param annotationFields
	 *            The annotation fields to search in
	 * @return A list of classes matching the search pattern.
	 */
	public CL searchCls(final String pattern, List<String> annotationFields) {
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
					paramValue.append(",");
				}
			}
			uriBuilder = uriBuilder.matrixParam("field", paramValue.toString());
		}
		uriBuilder = uriBuilder.segment(pattern);
		URI uri = uriBuilder.build();
		webResource = webResource.uri(uri);
		CL list = (CL) webResource.accept(MediaType.APPLICATION_JSON).get(
				getOntologyClassList());
		list.setConnector(this);
		return list;
	}

	/**
	 * Get the root of the ontology, that will be owl:Thing
	 * 
	 * @return
	 */
	public C getRoot() {
		String path = String.format("%s/cls/", getOntology());
		WebResource webResource = getWebResource();
		webResource = webResource.path(path);
		Class c = getOntologyClass();
		C response = (C) webResource.accept(MediaType.APPLICATION_JSON).get(c);
		response.setConnector(this);
		return response;
	}

	/**
	 * Gets the same class from the ontology as specified as parameter. A class
	 * with all parents and children are returned also if the parameter object
	 * is a shell for the class.
	 * 
	 * @param c
	 *            The class to get the name and namespace from.
	 * @return The complete class from the ontology server.
	 */
	public C getCls(final OntologyClass c) {
		// if (c == null) {
		// return getRoot();
		// }
		C cls = getCls(c.getName(), c.getNamespace());
		cls.setConnector(this);
		return cls;
	}

	/**
	 * Get a class with the given name in the given namespace. The class should
	 * exists on in the ontology, otherwise the HTTP status code 404 is
	 * returned.
	 * 
	 * @param name
	 *            The name of the ontology class.
	 * @param ns
	 *            The namespace of the ontology class.
	 * @return The requested ontology class or <code>null</code>.
	 */
	public C getCls(final String name, final String ns) {
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
			C response = (C) webResource.accept(MediaType.APPLICATION_JSON)
					.get(getOntologyClass());
			response.setConnector(this);
			return response;
		} catch (UniformInterfaceException ex) {
			logger.error(
					"the class '{}' in namespace '{}' was not found on the server",
					name, ns);
			logger.error(ex.getMessage());
			return null;
		}
	}

	/**
	 * Returns the paths between clsX and clsY. All shortest paths between the
	 * two classes are returned, where clsX should be an ancestor of clsY. If no
	 * path between the classes are found, or clsY is downstream of clsX no path
	 * is returned.
	 * 
	 * @param clsX
	 *            The downstream class
	 * @param clsY
	 *            The upstream class
	 * @return The shortest paths between the two classes.
	 */
	public C2L xDownstreamOfY(OntologyClass clsX, OntologyClass clsY) {
		String path;
		path = String
				.format("%s/functions/basic/XdownstreamOfY", getOntology());
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
		C2L list = (C2L) webResource.accept(MediaType.APPLICATION_JSON).get(
				getOntology2DClassList());
		return list;
	}

	/**
	 * Stores a list of ontology classes on the server.
	 * 
	 * @param partition
	 *            The partition to store the list in
	 * @param id
	 *            the Name of the list
	 * @param list
	 *            The list with the ontology classes
	 */
	public void storeList(String partition, String id, JsonClsList list) {
		String path;
		path = String.format("/storage/%s/%s", partition, id);
		WebResource webResource = getWebResource();
		webResource = webResource.path(path);
		JsonClsList putList;
		if (list instanceof OntologyClassList) {
			// reset the connector to hinder the marshaller to load more classes
			putList = ((OntologyClassList) list).clone();
			((OntologyClassList) putList).setConnector(null);
		} else {
			putList = list;
		}
		webResource.type(MediaType.APPLICATION_JSON).put(putList);

	}

	/**
	 * Get all object properties from the ontology.
	 * 
	 * @return
	 */
	public JsonPropertyList<JsonObjectProperty> getObjectProperties() {
		String path = String.format("%s/objectProperty/", getOntology());
		WebResource webResource = getWebResource();
		webResource = webResource.path(path);
		Class<JsonPropertyList> clazz = getPropertyListClass();

		JsonPropertyList response = (JsonPropertyList) webResource.accept(
				MediaType.APPLICATION_JSON).get(clazz);
		return response;
	}

	protected void init() {
		Properties defaultProps = new Properties();
		try {
			defaultProps.load(getClass().getResourceAsStream(
					"/oba-client.properties"));
		} catch (IOException e) {
			logger.error("could not load properties for the client");
			e.printStackTrace();
		}
		File userPropFile = new File(System.getProperty("user.home"),
				".oba-client.properties");
		if (userPropFile.exists() && userPropFile.canRead()) {
			logger.info("found property file '{}' for the client", userPropFile);
			props = new Properties(defaultProps);
			try {
				props.load(new FileReader(userPropFile));
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
		BASE_URI = props.getProperty("base_uri", "http://localhost:9998/");
	}

	/**
	 * Gets the ontology this instance of the connector is working on.
	 * 
	 * @return the ontology
	 */
	protected String getOntology() {
		return ontology;
	}

	/**
	 * Sets the ontology this instance of the connector is working on.
	 * 
	 * @param ontology
	 *            the ontology to set
	 */
	protected void setOntology(String ontology) {
		this.ontology = ontology;
	}

	protected <T> T getResponse(WebResource webResource, Class<T> cl) {

		T returnObject;
		try {
			returnObject = webResource.accept(MediaType.APPLICATION_JSON).get(
					cl);
		} catch (UniformInterfaceException ex) {
			logger.error("the request {}", ex.getResponse());
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
		return client.resource(BASE_URI);
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

	protected Class getPropertyClass() {
		return JsonObjectPropertyExpression.class;
	}

	protected Class getPropertyListClass() {
		return JsonPropertyList.class;
	}

	protected Ontology2DClassList convert2DClassList(Object o) {
		Json2DClsList<JsonClsList<JsonCls>, JsonCls> list = (Json2DClsList<JsonClsList<JsonCls>, JsonCls>) o;
		Ontology2DClassList ol = new Ontology2DClassList();
		for (JsonClsList jl : list.getEntities()) {
			ol.add(convertClassList(jl));
		}
		return ol;
	}

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
