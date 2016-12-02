package de.sybig.oba.server.alignment.inspect;

import de.sybig.oba.server.OntologyHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 *
 * @author juergen.doenitz@biologie.uni-goettingen.de
 */
public abstract class TableMarshaller implements MessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        Class<?> currentType = type;
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

    protected String getLabel(Object cell) {
        return OntologyHelper.getAnnotation((OWLEntity) cell,
                OntologyHelper.getOntology((OWLEntity) cell),
                "label");

    }

    @Override
    public long getSize(Object t, Class<?> type, Type type1, Annotation[] antns,
            MediaType mt) {
        return -1;
    }
}
