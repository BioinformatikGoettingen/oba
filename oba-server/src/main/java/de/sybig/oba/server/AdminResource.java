package de.sybig.oba.server;

import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
        return "1.2.1\n";
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
    @Path("/ontology/{ontology}/")
    public void deleteOntology(@PathParam("ontology") String ontology) {
        logger.info("deleting " + ontology);
        oh.deleteOntology(ontology);
    }

    @PUT
    @Path("/ontology/{ontology}/")
    public void putOntology(@PathParam("ontology") String ontology) {
        logger.info("loading ontology {} by request of user ", ontology);
        oh.addOntology(ontology);
    }

    @GET
    @Path("/ontology/{ontology}/properties")
    public String getProperties(@PathParam("ontology") String ontology) {
        Properties props = oh.getOntologyProperties(ontology);
        if (props == null){
            throw  new WebApplicationException(500);
        }
       return propertyToString(props);
    }

    @POST
    @Path("/ontology/")
    public void createOntology(InputStream is){
        String[] kv;
        Properties props = new Properties();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                kv = line.split("=");
                props.put(kv[0].trim(), kv[1].trim());
            }
            if (!(props.containsKey("identifier") && props.containsKey("file"))){
                throw new WebApplicationException(500);
            }
            OntologyResource existing = oh.getOntology(props.getProperty("identifier"));
            if (existing != null){
                throw new WebApplicationException(500);
            }
            oh.addOntology(props);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AdminResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("/stop")
    public String stop() {
        logger.info("stopping oba server");
        System.exit(0);  
    return "";
}
    private String propertyToString(Properties props){
         StringBuffer sb = new StringBuffer();
        for (Object key :  props.keySet()){
            sb.append(key);
            sb.append("=");
            sb.append(props.get(key));
            sb.append("\n");
        }
        return sb.toString();
    }
}
