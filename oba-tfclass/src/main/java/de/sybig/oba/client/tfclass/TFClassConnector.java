package de.sybig.oba.client.tfclass;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import de.sybig.oba.client.OboClassList;
import de.sybig.oba.client.OboConnector;
import de.sybig.oba.server.JsonCls;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class TFClassConnector extends OboConnector {

    protected static final String SUB_RESOURCE = "functions/tfclass";
    private static final Logger logger = LoggerFactory.getLogger(TFClassConnector.class);
    private static final String ERR_MSG = "error while communicating the OBA server";

    public TFClassConnector() {
        super("tfclass");
    }

    public TFClassConnector(String ontology) {
        super(ontology);
    }

    public OboClassList getSpeciesDownstream(JsonCls cls) throws ConnectException {
        String path = String.format("%s/%s/speciesDownstream/%s", getOntology(),
                SUB_RESOURCE, cls.getName());

        WebResource webResource = getWebResource().path(path);
        OboClassList species = null;
        try {
            webResource = webResource.queryParam("ns", cls.getNamespace());
            species = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            species.setConnector(this);

        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error(ERR_MSG, ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null
                    && ex.getResponse().getClientResponseStatus() != null
                    && ex.getResponse().getClientResponseStatus().getStatusCode() == 40) {
                return null;
            }
            logger.error(ERR_MSG, ex);
        }
        return species;
    }
     public OboClassList getGeneraDownstream(JsonCls cls) throws ConnectException {
        String path = String.format("%s/%s/generaDownstream/%s", getOntology(),
                SUB_RESOURCE, cls.getName());

        WebResource webResource = getWebResource().path(path);
        OboClassList genera = null;
        try {
            webResource = webResource.queryParam("ns", cls.getNamespace());
            genera = (OboClassList) getResponse(
                    webResource, OboClassList.class);
            genera.setConnector(this);

        } catch (ClientHandlerException ex) {
            if (ex.getCause() instanceof ConnectException) {
                throw (ConnectException) ex.getCause();
            }
            logger.error(ERR_MSG, ex);
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse() != null
                    && ex.getResponse().getClientResponseStatus() != null
                    && ex.getResponse().getClientResponseStatus().getStatusCode() == 40) {
                return null;
            }
            logger.error(ERR_MSG, ex);
        }
        return genera;
    }
}
