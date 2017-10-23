package de.sybig.oba.server.tribolium;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;
import de.sybig.oba.server.OntologyHandler;
import de.sybig.oba.server.OntologyResource;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
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

    private Set<String> genericExamples;
    private Set<String> concreteExamples;
    private Set<String> mixedExamples;

    /**
     * Load the test ontology and init the class with the functions.
     *
     * @throws Exception Thrown if loading the test ontology fails
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        obaOntology = new ObaOntology();
        URL testOntology = new TriboliumFunctions().getClass().getResource(
                "/tronTest.obo");
        obaOntology.setOwlURI(IRI.create(testOntology));
        obaOntology.setProperties(getOntologyProperties());
        obaOntology.init();
        OntologyResource or = new OntologyResource();
        or.setOntology(obaOntology);
        OntologyHandler.getInstance().addOntology("triboliumTest", or);
        testClass = new TriboliumFunctions();
        testClass.loadPropertiesFromJar("/triboliumTest.properties");
        testClass.setOntology(obaOntology);
    }

    //---- getRoot
    /**
     * Test if all REST endpoints are documented. For all methods which are
     * annotated with GET are checked, if their path is in the definition list.
     */
    @Test
    @Ignore
    public void testIfAllMethodsAreDocumented() {
        String documentation = testClass.getRoot();
        for (Method method : TriboliumFunctions.class.getDeclaredMethods()) {
            if (method.getAnnotation(javax.ws.rs.GET.class) == null) {
                continue;
            }
            if ("getRoot".equals(method.getName())) {
                continue;
            }
            String path = method.getAnnotation(javax.ws.rs.Path.class).value();
            if (!documentation.contains(String.format("<dt>%s</dt>", path))) {
                fail(path + " is not documented in the overview");
            }
        }
    }

    //------ devStages
    /**
     * Test the developmental stages. Also sub stages like L1 below of larva
     * should be returned.
     */
    @Test
    public void testGetAllDevStages() {
        Set<ObaClass> stages = testClass.getDevStages();
        assertEquals("There should be four developemental stages in the ontology (larva, L1, pupa",
                4, stages.size());
    }
    // --- getConcrete
    /**
     * List of concrete classes should not contain the <code>null</code>
     * element.
     */
    @Test
    public void concreteClassesWithoutNull() {
        Set<ObaClass> concreteClasses = testClass.getConcreteClasses();
        assertFalse("The set of concrete classes should not contain null", concreteClasses.contains(null));
    }

    @Test
    public void getConcreteClassesTest() {
        Set<ObaClass> concreteClasses = testClass.getConcreteClasses();
        Set<String> labels = getLabels(concreteClasses);
        assertTrue("getConcreteClasses should contain all concrete classes", labels.containsAll(getConcreteExamples()));
        assertFalse("getConcreteExamples should not contain any generic classs", labels.removeAll(getGenericExamples()));
        assertTrue("getConcreteExamples contains also all mixed classes", labels.containsAll(getMixedExamples()));
    }

    // ---- getGeneric
    /**
     * List of generic classes should not contain the <code>null</code> element.
     */
    @Test
    public void genericClassesWithoutNull() {
        Set<ObaClass> genericCls = testClass.getGenericClasses();
        assertFalse("The set of generic classes should not contain null", genericCls.contains(null));

    }

    @Test
    public void getGenericClassesTest() {
        Set<ObaClass> genericCls = testClass.getGenericClasses();
        Set<String> labels = getLabels(genericCls);
        assertTrue("getGenericClasses should contain all generic classes.", labels.containsAll(getGenericExamples()));
        assertFalse("getGenericClasses should not contain any concrete classes.", labels.removeAll(getConcreteExamples()));
        assertFalse("getGenericClasses should not contain any mixed classes.", labels.removeAll(getMixedExamples()));
    }
    // ---- getMixedClasses
    /**
     * List of mixed classes should not contain the <code>null</code> element.
     */
    @Test
    public void mixedClassesWithoutNull() {
        Set<ObaClass> mixedClasses = testClass.getMixedClasses();
        assertFalse("The set of mixed classes should not contain null", mixedClasses.contains(null));
    }

    /**
     * Mixed classes contains also some concrete classes, but no generic ones.
     */
    @Test
    public void getMixedClassesTest() {
        Set<ObaClass> mixedClasses = testClass.getMixedClasses();
        Set<String> labels = getLabels(mixedClasses);
        assertTrue("getMixedClasses should contain all mixed classes", labels.containsAll(getMixedExamples()));
        assertTrue("The concrete classes should also contain the mixed classes", getConcreteExamples().containsAll(labels));
        assertFalse("getMixedExamples should not contain any generic classes", labels.removeAll(getGenericExamples()));
    }

    // --- search
    /**
     * SearchInGeneric should find generic classes.
     */
    @Test
    public void searchGenericInGenericTest() {
        List<ObaClass> hits = testClass.searchInGeneric("leg");
        assertEquals("Generic class should be found with searchInGeneric", 1, hits.size());
    }

    /**
     * SearchInGeneric should not find concrete classes.
     */
    @Test
    public void searchConcreteInGenericTest() {
        List<ObaClass> hits = testClass.searchInGeneric("pupal_leg");
        assertEquals("Concrete class should not not be found with searchInGeneric", 0, hits.size());
    }

    /**
     * SearchInGeneric should not find mixed classes.  
     */
    @Test
    public void searchMixedInGenericTest() {
        List<ObaClass> hits = testClass.searchInGeneric("femoral_brush");
        assertEquals("Mixed class should be not found with searchInGeneric", 0, hits.size());
    }

     /**
     * SearchInGenericAndMixed should find generic classes.
     */
    @Test
    public void searchGenericInGenericAndMixedTest() {
        List<ObaClass> hits = testClass.searchInGenericAndMixed("leg");
        assertEquals("Generic class should be found with searchInGenericAndMixed", 1, hits.size());
    }

    /**
     * SearchInGenericAndMixed should not find concrete classes.
     */
    @Test
    public void searchConcreteInGenericAndMixedTest() {
        List<ObaClass> hits = testClass.searchInGenericAndMixed("pupal_leg");
        assertEquals("Concrete class should not be found with searchInGenericAndMixed", 0, hits.size());
    }

    /**
     * SearchInGenericAndMixed should not find mixed classes.
     */
    @Test
    public void searchMixedInGenericTAndMixedTest() {
        List<ObaClass> hits = testClass.searchInGenericAndMixed("femoral_brush");
        assertEquals("Mixed class should be found with searchInGenericAndMixed", 1, hits.size());
    }

     /**
     * SearchInConcrete should not find generic classes.
     */
    @Test
    public void searchGenericInConcreteTest() {
        List<ObaClass> hits = testClass.searchInConcrete("leg");
        assertEquals("Generic class should not be found with searchInConcrete", 0, hits.size());
    }

    /**
     * SearchInConcrete should  find concrete classes.
     */
    @Test
    public void searchConcreteInConcreteTest() {
        List<ObaClass> hits = testClass.searchInConcrete("pupal_leg");
        assertEquals("Concrete class should  be found with searchInConcrete", 1, hits.size());
    }

    /**
     * SearchInConcrete should not find mixed classes.
     */
    @Test
    @Ignore
    public void searchMixedInConcreteTest() {
        List<ObaClass> hits = testClass.searchInConcrete("femoral_brush");
               System.out.println(" --- " + getLabels(hits));
        assertEquals("Mixed class should be found with searchInConcrete", 0, hits.size());
    }
    /// --- helper functions

    /**
     * Generates a simple properties object for the test ontology.
     * @return a property config for the test ontology.
     */
    private static Properties getOntologyProperties() {
        Properties p = new Properties();
        p.setProperty("indexAnnotations", "label");
        return p;
    }

    /**
     * Get a set of IDs for a list of ontology classes
     *
     * @TODO change to labels for better reading
     *
     * @param classes The set of classes to get the IDs for.
     * @return A set of IDs of the input classes.
     */
    private Set<String> getLabels(Collection<ObaClass> classes) {
        Set<String> labels = new HashSet<String>();
        for (ObaClass cls : classes) {
            labels.add(cls.toString());
        }
        return labels;
    }

    /**
     * Get a list of IDs of generic classes.
     *
     * @return Examples for generic classes.
     */
    private Set<String> getGenericExamples() {
        if (genericExamples == null) {
            genericExamples = new HashSet<String>();
//            genericExamples.add("anatomical_entity");
//            genericExamples.add("appendage");
//            genericExamples.add("organism");
//            genericExamples.add("leg");
            genericExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000001>");
            genericExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000000>");
            genericExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000005>");
            genericExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000006>");
        }
        return genericExamples;
    }

    /**
     * Get a list of IDs for concrete classes.
     *
     * @return Examples for concrete classes.
     */
    private Set<String> getConcreteExamples() {
        if (concreteExamples == null) {
            concreteExamples = new HashSet<String>();
//            concreteExamples.add("larva");
//            concreteExamples.add("L1");
//            concreteExamples.add("pupa");
//            concreteExamples.add("pupal_leg");
            concreteExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000002>");
            concreteExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000003>");
            concreteExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000007>");
            concreteExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000007>");
            concreteExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000008>");
        }
        return concreteExamples;
    }

    /**
     * Get a list of IDs of mixed classes.
     *
     * @return Examples for mixed classes.
     */
    private Set<String> getMixedExamples() {
        if (mixedExamples == null) {
            mixedExamples = new HashSet<String>();
            mixedExamples.add("<http://purl.org/obo/owlapi/tribolium.anatomy.test#TrOnT_0000008>");
        }
        return mixedExamples;
    }
}
