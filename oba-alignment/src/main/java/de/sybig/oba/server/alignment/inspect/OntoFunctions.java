package de.sybig.oba.server.alignment.inspect;

import de.sybig.oba.server.AbstractOntolgyResource;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.OntologyFunction;
import de.sybig.oba.server.alignment.AlignmentOntology;
import de.sybig.oba.server.alignment.Methods;
import java.util.ArrayList;
import java.util.Arrays;
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
        return "Functions to inspect the alignment ";
    }

    @GET
    @Path("allScores")
    @Produces({"text/plain", "text/html"})
    public List<Object[]> getAllScores() {
        int methodsLength = Methods.values().length;
        System.out.println("methods " + Arrays.asList(Methods.values()));
        List<Object[]> table = new ArrayList<>();
        Object[] header = new Object[2 + methodsLength+1];
        header[0] = "# Cls A";
        header[1] = "Cls B";
        for (int i = 0; i < methodsLength ; i++) {
            header[2 + i] = Methods.values()[i];
        }
        table.add(header);
        Map<ObaClass, Map<ObaClass, double[]>> scores = ((AlignmentOntology) ontology).getScores();
//
        for (ObaClass clsA : scores.keySet()) {
            Map<ObaClass, double[]> map = scores.get(clsA);
            Object[] row = new Object[2 + methodsLength +1];
            table.add(row);
            for (ObaClass clsB : map.keySet()) {

                row[0] = clsA;
                row[1] = clsB;
                for (int i = 0; i < methodsLength ; i++) {
                    row[2 + i] = map.get(clsB)[i];
                }
            }
        }
        return table;
    }

    @GET
    @Produces({"text/plain", "application/json", "text/html"})
    @Path("notMappedInA")
    public List<ObaClass> notMappedInA() {

        Set<ObaClass> mappedA = ((AlignmentOntology) ontology).getScores().keySet();
        List<ObaClass> allA = ((AlignmentOntology) ontology).getOntoA().getOntology().getClasses();
        List<ObaClass> out = allA.parallelStream().filter(cls -> !mappedA.contains(cls)).collect(Collectors.toList());

        for (ObaClass item : out.subList(735, 742)) {
            System.out.println("item " + item);
        }
        System.out.println("size " + out.size());
//        System.out.println(out.get(744));
//        return out.subList(41, 50);
        return out;
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @Override
    public void reset() {

    }
}
