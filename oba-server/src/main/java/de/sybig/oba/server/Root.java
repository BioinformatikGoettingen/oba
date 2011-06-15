/*
 * Created on Oct 6, 2009
 *
 */
package de.sybig.oba.server;

import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root class for all web request. Each request is forwarded to the class which
 * handles this resource i.e. browse the ontology, functions or storage hander.
 * If needed, the ontology is loaded before.
 * 
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * 
 */
@Path("/")
public class Root {

	private Logger logger = LoggerFactory.getLogger(Root.class);
	private OntologyHandler oh = OntologyHandler.getInstance();
	private StorageHandler storageHandler = new StorageHandler();

	// @Context
	// private UriInfo uriInfo;
	// @Context
	// private Request request;

	@GET
	@Path("/")
	public Object listOntologies() {
		StringBuilder out = new StringBuilder();
		out.append("<html><head></head><body><h1>oba service</h1>");
		out.append("<p>The oba service provides a basic html interface you may use with a web browser, "
				+ "but is primary used by scripts communicating with this server using the MIME types "
				+ "application/json or text/plain</p>");
		out.append("<p>Please find more information at <a href=\"http://www.bioinf.med.uni-goettingen.de/oba\">"
				+ "http://www.bioinf.med.uni-goettingen.de/oba</a>");
		Set<String> names = oh.getOntologyNames();
		out.append("<h2>Available ontologies</h2><ul>");
		for (String n : names) {
			out.append(String.format("<li><a href=\"%s/cls\">%s</a></li>\n", n,
					n));
		}
		out.append("</ul>");
		out.append("<h2>Available function classes</h2><ul>");
		for (String name : oh.getFunctionNames()) {
			out.append(String.format(
					"<li><a href=\"functions/%s\">%s</a></li>\n", name, name));
		}
		out.append("</ul>");
		// out.append("<h2>Documentation</h2>\n");
		// out.append("The documentatin for the oba-service is <a href=\"oba-documentation.pdf\">here</a>\n");
		out.append("</body></html>");
		return out.toString();
	}

	@GET
	@Path("/oba-documentation.pdf")
	@Produces("application/pdf")
	public Object getDocumentation() {
		InputStream pdf = this.getClass().getResourceAsStream(
				"/oba-documentation.pdf");
		return pdf;
	}

	// The following to methods have to work on the resource "admin" instead of
	// "/". It is not allowed to mix subresource (with should not have a HTTP
	// method) with functions with the equal path. If we move this function in
	// the OntologyResource (the subresource) we may first have to load the
	// ontology before it can be deleted and PUT can not work, because the
	// resource is not available at that moment.
	@DELETE
	@Path("admin/{ontology}/")
	public void deleteOntology(@PathParam("ontology") String ontology) {
		logger.info("deleting " + ontology);
		oh.deleteOntology(ontology);
	}

	@PUT
	@Path("admin/{ontology}/")	
	public void putOntology(@PathParam("ontology") String ontology) {
		logger.info("loading ontology {} by request of user ", ontology);
		oh.addOntology(ontology);
	}

	/**
	 * Gets the OntologyResource registered for this ontology name and delegates
	 * further actions to this subresource.
	 * 
	 * @return The subresource for the ontology
	 */
	@Path("{ontology}/")
	public OntologyResource getOntologyBrowser(
			@PathParam("ontology") String ontology) {
		OntologyResource resource = getOntologyByName(ontology);
		if (resource == null) {
			logger.warn("ontology '{}' not loaded by this server", ontology);
			WebApplicationException ex = new WebApplicationException(404);
			throw ex;
		}
		return resource;
	}

	/**
	 * Calls an ontology function on and with the given ontology. The ontology
	 * named in the first part of the path is used to operate on. The second
	 * part of the path have to be "functions". The third part is the name of
	 * the class providing the function. All following parts of the path will be
	 * parameters for the class.
	 * 
	 * @param ontology
	 * @param fc
	 * @return
	 */
	@Path("{ontology}/functions/{fc}.*")
	public Object getOntologyFunction(@PathParam("ontology") String ontology,
			@PathParam("fc") String fc) {
		logger.info("calling function class {} with ontology {}", fc, ontology);
		OntologyFunction c = getFunctionClass(fc);
		if (c == null) {
			logger.error("could not get function class " + c);
			WebApplicationException ex = new WebApplicationException(404);
			throw ex;
		}
		c.setOntology(getOntologyByName(ontology).getOntology());
		return c;

	}

	/**
	 * Get the overview of the functions in a function class. For this case, the
	 * method {@link OntologyFunction#getRoot()} is called directly without
	 * setting the ontology in the function class.
	 * 
	 * @param fc
	 *            The function class to get the overview from.
	 * @return A short description of the available functions.
	 */
	@GET
	@Path("functions/{fc}")
	@Produces("text/html")
	public String functionClassOverview(@PathParam("fc") String fc) {
		OntologyFunction c = getFunctionClass(fc);
		String overview = c.getRoot();
		return overview;
	}

	/**
	 * Get the storage handler and delegates the request to it.
	 * 
	 * @return
	 */
	@Path("storage/")
	public Object handleStorage() {
		logger.debug("deligation to storage hand");
		return storageHandler;
	}

	// /////////////
	private OntologyResource getOntologyByName(String name) {
		if (!oh.containsOntology(name)) {
			logger.info("ontology '{}' not found", name);
			throw new WebApplicationException(404);
		}
		return oh.getOntology(name);
	}

	private OntologyFunction getFunctionClass(String name) {
		return oh.getFunctionClass(name);
	}

}
