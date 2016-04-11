/*
 * Created on Apr 11, 2016
 *
 */
package de.sybig.oba.server.go;

import de.sybig.oba.server.OntologyFunction;
import de.sybig.oba.server.OntologyFunctions;
import javax.ws.rs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoFunctions extends OntologyFunctions implements
        OntologyFunction {

    private Logger log = LoggerFactory.getLogger(GoFunctions.class);

    /**
     * A class providing ontology functions specific for Cytomer.
     *
     */
    public GoFunctions() {
        super();
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    /**
     * Gets a short documentation of the implemented functions in html.
     */
    @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
        StringBuffer out = new StringBuffer();

        return out.toString();
    }
}
