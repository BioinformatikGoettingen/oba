package de.sybig.oba.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class ObaOntologyTest {
	static ObaOntology testClass;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		testClass = new ObaOntology();
		URL testOntology = testClass.getClass()
				.getResource("/testOntology.owl");
		testClass.setOwlURI(IRI.create(testOntology));
		testClass.init();
	}

	// ////// init
	@Test
	public void test_type() throws Exception {
		assertNotNull(ObaOntology.class);
	}

	@Test(expected = OWLOntologyCreationException.class)
	public void testInitNull() throws OWLOntologyCreationException {
		testClass.setOwlURI(null);
		testClass.init();
	}

	@Test(expected = OWLOntologyCreationException.class)
	public void testInitNonExistingFile() throws OWLOntologyCreationException {
		ObaOntology newTestClass = new ObaOntology();
		newTestClass.setOwlURI(IRI.create("file:///no"));
		newTestClass.init();
	}

	// //// getOntology()
	@Test
	public void testGetOntology() {
		assertNotNull(testClass.getOntology());
	}

	// ////// getRoot()
	@Test
	public void testGetRoot() {
		assertTrue(testClass.getRoot().isOWLThing());
		assertFalse(testClass.getRoot().isOWLNothing());
		assertTrue(testClass.getRoot().isTopEntity());
	}

	// //// getOntologyClass()
	@Test
	public void testGetOntologyClass() {
		// assertNotNull(testClass.getOntologyClass("cellA",
		// "http://sybig.de/cytomer/testOntology/"));
		assertNotNull(testClass.getOntologyClass("organ",
				"http://sybig.de/cytomer/testOntology/"));
		assertNotNull(testClass.getOntologyClass("organ",
				"http://sybig.de/cytomer/testOntology/#"));
		assertNull(testClass.getOntologyClass("no",
				"http://sybig.de/cytomer/testOntology/#"));
		assertNull(testClass.getOntologyClass("",
				"http://sybig.de/cytomer/testOntology/#"));
		assertNull(testClass.getOntologyClass(null,
				"http://sybig.de/cytomer/testOntology/#"));
		assertNotNull(testClass.getOntologyClass("organ", null));

		assertNull(testClass.getOntologyClass("otherNamespace", null));
		assertNotNull(testClass.getOntologyClass("otherNamespace",
				"http://sybig.de/cytomer/testOntology/other/"));
	}

	@Test
	public void countParentsOfRoot() {
		OWLClass root = testClass.getRoot();
		// assertEquals(2,
		// (root.getSubClasses(testClass.getOntology()).size() + testClass
		// .getOrphanChildren().size()));
		assertEquals(3, (root.getSubClasses(testClass.getOntology()).size()));
	}

	// @Test
	// public void testGetOrphanChildren() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testGetOntologyClass() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testSearchCls() {
	// testClass.searchCls(pattern)
	// fail("Not yet implemented");
	// }

}
