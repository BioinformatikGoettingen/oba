/*
 * Created on May 11, 2010
 *
 */
package de.sybig.oba.server;

import com.sun.corba.se.pept.transport.ContactInfo;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import javax.ws.rs.*;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.*;

public class OntologyFunctions extends AbstractOntolgyResource implements
        OntologyFunction {

    private Logger logger = LoggerFactory.getLogger(OntologyFunctions.class);
    protected static final String ALL_TYPES = "text/plain, text/html, application/json";
    @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
        StringBuffer out = new StringBuffer();
        out.append("<h1>Available functions</h1>\n");
        out.append("<dl>");
        out.append("<dt>/searchCls/{cls}</dt><dd>Searches for a class in the ontology. {cls} should be replaced with the name of the searched class. The namespace can be defined by the query parameter 'ns'</dd>");
        out.append("<dt>XdownstreamOfY/{x}/{y}</dt><dd>Searches the class x below  of class y, i. e. x is a sibling of y. The namespace of the classes can be defined as matrix or query parameter 'ns'</dd>");
        out.append("<dt>reduceToLevel/{level}/{cls}</dt><dd>Returns a list of ancestor of the class at the given level below of owl:Thing. If the level of the given class is less then the required level, the class itself is returned in the list. The namespace of the classes can be defined as matrix or query parameter 'ns'</dd>");
        out.append("<dt>reduceToLevel/{level}/{partition}/{name}</dt><dd>Similar to the function above, but uses a reference to a stored list as input and returns a list of list.</dd>");
        out.append("<dt>reduceToLevelShortestPath/{level}/{cls}</dt><dd>Same 'reduceToLevel' above, but only the shortest paths are honored.</dd>");
        out.append("<dt>reduceToLevelShortestPath/{level}/{partition}/{name}</dt><dd>Same 'reduceToLevel' above, but only the shortest paths are honored.</dd>");
        out.append("<dt>reduceToClusterSize/{size}/{partition}/{name}</dt><dd>Maps in each iteration the input classes to its parents, until there are not more clusters than specified by 'level'. The classes with the greatest distance from root are mapped first.</dd>");
        out.append("<dt>clsForObjectProperty/{restriction}</dt><dd>Get all (domain) classes with this object property</dd>");
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
     * Searches a class in the ontology. The class is searched by its name in the lucene index. An ordered list of
     * classes is returned
     *
     * @param searchPattern
     * @param ns
     * @return
     */
    @GET
    @Path("{searchCls:searchCls}/{cls}")
    @Produces("text/plain, application/json, text/html")
    @HtmlBase(value = "../../../cls/")
    public List<ObaClass> searchCls(
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
     * Maps a class to its parents at the given level below the root node. If the level of the given class is below or
     * equal to the requested level, the input class is returned as only member of the list.
     * <p/>
     * If the input class is not found in the ontology, a web application exception 404 is thrown.
     *
     * @param level
     * @param clsPathSegment
     * @param ns
     * @return A list of classes
     */
    @GET
    @Path("reduceToLevelShortestPath/{level}/{cls}")
    @Produces("text/html, text/plain, application/json")
    public List<OWLClass> reduceToLevelShortestPath(
            @PathParam("level") Integer level,
            @PathParam("cls") PathSegment clsPathSegment,
            @QueryParam("ns") String ns) {

        ObaClass cls = getClassFromPathSegement(clsPathSegment, ns);
        List<List<ObaClass>> path = getShortestPathsToRoot(cls);

        LinkedList<OWLClass> outList = new LinkedList<OWLClass>();
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
    @Path("reduceToLevel/{level}/{cls}")
    @Produces("text/html, text/plain, application/json")
    public List<ObaClass> reduceToLevel(@PathParam("level") Integer level,
                                        @PathParam("cls") PathSegment clsPathSegment,
                                        @QueryParam("ns") String ns) {
        ObaClass cls = getClassFromPathSegement(clsPathSegment, ns);

        List<List<ObaClass>> pathes = getAllPathsToRoot(cls);
        List<ObaClass> outList = new LinkedList<ObaClass>();
        for (List<ObaClass> p : pathes) {
            outList.add(p.get(p.size() - level));
        }
        return outList;
    }

    @GET
    @Path("reduceToLevelShortestPath/{level}/{partition}/{name}")
    @Produces("text/html, text/plain, application/json")
    public List<List<ObaClass>> reduceListToLevelShortestPath(
            @PathParam("level") Integer level,
            @PathParam("partition") String partition,
            @PathParam("name") String name) {
        LinkedList<List<ObaClass>> outList = new LinkedList<List<ObaClass>>();
        StorageHandler storageHandler = new StorageHandler();
        Set<ObaClass> startClasses = storageHandler.getStorage(partition, name);
        ObaClass ancestor;

        for (ObaClass startClass : startClasses) {
            LinkedList<ObaClass> clsPath = new LinkedList<ObaClass>();
            clsPath.add(startClass);
            List<List<ObaClass>> path = getShortestPathsToRoot(startClass);
            if (path != null && path.size() > 0 && path.get(0).size() >= level) {
                for (List<ObaClass> p : path) {
                    ancestor = p.get(p.size() - level);
                    if (! clsPath.contains(ancestor)){
                    clsPath.add(ancestor);
                    }
                }
            }
            outList.add(clsPath);
        }
        return outList;
    }

    /**
     * Get all ancestors at a given level for the classes in the set. For the search for the ancestors all paths are
     * honored, not only the shortest.
     * <p/>
     * The result is a list of list of ontology classes. The result of each input class is stored in one list, with the
     * input class at the first place.
     *
     * @param level
     * @param partition
     * @param name
     * @return
     */
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
        ObaClass ancestor;

        for (ObaClass startClass : startClasses) {
            List<List<ObaClass>> pathes = getAllPathsToRoot(startClass);
            List<ObaClass> list = new LinkedList<ObaClass>();
            list.add(startClass);
            for (List<ObaClass> p : pathes) {
                if (p.size() >= level) {
                    ancestor = p.get(p.size() - level);
                    if (!list.contains(ancestor)){
                        list.add(ancestor);
                    }
//                    list.add(p.get(p.size() - level));
                }
            }
            outList.add(list);
        }
        return outList;
    }

    /**
     * <p> Is X a child of Y? Starting from class X the ontology is searched upstream along the "is_a" relation until
     * class Y or owl:Thing is found. If class Y is found on a path, the path between class Y and X is returned.<br />
     * For both classes the namespace can be defined as matrix parameter. <em>'/' is not encoded correct, and has to be
     * replaced by '$'</em><br /> Both class names should be names of existing classes in the ontology. </p> Response
     * codes <ul> <li>404: if class X or Y could not be found in the ontology</li> </ul>
     *
     * @param x The name of the successor class
     * @param y The name of the precursor parent
     * @return The path between the both class, <code>null</code> if X is not a successor of Y or one of the response
     *         codes above.
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
        List<List<ObaClass>> downstreamList = searchXdownstreamOfY(clsX, clsY);
        logger.info("-> " + downstreamList.size() + " downstream");
        return downstreamList;
    }

    // @GET
    // @Path("extendedPartOf/{x}/{y}")
    // @Produces("text/plain, text/html, application/json")
    // public List<ObaClass> extendedPartOf(@PathParam("x") PathSegment x,
    // @PathParam("y") PathSegment y, @QueryParam("ns") String ns) {
    // ObaClass clsX = getClassFromPathSegement(x);
    // ObaClass clsY = getClassFromPathSegement(y, ns);
    // OWLObjectProperty partOf = getPartOfRelationOfOntology(clsX);
    // if (partOf == null) {
    // logger.error("could not find a part of relation in the ontology");
    // return null;
    // }
    // LinkedList<ObaClass> path = new LinkedList<ObaClass>();
    // path.add(clsX);
    // LinkedList<List<ObaClass>> pathes = new LinkedList<List<ObaClass>>();
    // pathes.add(path);
    // return getPartOfPath(clsY, pathes, partOf);
    // }

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
        ObaClass c;
        for (; maxSteps > 0; maxSteps--) {
            Map<OWLClass, List<OWLClass>> map = new HashMap<OWLClass, List<OWLClass>>();
            for (List<ObaClass> path : allPaths) {
                OWLClass cls = path.size() < maxSteps ? path.get(0) : path
                        .get(path.size() - maxSteps);
                if (!map.containsKey(cls)) {
                    map.put(cls, new LinkedList<OWLClass>());
                }
                c = path.get(0);
                if (! map.get(cls).contains(c)){
                map.get(cls).add(c);
                }
            }
            if (map.keySet().size() <= size) {
                return map;
            }
        }
        return null;
    }

    @GET
    @Path("clsForObjectProperty/{restriction}")
    @Produces("text/plain, application/json, text/html")
    public Set<ObaClass> getDomainClsOfProperty(
            @PathParam("restriction") String cls, @QueryParam("ns") String ns) {
        Set<ObaClass> out = new HashSet<ObaClass>();
        OWLObjectProperty restriction = ontology.getPropertyByName(cls, ns);
        System.out.println("search axioms");
        Set<OWLAxiom> axioms = restriction.getReferencingAxioms(ontology
                .getOntology());
        for (OWLAxiom axiom : axioms) {
            if (!axiom.isOfType(AxiomType.SUBCLASS_OF)) {
                continue;
            }
            OWLClass x = ((OWLSubClassOfAxiomImpl) axiom).getSubClass()
                    .getClassesInSignature().iterator().next();
            ObaClass c = new ObaClass(x, ontology.getOntology());
            out.add(c);
            // System.out.println(x);
            // System.out.println();
        }
        return out;
    }

    /**
     * Searches the class X downstream of class Y, i. e. X should be a ancestor of Y. On success all shortest paths
     * between the two classes are returned. On the first position class X is stored, on the last one class Y.
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

    protected List<List<ObaClass>> getAllPathsToRoot(ObaClass cls) {
        List<ObaClass> path = new LinkedList<ObaClass>();
        path.add(cls);
        List<List<ObaClass>> pathes = new LinkedList<List<ObaClass>>();
        pathes.add(path);
        return getAllPathsToRoot(pathes);
    }

    // TODO join with #extendUpstream
    protected List<List<ObaClass>> getAllPathsToRoot(List<List<ObaClass>> paths) {

        LinkedList<List<ObaClass>> outPathes = new LinkedList<List<ObaClass>>();
        boolean allFinished = true;
        for (List<ObaClass> path : paths) {
            ObaClass last = path.get(path.size() - 1);
            if (last.isOWLThing()) {
                outPathes.add(path);
                continue;
            }

            Set<ObaClass> parents = OntologyHelper.getParents(last);
            List<ObaClass> origPath = path;
            for (ObaClass parent : parents) {
                List<ObaClass> n = new LinkedList<ObaClass>();
                n.addAll(origPath);
                n.add(parent);
                if (!parent.isOWLThing()) {
                    allFinished = false;
                }
                outPathes.add(n);
            }
        }
        if (allFinished) {
            return outPathes;
        }
        return getAllPathsToRoot(outPathes);

    }

    /**
     * Extends a list of OWLClasses until the target class or owl:Thing is hit. If a class has more than one parent the
     * path to the current last are copied so many time as parents are available. The new paths are added to the list of
     * paths.
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

    private OWLObjectProperty getPartOfRelationOfOntology(ObaClass cls) {

        Set<OWLObjectProperty> restrictions = OntologyHelper
                .getObjectProperties(ontology.getOntology(), null);
        for (OWLObjectProperty restriction : restrictions) {
            if (restriction.getIRI().getFragment().replace("_", "")
                    .equalsIgnoreCase("partOf")) {
                return restriction;
            }
        }
        return null;
    }

    private List<ObaClass> getPartOfPath(ObaClass y,
                                         List<List<ObaClass>> pathes, OWLObjectProperty partOf) {
        Iterator<List<ObaClass>> pathIterator = pathes.iterator();
        while (pathIterator.hasNext()) {
            List<ObaClass> path = pathIterator.next();
            ObaClass last = path.get(path.size());

            for (ObaObjectPropertyExpression restriction : OntologyHelper
                    .getObjectRestrictions(last)) {
                if (!restriction.getRestriction().equals(partOf)) {
                    continue;
                }
                ObaClass t = restriction.getTarget();
                if (t.equals(y)) {
                    // add curent
                    // return path;
                } else {
                    List<ObaClass> newPath = new LinkedList<ObaClass>();
                    newPath.addAll(path);
                    pathes.add(newPath);
                }
            }
            pathIterator.remove();
            Set<ObaClass> parents = OntologyHelper.getParents(last);
            if (parents != null) {
                Iterator<ObaClass> parentsIterator = parents.iterator();
                while (parentsIterator.hasNext()) {
                    ObaClass parent = parentsIterator.next();
                    if (parent.equals(y)) {
                        // add curent
                        return path;
                    }
                    List<ObaClass> newPath = new LinkedList<ObaClass>();
                    newPath.addAll(path);
                    pathes.add(newPath);
                }
                parentsIterator.remove();
            }

        }
        return getPartOfPath(y, pathes, partOf);
    }

    /**
     * Returns the list of shortest paths from the given node to owl:Thing One the index 0 is the start class, on the
     * last place owl:Thing
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
