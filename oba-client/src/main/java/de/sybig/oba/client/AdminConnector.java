/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

import com.sun.jersey.api.client.WebResource;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import sun.net.www.content.audio.x_aiff;

/**
 *
 * @author jdo
 */
public class AdminConnector extends GenericConnector {

    public AdminConnector() {
        super("admin");
    }

    public String getVersion() {
        String path = "/admin/version";
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        String response = webResource.accept(MediaType.TEXT_PLAIN).get(String.class);
        return response;
    }

    public Properties getProperties(String ontology) {
        String path = String.format("/admin/ontology/%s/properties", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        String response = webResource.accept(MediaType.TEXT_PLAIN).get(String.class);
        String[] lines = response.split("\n");
        Properties ontoProps = new Properties();
        for (String l : lines) {
            String[] kv = l.split("=");
            ontoProps.put(kv[0], kv[1]);
        }
        return ontoProps;
    }

    public void addOntologie(Properties ontoProps) {
        String path = String.format("/admin/ontology/");
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        webResource.post(propertyToString(ontoProps));
    }

    public void loadOntology(String ontology) {
        String path = String.format("/admin/ontology/%s", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        webResource.put();
    }

    public void removeOntology(String ontology) {
        String path = String.format("/admin/ontology/%s", ontology);
        WebResource webResource = getWebResource();
        webResource = webResource.path(path);
        webResource.delete();
    }

    public static void main(String[] args) {
        AdminConnector ac = new AdminConnector();
        Properties p = new Properties();
        p.put("identifier", "foo");
        p.put("file", "/srv/ontologies/ibeetle_4-short.obo");
        ac.addOntologie(p);
        System.out.println(ac.getProperties("foo").keySet());

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
