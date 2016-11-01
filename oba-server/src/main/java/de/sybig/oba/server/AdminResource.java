package de.sybig.oba.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The admin resource provides methods for adminisrative tasks, like stopping
 * the server or loading ontologies etc.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AdminResource {

    private static final Logger logger = LoggerFactory.getLogger(AdminResource.class);
    private final OntologyHandler oh = OntologyHandler.getInstance();

    // The following methods have to work on the resource "admin" instead of
    // "/". It is not allowed to mix subresource (with should not have a HTTP
    // method) with functions with the equal path. If we move this function in
    // the OntologyResource (the subresource) we may first have to load the
    // ontology before it can be deleted and PUT can not work, because the
    // resource is not available at that moment.
    /**
     * Get the version of the OBA server. This can be used by clients to check
     * compatibility.
     *
     * @return The vesion of the server
     */
    @GET
    @Path("/version")
    @Produces("text/plain")
    public String getServerVersion() {
        return "1.3\n";
    }

    /**
     * Lists all registered ontologies.
     *
     * @return The ontologies of the server.
     */
    @GET
    @Path("/ontology")
    @Produces("text/plain")
    public String getOntologies() {
        StringBuilder out = new StringBuilder();
        Set<String> names = oh.getOntologyNames();
        for (String n : names) {
            out.append(n).append('\n');
        }
        return out.toString();
    }

    /**
     * Remove the ontology from the server. The ontology functions should be
     * resetted to give the ontology free.
     *
     * @param ontology The ontoloyg to remove from the server.
     */
    @DELETE
    @Path("/ontology/{ontology}/")
    public void deleteOntology(@PathParam("ontology") String ontology) {
        logger.info("deleting " + ontology);
        oh.deleteOntology(ontology);
    }

    /**
     * Loads an ontology from the ontolgy directory. The parameter is the name
     * of the corresponding property file without the extension.
     *
     * @param ontology The name of the property file.
     */
    @PUT
    @Path("/ontology/{ontology}/")
    public void putOntology(@PathParam("ontology") String ontology) {
        logger.info("loading ontology {} by request of user ", ontology);
        oh.addOntology(ontology);
    }

    /**
     * Get the properties used to load the ontology. Typically the properties
     * are read from a file in the ontology directory.
     *
     * @param ontology The ontology to get the properties for.
     * @return The properties of the ontology
     */
    @GET
    @Path("/ontology/{ontology}/properties")
    public String getProperties(@PathParam("ontology") String ontology) {
        Properties props = oh.getOntologyProperties(ontology);
        if (props == null) {
            throw new WebApplicationException(500);
        }
        return propertyToString(props);
    }

    @POST
    @Path("/ontology/")
    public void createOntology(InputStream is) {
        String[] kv;
        Properties props = new Properties();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                kv = line.split("=");
                props.put(kv[0].trim(), kv[1].trim());
            }
            if (!(props.containsKey("identifier") && props.containsKey("file"))) {
                throw new WebApplicationException(500);
            }
            OntologyResource existing = oh.getOntology(props.getProperty("identifier"));
            if (existing != null) {
                throw new WebApplicationException(500);
            }
            oh.addOntology(props);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AdminResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(500);
        }
    }

    /**
     * Resets a function class, all cached values should be resetted and the
     * ontology refetced from the server. If the name function class does not
     * exist HTTP status code 400 is returned
     *
     * @param function The function class to reset
     * @return "ok" if no error occured.
     */
    @GET
    @Path("/reset/{function}")
    public String resetFunctionClass(@PathParam("function") String function) {
        try {
            oh.resetFunctionClass(function);
            return "ok";
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.Status.BAD_REQUEST);
        }

    }

    /**
     * Terminates the OBA server.
     *
     * @return "The string "Bye"
     */
    @GET
    @Path("/stop")
    public String stop() {
        logger.info("stopping oba server");
        RestServer.shutdown();
        return "Bye";
    }

    private String propertyToString(Properties props) {
        StringBuilder sb = new StringBuilder();
        for (Object key : props.keySet()) {
            sb.append(key)
                    .append('=')
                    .append(props.get(key))
                    .append('\n');
        }
        return sb.toString();
    }
}
