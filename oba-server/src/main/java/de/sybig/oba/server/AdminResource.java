package de.sybig.oba.server;

import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminResource {
	private Logger logger = LoggerFactory.getLogger(AdminResource.class);
	private OntologyHandler oh = OntologyHandler.getInstance();

	// The following methods have to work on the resource "admin" instead of
	// "/". It is not allowed to mix subresource (with should not have a HTTP
	// method) with functions with the equal path. If we move this function in
	// the OntologyResource (the subresource) we may first have to load the
	// ontology before it can be deleted and PUT can not work, because the
	// resource is not available at that moment.

	@GET
	@Path("/version")
	@Produces("text/plain")
	public String getServerVersion() {
		return "1.3";
	}

	@GET
	@Path("/ontology")
	@Produces("text/plain")
	public String getOntologies() {
		StringBuilder out = new StringBuilder();
		Set<String> names = oh.getOntologyNames();
		for (String n : names) {
			out.append(n);
			out.append("\n");
		}
		return out.toString();
	}

	@DELETE
	@Path("admin/ontology/{ontology}/")
	public void deleteOntology(@PathParam("ontology") String ontology) {
		logger.info("deleting " + ontology);
		oh.deleteOntology(ontology);
	}

	@PUT
	@Path("admin/ontology{ontology}/")
	public void putOntology(@PathParam("ontology") String ontology) {
		logger.info("loading ontology {} by request of user ", ontology);
		oh.addOntology(ontology);
	}

}
