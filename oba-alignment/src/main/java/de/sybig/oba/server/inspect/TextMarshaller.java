package de.sybig.oba.server.inspect;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.semanticweb.owlapi.model.OWLClass;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
@Provider
@Produces("text/plain")
public class TextMarshaller extends TableMarshaller {

    @Override
    public void writeTo(Object t, Class<?> type,
            Type type1, Annotation[] antns,
            MediaType mt, MultivaluedMap<String, Object> mm,
            OutputStream out) throws IOException, WebApplicationException {
        List<Object[]> table = (List<Object[]>) t;
        for (Object[] row : table) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length - 1; i++) {
                Object cell = row[i];
                sb.append(cell.toString()).append('\t');
                if (cell instanceof OWLClass) {
                    String label = getLabel(cell);
                    if (label != null) {
                        sb.append(label).append('\t');
                    }
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            out.write(sb.append('\n').toString().getBytes());
        }
    }

    
}
