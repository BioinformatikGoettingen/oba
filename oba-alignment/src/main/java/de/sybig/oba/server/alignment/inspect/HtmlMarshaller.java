package de.sybig.oba.server.alignment.inspect;

import de.sybig.oba.server.OntologyHandler;
import de.sybig.oba.server.OntologyHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author juergen.doenitz@biologie.uni-goettingen.de
 */
@Provider
@Produces("text/html")
public class HtmlMarshaller extends TableMarshaller {

    private Map<OWLOntology, String> ontologyIdentifier = new HashMap<>();

    @Override
    public void writeTo(Object t, Class<?> type,
            Type type1, Annotation[] antns,
            MediaType mt, MultivaluedMap<String, Object> mm,
            OutputStream out) throws IOException, WebApplicationException {
        List<Object[]> table = (List<Object[]>) t;

        out.write("<html><head><title>OBA alignment - all scores</title><head><body><table>".getBytes());

        for (Object[] row : table) {
            StringBuilder sb = new StringBuilder("<tr>");
            for (int i = 0; i < row.length - 1; i++) {
                Object cell = row[i];

                if (cell instanceof OWLClass) {
                    OWLClass cls = (OWLClass) cell;
                    String label = getLabel(cell);
                    if (label != null) {
                        String ontology = getOntologyIdentifier(cls);                   
                        sb.append("<td><a href=\"../../../").append(ontology).append("/cls/").append(cls.getIRI().getFragment()).append("\">")
                                .append(label).append("</a></td>");
                    }
                } else {
                    sb.append("<td>").append(cell.toString()).append("</td>");
                }
            }

            out.write(sb.append("</tr>\n").toString().getBytes());
        }
    }

    private String getOntologyIdentifier(OWLClass cls) {
        OWLOntology ontology = OntologyHelper.getOntology(cls);
        if (!ontologyIdentifier.containsKey(ontology)) {
            ontologyIdentifier.put(ontology,
                    OntologyHandler.getInstance().getNameOfOntology(ontology));
        }
        return ontologyIdentifier.get(ontology);
    }
}
