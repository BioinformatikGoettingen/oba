package de.sybig.oba.server;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

public class OntologyFunctionsTest {

	private static OntologyFunctions testClass;
	private static ObaOntology obaOntology;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testClass = new OntologyFunctions();
		obaOntology = new ObaOntology();
		URL testOntology = obaOntology.getClass().getResource(
				"/testOntology.owl");
		obaOntology.setOwlURI(IRI.create(testOntology));
		obaOntology.init();
		testClass.setOntology(obaOntology);
	}

	@Test
	public void overviewDocumentation() {
		// checks if all methods are listed in the overview page
		String documentation = testClass.getRoot();

		Class<? extends OntologyFunctions> c = testClass.getClass();
		Method m[] = c.getDeclaredMethods();

		for (int i = 0; i < m.length; i++) {
			if (m[i] != null && !m[i].isAnnotationPresent(GET.class)) {
				continue;
			}
			if (m[i].getName().contains("catchAll")) {
				continue;
			}
			String method = m[i].getAnnotation(Path.class).value();
			method = method.replaceAll("\\{[a-zA-Z0-9]*:([a-zA-Z0-9]*)\\}",
					"$1");
			if (!documentation.contains(method)) {
				Assert.fail(method + " is not documented in the overview");
			}
		}
	}

	@Test
	public void shortestPathToRoot() {
		ObaClass clsA = obaOntology.getOntologyClass("classWithTwoParents",
				"http://sybig.de/cytomer/testOntology/");
		List<List<ObaClass>> shortestPathToRoot = testClass
				.getShortestPathsToRoot(clsA);
		assertEquals(1, shortestPathToRoot.size());
		assertEquals(3, shortestPathToRoot.get(0).size());
	}
}
