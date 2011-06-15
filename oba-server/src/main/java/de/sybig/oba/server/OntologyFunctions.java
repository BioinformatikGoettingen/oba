/*
 * Created on May 11, 2010
 *
 */
package de.sybig.oba.server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyFunctions extends AbstractOntolgyResource implements
		OntologyFunction {

	private Logger logger = LoggerFactory.getLogger(OntologyFunctions.class);

	@GET
	@Path("/")
	@Produces("text/html")
	@Override
	public String getRoot() {
		StringBuffer out = new StringBuffer();
		out.append("<h1>Available functions</h1>\n");
		out.append("<dl>");
		out.append("<dt>/searchCls/{cls}</dt><dd>Searches for a class in the ontology. {cls} should be replaced with the name of the searched class. The namespace can be defined by the query parameter 'ns'</dd>");
		out.append("<dt>XdownstreamOfY/{x}/{y}</dt><dd>Searches the class x below  of class y, i. e. x is a sibbling of y. The namespace of the classes can be defined as matrix or query parameter 'ns'</dd>");
		out.append("<dt>reduceToLevel/{level}/{cls}</dt><dd>Returns a list of anchestor of the class at the given level below of owl:Thing. If the level of the given class is less then the required level, the class itself is returned in the list. The namespace of the classes can be defined as matrix or query parameter 'ns'</dd>");
		out.append("<dt>reduceToLevel/{level}/{partition}/{name}</dt><dd>Similar to the function above, but uses a reference to a stored list as input and returns a list of list.</dd>");
		out.append("<dt>reduceToClusterSize/{size}/{partition}/{name}</dt><dd>TODO </dd>");
		out.append("</dl>");
		return out.toString();
	}

	@GET
	@Path("{function:.*}")
	@Produces("text/plain, application/json, text/html")
	public Object catchAll(@PathParam("function") String function) {
		logger.warn("could not find function '{}'", function);
		javax.ws.rs.core.Response.ResponseBuilder builder = Response
				.status(404);
		return builder.build();
	}

	/**
	 * Searches a class in the ontology. The class is searched by its name in
	 * the lucene index. An ordered list of classes is returned
	 * 
	 * @param searchPattern
	 * @param ns
	 * @return
	 */
	@GET
	@Path("{searchCls:searchCls}/{cls}")
	@Produces("text/plain, application/json, text/html")
	@HtmlBase(value = "../../../cls/")
	public List<OWLClass> searchCls(
			@PathParam("searchCls") PathSegment searchPathSeg,
			@PathParam("cls") String searchPattern, @QueryParam("ns") String ns) {
		logger.debug("searching for pattern '{}' in ontology {}",
				searchPattern, ontology);
		String fields = null;
		if (searchPathSeg.getMatrixParameters().get("field") != null) {
			fields = searchPathSeg.getMatrixParameters().get("field").get(0);
		}
		return ontology.searchCls(searchPattern, fields);

	}

	/**
	 * Maps a class to its parents at the given level below the root node. If
	 * the level of the given class is below or equal to the requested level,
	 * the input class is returned.
	 * 
	 * If the input class is not found in the ontology, a web application
	 * exception 404 is thrown.
	 * 
	 * @param level
	 * @param clsPathSegment
	 * @param ns
	 * @return A list of classes
	 */
	@GET
	@Path("reduceToLevel/{level}/{cls}")
	@Produces("text/html, text/plain, application/json")
	public List<OWLClass> reduceToLevel(@PathParam("level") Integer level,
			@PathParam("cls") PathSegment clsPathSegment,
			@QueryParam("ns") String ns) {
		LinkedList<OWLClass> outList = new LinkedList<OWLClass>();
		ObaClass cls = getClassFromPathSegement(clsPathSegment, ns);
		logger.info("search level {} of class {}", level, cls);

		List<List<ObaClass>> path = getShortestPathsToRoot(cls);
		if (path == null || path.size() < 1 || path.get(0).size() < level) {
			outList.add(cls);
			return outList;
		}
		for (List<ObaClass> p : path) {
			outList.add(p.get(p.size() - level));
		}
		return outList;
	}

	@GET
	@Path("reduceToLevel/{level}/{partition}/{name}")
	@Produces("text/html, text/plain, application/json")
	public List<List<ObaClass>> reduceListToLevel(
			@PathParam("level") Integer level,
			@PathParam("partition") String partition,
			@PathParam("name") String name) {
		LinkedList<List<ObaClass>> outList = new LinkedList<List<ObaClass>>();
		StorageHandler storageHandler = new StorageHandler();
		Set<ObaClass> startClasses = storageHandler.getStorage(partition, name);
		for (ObaClass startClass : startClasses) {
			LinkedList<ObaClass> clsPath = new LinkedList<ObaClass>();
			clsPath.add(startClass);
			List<List<ObaClass>> path = getShortestPathsToRoot(startClass);
			if (path != null && path.size() > 0 && path.get(0).size() >= level) {
				for (List<ObaClass> p : path) {
					clsPath.add(p.get(p.size() - level));
				}
			}
			outList.add(clsPath);
		}
		return outList;
	}

	/**
	 * <p>
	 * Is X a child of Y? Starting from class X the ontology is searched
	 * upstream along the "is_a" relation until class Y or owl:Thing is found.
	 * If class Y is found on a path, the path between class Y and X is
	 * returned.<br />
	 * For both classes the namespace can be defined as matrix parameter.
	 * <em>'/' is not encoded correct, and has to be replaced by '$'</em><br />
	 * Both class names should be names of existing classes in the ontology.
	 * </p>
	 * Response codes
	 * <ul>
	 * <li>404: if class X or Y could not be found in the ontology</li>
	 * </ul>
	 * 
	 * @param x
	 *            The name of the successor class
	 * @param y
	 *            The name of the precursor parent
	 * @return The path between the both class, <code>null</code> if X is not a
	 *         successor of Y or one of the response codes above.
	 */
	@GET
	@Path("XdownstreamOfY/{x}/{y}")
	@Produces("text/plain, text/html, application/json")
	public Object xdownStreamOfY(@PathParam("x") PathSegment x,
			@PathParam("y") PathSegment y, @QueryParam("ns") String ns) {
		// http://localhost:9998/cytomer/functions/basic/XbelowY/liver;ns=http:$$protege.stanford.edu$plugins$owl$protege/organ?ns=http://protege.stanford.edu/plugins/owl/protege
		ObaClass clsX = getClassFromPathSegement(x);
		ObaClass clsY = getClassFromPathSegement(y, ns);
		logger.info("search class {} downstream of class {}", clsX, clsY);
		if (clsX == null || clsY == null) {
			javax.ws.rs.core.Response.ResponseBuilder builder = Response
					.status(404);
			return builder.build();
		}
		return searchXdownstreamOfY(clsX, clsY);
	}

	@GET
	@Path("reduceToClusterSize/{size}/{partition}/{name}")
	@Produces("text/html, text/plain, application/json")
	public Map<OWLClass, List<OWLClass>> reduceToClusterSize(
			@PathParam("size") Integer size,
			@PathParam("partition") String partition,
			@PathParam("name") String name) {
		LinkedList<List<OWLClass>> outList = new LinkedList<List<OWLClass>>();
		StorageHandler storageHandler = new StorageHandler();
		Set<ObaClass> startClasses = storageHandler.getStorage(partition, name);
		LinkedList<List<ObaClass>> allPaths = new LinkedList<List<ObaClass>>();
		int maxSteps = 0;
		for (ObaClass startClass : startClasses) {
			List<List<ObaClass>> shortest = getShortestPathsToRoot(startClass);
			if (shortest.get(0).size() > maxSteps) {
				maxSteps = shortest.get(0).size();
			}
			allPaths.addAll(shortest);
		}
		for (; maxSteps > 0; maxSteps--) {
			Map<OWLClass, List<OWLClass>> map = new HashMap<OWLClass, List<OWLClass>>();
			for (List<ObaClass> path : allPaths) {
				OWLClass cls = path.size() < maxSteps ? path.get(0) : path
						.get(path.size() - maxSteps);
				if (!map.containsKey(cls)) {
					map.put(cls, new LinkedList<OWLClass>());
				}
				map.get(cls).add(path.get(0));
			}
			if (map.keySet().size() <= size) {
				return map;
			}
		}
		return null;
	}

	/**
	 * Searches the class X downstream of class Y, i. e. X should be a ancestor
	 * of Y. On success all shortest paths between the two classes are returned.
	 * On the first position class X is stored, on the last one class Y.
	 * 
	 * @param clsX
	 * @param clsY
	 * @return
	 */
	protected List<List<ObaClass>> searchXdownstreamOfY(ObaClass clsX,
			ObaClass clsY) {

		List<ObaClass> path = new LinkedList<ObaClass>();
		path.add(clsX);
		LinkedList<List<ObaClass>> pathes = new LinkedList<List<ObaClass>>();
		pathes.add(path);

		List<List<ObaClass>> allPathes = extendUpstream(pathes, clsY);
		// allPathes contains all paths from from clsX to clsY and from clsX to
		// owl:Thing
		List<List<ObaClass>> shortestPath = null;
		for (List<ObaClass> p : allPathes) {
			if (p.get(p.size() - 1).getIRI().equals(clsY.getIRI())) {
				// the path ends by the target class
				if (shortestPath == null
						|| p.size() < shortestPath.get(0).size()) {
					// we have the first path or a path shorter than the
					// previous shortest paths.
					shortestPath = new LinkedList<List<ObaClass>>();
				}
				if (shortestPath.size() == 0
						|| p.size() == shortestPath.get(0).size()) {
					shortestPath.add(p);
				}
			}
		}
		return shortestPath;
	}

	/**
	 * Extends a list of OWLClasses until the target class or owl:Thing is hit.
	 * If a class has more than one parent the path to the current last are
	 * copied so many time as parents are available. The new paths are added to
	 * the list of paths.
	 */
	protected List<List<ObaClass>> extendUpstream(
			List<List<ObaClass>> allPaths, ObaClass targetClass) {

		boolean allPathsFinished = true;
		LinkedList<List<ObaClass>> outPaths = new LinkedList<List<ObaClass>>();
		for (List<ObaClass> currentPath : allPaths) {
			OWLClass lastNode = currentPath.get(currentPath.size() - 1);
			if (lastNode.isOWLThing()
					|| lastNode.getIRI().equals(targetClass.getIRI())) {
				outPaths.add(currentPath);
				continue;
			}
			allPathsFinished = false;
			Set<ObaClass> parentsOfLastNode = OntologyHelper.getParents(
					lastNode, ontology.getOntology());
			for (ObaClass parent : parentsOfLastNode) {
				LinkedList<ObaClass> newPath = new LinkedList<ObaClass>();
				newPath.addAll(currentPath);
				newPath.add(parent);
				outPaths.add(newPath);
			}
		}

		if (allPathsFinished) {
			return outPaths;
		}
		return extendUpstream(outPaths, targetClass);
	}

	private ObaClass getClassFromPathSegement(PathSegment pathSegment) {
		return getClassFromPathSegement(pathSegment, null);
	}

	/**
	 * Returns the list of shortest paths from the given node to owl:Thing One
	 * the index 0 is the start class, on the last place owl:Thing
	 * 
	 * @param startNode
	 * @return
	 */
	protected List<List<ObaClass>> getShortestPathsToRoot(ObaClass startNode) {
		ObaClass rootNode = ontology.getRoot();
		List<List<ObaClass>> path = searchXdownstreamOfY(startNode, rootNode);
		return path;
	}

}
