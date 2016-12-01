package de.sybig.oba.server.inspect;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.OntologyHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.semanticweb.owlapi.model.OWLClass;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
@Provider
@Produces("text/plain")
public class TextMarshaller implements MessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
       Class<?>  currentType =type ;
        while (currentType != null) {

            Class[] interfaces = currentType.getInterfaces();
            for (Class c : interfaces) {
                if (c.equals(Set.class) || c.equals(List.class)) {

                    if (type1 instanceof ParameterizedType) {
                        ParameterizedType t = (ParameterizedType) type1;
                        if (t.getActualTypeArguments()[0].equals(Object[].class)) {
                            return true;
                        }
                    }
                }
            }
            currentType = currentType.getSuperclass();
        }
        return false;
    }

    @Override
    public long getSize(Object t, Class<?> type, Type type1, Annotation[] antns,
            MediaType mt) {
        return -1;
    }

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
//                    OntologyHelper.
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            out.write(sb.append('\n').toString().getBytes());
        }
    }

}
