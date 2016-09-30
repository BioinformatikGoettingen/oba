package de.sybig.oba.client;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.net.ConnectException;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The admin connector is mainly used to control the OBA server, like adding or
 * removing ontologies or stopping the sever.
 *
 * @author juergen.doenitz@bioinf.med.uni-goetingen.de
 */
public class AdminConnector extends GenericConnector {

    private static final Logger logger = LoggerFactory.getLogger(AdminConnector.class);

    /**
     * Create a new admin connector. It is not possible to use an ontology with
     * this connector.
     */
    public AdminConnector() {
        super("admin");
    }

    /**
     * Gets the version of the OBA service.
     *
     * @return The version of the server.
     * @throws ConnectException Thrown if the server is not reached.
     */
    public String getVersion() throws ConnectException {
        String path = "/admin/version";
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            String response = webResource.accept(MediaType.TEXT_PLAIN).get(String.class);
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
     * Get the properties used to load the specified ontology.
     *
     * @param ontology The ontology to get the properties for.
     * @return The properties of the ontology.
     * @throws ConnectException Thrown if the OBA server is not reached.
     */
    public Properties getProperties(String ontology) throws ConnectException {
        String path = String.format("/admin/ontology/%s/properties", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            String response = webResource.accept(MediaType.TEXT_PLAIN).get(String.class);
            String[] lines = response.split("\n");
            Properties ontoProps = new Properties();
            for (String l : lines) {
                String[] kv = l.split("=");
                ontoProps.put(kv[0], kv[1]);
            }
            return ontoProps;
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

    public void addOntologie(Properties ontoProps) throws ConnectException {
        String path = String.format("/admin/ontology/");
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            webResource.post(propertyToString(ontoProps));
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        }
    }

    public void loadOntology(String ontology) throws ConnectException {
        String path = String.format("/admin/ontology/%s", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            webResource.put();
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        }
    }

    public void removeOntology(String ontology) throws ConnectException {
        String path = String.format("/admin/ontology/%s", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        try {
            webResource.delete();
        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error("error while communicating the OBA server", ex);
        }
    }

    public void resetFunctionClass(String fc) {
        String path = String.format("/admin/reset/%s", fc);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        String response = webResource.get(String.class);
        if (!response.equalsIgnoreCase("ok")) {
            //TODO
            System.err.println("could not reset function class " + fc);
        }
    }

    public static void main(String[] args) {
        try {
            AdminConnector ac = new AdminConnector();
            Properties p = new Properties();
            p.put("identifier", "foo");
            p.put("file", "/srv/ontologies/ibeetle_4-short.obo");
            ac.addOntologie(p);
            System.out.println(ac.getProperties("foo").keySet());
        } catch (ConnectException ex) {
            logger.error("Error ", ex);
        }

    }

    private String propertyToString(Properties props) {
        StringBuffer sb = new StringBuffer();
        for (Object key : props.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(props.get(key));
            sb.append("\n");
        }
        return sb.toString();
    }
}
