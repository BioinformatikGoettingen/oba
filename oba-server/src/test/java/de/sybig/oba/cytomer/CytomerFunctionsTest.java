package de.sybig.oba.cytomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaOntology;

public class CytomerFunctionsTest {
	private static ObaOntology obaOntology;
	private static CytomerFunctions testClass;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		obaOntology = new ObaOntology();
		URL testOntology = obaOntology.getClass().getResource(
				"/testOntology.owl");
		obaOntology.setOwlURI(IRI.create(testOntology));
		obaOntology.setProperties(getCytomerProps());
		obaOntology.init();
		testClass = new CytomerFunctions();
		testClass.setOntology(obaOntology);
	}

	private static Properties getCytomerProps() {
		InputStream inStream = obaOntology.getClass().getResourceAsStream(
				"/cytomer.properties");
		Properties props = new Properties();
		try {
			props.load(inStream);
			return props;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testGetOrgans() {
		assertNotNull(testClass.getOrganList());
	}

	@Test
	public void organListDoesNotContainOrgan() {
		assertFalse(testClass.getOrganList().contains(testClass.getOrganCls()));
	}

	@Test
	public void organListDoesNotContainAbstractClass() {
		OWLClass abstractClass = obaOntology.getOntologyClass(
				"abstractOrganGroup", "http://sybig.de/cytomer/testOntology/");
		assertFalse(testClass.getOrganList().contains(abstractClass));
	}

	@Test
	public void organListContainsClassWithoutAbstractAnnotation() {
		OWLClass abstractClass = obaOntology.getOntologyClass("organA",
				"http://sybig.de/cytomer/testOntology/");
		assertTrue(testClass.getOrganList().contains(abstractClass));
	}

	@Test
	public void organListContainsClassWithAbstractAnnotation() {
		OWLClass abstractClass = obaOntology.getOntologyClass("organB",
				"http://sybig.de/cytomer/testOntology/");
		assertTrue(testClass.getOrganList().contains(abstractClass));
	}

	@Test
	public void organListContainsGroupWithoutAbstractAnnotation() {
		OWLClass abstractClass = obaOntology.getOntologyClass("organGroup",
				"http://sybig.de/cytomer/testOntology/");
		assertTrue(testClass.getOrganList().contains(abstractClass));
	}

	@Test
	public void organListDoesNotContainClassBelowOfGroup() {
		OWLClass abstractClass = obaOntology.getOntologyClass("organC",
				"http://sybig.de/cytomer/testOntology/");
		assertFalse(testClass.getOrganList().contains(abstractClass));
	}

	@Test
	public void organsOf() {
		assertNotNull(testClass.getOrgansFor("organC",
				"http://sybig.de/cytomer/testOntology/"));
		for (ObaClass organ : testClass.getOrgansFor("organC",
				"http://sybig.de/cytomer/testOntology/")) {
			assertTrue(contains(testClass.getOrganList(), organ));
		}

	}

	// /////////////////////////////////////////
	@Test
	public void organsOfcellA() {
		// checks if organs of cellA are detected.
		assertNotNull(testClass.getOrgansFor("cellA",
				"http://sybig.de/cytomer/testOntology/"));
		assertEquals(
				3,
				testClass.getOrgansFor("cellA",
						"http://sybig.de/cytomer/testOntology/").size());
		for (ObaClass organ : testClass.getOrgansFor("cellA",
				"http://sybig.de/cytomer/testOntology/")) {
			assertTrue(contains(testClass.getOrganList(), organ));
		}
	}

	@Test
	public void organsOfNoneOrganPart() {
		// checks if organs of cellA are detected.
		assertNotNull(testClass.getOrgansFor("SecondSubClass",
				"http://sybig.de/cytomer/testOntology/"));
		assertEquals(
				0,
				testClass.getOrgansFor("SecondSubClass",
						"http://sybig.de/cytomer/testOntology/").size());
	}

	@Test(expected = WebApplicationException.class)
	public void organsOfInvalidStartClass() {
		testClass.getOrgansFor("nonExisting",
				"http://sybig.de/cytomer/testOntology/");
	}

	// /////////////////////////////////////////
	@Test
	public void overviewDocumentation() {
		// checks if all methods are listed in the overview page
		String documentation = testClass.getRoot();

		Class<? extends CytomerFunctions> c = testClass.getClass();
		Method m[] = c.getDeclaredMethods();

		for (int i = 0; i < m.length; i++) {
			if (m[i] != null && !m[i].isAnnotationPresent(GET.class)) {
				continue;
			}
			String method = m[i].getAnnotation(Path.class).value();
			if (!documentation.contains(method)) {
				Assert.fail(method + " is not documented in the overview");
			}
		}

	}

	private boolean contains(Set<ObaClass> list, ObaClass testCl) {
		for (OWLClass item : list) {
			if (testCl.getIRI().equals(item.getIRI())) {
				return true;
			}
		}
		return false;
	}
}
