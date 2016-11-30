package de.sybig.oba.server.inspect;

import de.sybig.oba.server.AbstractOntolgyResource;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.OntologyFunction;
import de.sybig.oba.server.alignment.AlignmentOntology;
import de.sybig.oba.server.alignment.Methods;
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
        return "Functions to inspect the alignment ";
    }

    @GET
    @Path("allScores")
    @Produces("text/plain")
    public List<Object[]> getAllScores() {
        int methodsLength = Methods.values().length;
        List<Object[]> table = new ArrayList<>();
        Map<ObaClass, Map<ObaClass, double[]>> scores = ((AlignmentOntology) ontology).getScores();
        List<ObaClass> out = new ArrayList<>();
//
        for (ObaClass clsA : scores.keySet()) {
            Map<ObaClass, double[]> map = scores.get(clsA);
            Object[] row = new Object[2 + methodsLength];
            table.add(row);
            for (ObaClass clsB : map.keySet()) {

                row[0] = clsA;
                row[1] = clsB;
                for (int i = 0; i < methodsLength -1 ; i++) {
                   row[2 + i] = map.get(clsB)[i];
                }
            }
        }
        return table;
    }

//    @GET
//    @Produces("text/plain application/json text/html")
//    @Path("notMappedInA")
//    public List<ObaClass> notMappedInA() {
//
//        Set<ObaClass> mappedA = ((AlignmentOntology) ontology).getScores().keySet();
//        List<ObaClass> allA = ((AlignmentOntology) ontology).getOntoA().getOntology().getClasses();
//        List<ObaClass> out = allA.parallelStream().filter(cls -> !mappedA.contains(cls)).collect(Collectors.toList());
////        out.forEach(System.out::println);
//
//        return out.subList(0, 5);
//    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @Override
    public void reset() {

    }
}
