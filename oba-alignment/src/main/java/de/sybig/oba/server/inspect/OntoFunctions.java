package de.sybig.oba.server.inspect;

import de.sybig.oba.server.AbstractOntolgyResource;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.OntologyFunction;
import de.sybig.oba.server.alignment.AlignmentOntology;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen
 */
public class OntoFunctions extends AbstractOntolgyResource implements
        OntologyFunction {

    @GET
    @Produces("text/html")
    @Override
    public String getRoot() {
        System.out.println("hi");
        return "Functions to inspect the alignment " + ontology;
    }

    @GET
    @Path("allScores")
    @Produces("text/plain")
    public String getAllScores() {
        Map<ObaClass, Map<ObaClass, double[]>> scores = ((AlignmentOntology) ontology).getScores();
        StringBuilder out = new StringBuilder();
        for (ObaClass clsA : scores.keySet()) {
            out.append(clsA + "\n");
            System.out.println(clsA);
        }
        return out.toString();
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @Override
    public void reset() {

    }
}
