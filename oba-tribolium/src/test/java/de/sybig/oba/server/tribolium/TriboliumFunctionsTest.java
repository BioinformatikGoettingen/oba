package de.sybig.oba.server.tribolium;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

/**
 * Test class for TriboliumFunctions.
 *
 * @author juegen.doenitz@ibeetle-base.uni-goettingen.de
 */
public class TriboliumFunctionsTest {

    private static ObaOntology obaOntology;
    private static TriboliumFunctions testClass;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        obaOntology = new ObaOntology();
        URL testOntology = new TriboliumFunctions().getClass().getResource(
                "/tronTest.obo");
        obaOntology.setOwlURI(IRI.create(testOntology));

        obaOntology.init();
        testClass = new TriboliumFunctions();
        testClass.loadPropertiesFromJar("/triboliumTest.properties");
        testClass.setOntology(obaOntology);
    }

    //getRoot
    @Test
    @Ignore
    public void testIfAllMethodsAreDocumented() {
        String documentation = testClass.getRoot();
        for (Method method : TriboliumFunctions.class.getDeclaredMethods()) {
            if (method.getAnnotation(javax.ws.rs.GET.class) == null) {
                continue;
            }
            if ("getRoot".equals(method.getName())){
                continue;
            }
            String path = method.getAnnotation(javax.ws.rs.Path.class).value();
            assert(documentation.contains(String.format("<dt>%s</dt>", path)));
        }
    }

    //devStages
    @Test  
    public void testGetAllDevStages(){
        Set<ObaClass> stages = testClass.getDevStages();
        assertEquals(2, stages.size());
    }

}
