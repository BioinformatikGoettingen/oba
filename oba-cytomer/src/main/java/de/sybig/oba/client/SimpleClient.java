/*
 * Created on Nov 24, 2009
 *
 */
package de.sybig.oba.client;

import java.util.Set;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonObjectProperty;
import de.sybig.oba.server.JsonObjectPropertyExpression;
import de.sybig.oba.server.JsonPropertyList;

public class SimpleClient {

	public static void main(String[] args) {

		SimpleClient client = new SimpleClient();
		client.go();
	}

	private CytomerConnector cc;

	private void go() {

		cc = new CytomerConnector();
		System.out.println("Getting the root of the ontology:");
		CytomerClass root = cc.getRoot();
		System.out.println("\tRoot of Cytomer: " + root);
		System.out.println("\tRoot is in ns " + root.getNamespace());
		//
		System.out.println("Testing if classes are shells");
		Set<OntologyClass> firstChildren = root.getChildren();
		System.out.println("\tNumber of children on the first level: "
				+ firstChildren.size());
		OntologyClass firstChild = firstChildren.iterator().next();
		System.out.println("\tFirst child is: " + firstChild);
		System.out.println("\tFirst child is a shell: " + firstChild.isShell());
		//
		System.out.println("Fetch first child directly");
		firstChild = cc.getCls(firstChild);
		System.out.println("\tAfter refetch, first child is a shell: "
				+ firstChild.isShell());
		System.out.println("Children of first child:");
		System.out.println("\t" + firstChild.getChildren());
		//
		System.out
				.println("Get class with name and namespace'hilus_of_liver' in ns 'http://cytomer.bioinf.med.uni-goettingen.de/organ'");
		CytomerClass cls = cc.getCls("hilus_of_liver",
				"http://cytomer.bioinf.med.uni-goettingen.de/organ");
		System.out.println("\tGot class " + cls);
		// //
		
		System.out.println("Accessing annotation values");
		System.out.println("\tACC in the list of annotations : "
				+ cls.getAnnotationValues("ACC").iterator().next());
		System.out.println("\tACC directly : " + cls.getACC());

		//
		System.out.println("Is organ downstream of owl:ting?");
		JsonCls organ = cc.getCls("organ",
				"http://protege.stanford.edu/plugins/owl/protege");
		Cytomer2DClassList list2d = cc.xDownstreamOfY(cls, root);
		System.out.print("\t Path between hilus of liver and owl:thing ");
		CytomerClassList firstPath = (CytomerClassList) list2d.getEntities()
				.get(0);
		for (CytomerClass step : firstPath.getEntities()) {
			System.out.print(step + " ");
		}
		System.out.println();
		//
		System.out.println("Is hilus of liver an entity or concept?");
		CytomerClassList abstractCls = cc.reduceToLevel(2, cls);
		System.out.println("\tClass at second level is: " + abstractCls.get(0));
		System.out.println();

		System.out.println("Getting all organs");
		OntologyClassList organList = cc.getOrganList();
		System.out.println("\tNumber of organs: "
				+ cc.getOrganList().getEntities().size());

		System.out.println("Getting organ for hilus_of_liver");
		System.out.println("\tOrgan:" + cc.getOrgansForClass(cls).get(0));

		System.out
				.println("Storing the organList on the server in the partition 'tmp' as list 'localtest'");

		cc.storeList("tmp", "localtest", organList);

		System.out
				.println("searching coresponding class for hepatocyte in stored set");
		CytomerClass hepatocyte = cc.getCls("hepatocyte",
				"http://cytomer.bioinf.med.uni-goettingen.de/cell");
		CytomerClassList hepSet = cc.findUpstreamInSet(hepatocyte, "tmp",
				"localtest");
		System.out.println("\t" + hepSet.get(0));

		//
		System.out.println("Search for 'hepa*'");
		CytomerClassList hepaList = cc.searchCls("hepa*");
		System.out.println("\t" + hepaList.size() + " hits");
		cc.storeList("tmp", "localtest", hepaList);
		System.out
				.println("Cluster the search results for 'hepa*' in max 15 groups");
		Cytomer2DClassList clusters = cc.reduceToClusterSize(15, "tmp",
				"localtest");
		for (Object o : clusters.getEntities()) {
			CytomerClassList list = (CytomerClassList) o;
			System.out.print("\t" + list.get(0) + "\n\t\t");
			for (int i = 1; i < list.size(); i++) {
				System.out.print(list.get(i) + "  ");
			}
			System.out.println();
		}

		System.out
				.println("Walking down one way to show that classes are loading their children lazy");
		OntologyClass n = root;
		System.out.print("\t");
		while (n.getChildren() != null && n.getChildren().size() > 0) {
			System.out.print(" -> " + n);
			Set children = n.getChildren();
			OntologyClass firstC = (OntologyClass) children.iterator().next();
			n = firstC;
		}
		System.out.println();

		System.out
				.println("Getting all second level object properties from the ontology");
		JsonPropertyList<JsonObjectProperty> properties = cc
				.getObjectProperties();
		System.out.print("\t");
		for (JsonObjectProperty p : properties.getEntities()) {
			Set<JsonObjectProperty> superProperties = p.getSubProperties();
			if (superProperties == null || superProperties.size() < 1) {
				continue;
			}
			System.out.print(superProperties.iterator().next().getName() + " ");
		}
		System.out.println();
	}
}
