package de.sybig.oba.server.tribolium;

import de.sybig.oba.server.ObaAnnotation;
import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaObjectPropertyExpression;
import de.sybig.oba.server.OntologyFunctions;
import de.sybig.oba.server.OntologyHelper;
import de.sybig.oba.server.StorageHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides ontology functions specific for the Tribolium Ontology
 * TrOn.
 *
 * @author juergen.doenitz@ibeetle-base.uni-goettingen.de
 */
public class TriboliumFunctions extends OntologyFunctions {

    private static final Logger log = LoggerFactory.getLogger(TriboliumFunctions.class);
    private volatile Map<ObaClass, ObaClass> concreteClasses;
    private Set<ObaClass> mixedClasses;
    private Set<ObaClass> devStages;
    private OWLObjectProperty partOfRestriction;
    private Map<ObaClass, Set<ObaClass>> hasParts;
    private Set<ObaClass> genericClasses;
    private Set<ObaClass> concreteAndAdditinalClasses;

    /**
     * Inits the class with the functions for TrOn and loads the property file.
     *
     */
    public TriboliumFunctions() {
        super();
        loadPropertiesFromJar("/tribolium.properties");
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
        StringBuilder out = new StringBuilder()
                .append("<h1>Available functions</h1>\n")
                .append("<dl>")
                .append("<dt>/concreteClasses</dt><dd>Gets a all concrete classes, i.e. disectible structues linked to a developmental stage</dd>")
                .append("<dt>/devStages</dt><dd>Get all developemental stages, including sub stages like 'L1'</dd>")
                .append("<dt>/genericClasses</dt><dd>Get all generic classes; generic classes are biological concepts not related to a developemental stage.</dd>")
                .append("<dt>/concreteClasses</dt><dd>Get all concrete classes; concrete classes are dissectible morphological structures and linkded to a developmental stage. Mixed classes are also in te set of generic classes. </dd>")
                .append("<dt>/mixedClasses</dt><dd>Get all mixed classes. Mixed classes are morphological structures only present in a singel developmental stage. Mixed classes are also concrete classes.</dd>")
                .append("<dt>/searchInGeneric/{pattern}</dt><dd>Searches a pattern in the index fields of only the generic classes.</dd>")
                .append("<dt>/searchInGenericAndMixed/{pattern}</dt><dd>Searches a pattern in the index fields of only the generic and mixed classes, not in the concrete ones.</dd>");
        return out.toString();
    }

    @Override
    public void reset() {
        concreteClasses.clear();
        mixedClasses.clear();
        devStages.clear();
        partOfRestriction = null;
        hasParts = null;
        genericClasses = null;
        concreteAndAdditinalClasses = null;
    }

    /**
     * Get all concrete classes. A concrete class has, direct or indirect, a
     * partOf relation to a developmental stage.
     *
     * @return The concrete classes
     */
    @GET
    @Path("/concreteClasses")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getConcreteClasses() {

        if (concreteClasses == null) {
            log.info("creating the list of concrete classes");
            ObaClass root = ontology.getRoot();
            concreteClasses = new HashMap<ObaClass, ObaClass>();
            addDevStagesToConcreteClasses();
            Set<String> restrictions = new HashSet<String>();

            restrictions.add("part_of");
            partOfRestriction = OntologyHelper.getObjectProperties(root.getOntology(), restrictions).iterator().next();

            for (ObaClass child : OntologyHelper.getChildren(root)) {
                findConcreteClassesDownstream(child);
            }
            // for (ObaClass child : OntologyHelper.getChildren(root)) {
            // findConcreteClassesDownstream(child);
            // }
            fixPodomer();
            log.info("{} classes found", concreteClasses.size());
        }

        return concreteClasses.keySet();
    }

    /**
     * Add the concrete developemental stages to the list of concrete classes.
     *
     */
    private void addDevStagesToConcreteClasses() {
        for (ObaClass stage : getDevStages()) {
            concreteClasses.put(stage, stage);
        }
    }

    private Set<ObaClass> getConcreteAndAdditClasses() {
        if (concreteAndAdditinalClasses == null) {
            concreteAndAdditinalClasses = new HashSet<ObaClass>();
            concreteAndAdditinalClasses.addAll(getConcreteClasses());
//            System.out.println("concrete classes " + concreteAndAdditinalClasses.size());
            StorageHandler sh = new StorageHandler();
//            Set<ObaClass> storedList = sh.getStorage("ibeetle", "addGenCls");
//            if (storedList != null) {
//                concreteAndAdditinalClasses.addAll(storedList);
//            }
//            System.out.println("concretea and additiaonl classes " + concreteAndAdditinalClasses.size());
        }
//        System.out.println("returning " + concreteAndAdditinalClasses.size());
        return concreteAndAdditinalClasses;
//        return getConcreteClasses();
    }

    /**
     * Get all generic classes, ontology classes that represent biological
     * concepts and not linked to developmental stage. The mixed classes are not
     * part of the generic classes.
     *
     * @return The generic classes of the ontology.
     */
    @GET
    @Path("/genericClasses")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getGenericClasses() {
        if (genericClasses == null) {
            genericClasses = new HashSet<ObaClass>();
            addToGenericClasses(ontology.getRoot());
            fixPodomer();
        }
        return genericClasses;
    }

    @GET
    @Path("/mixedClasses")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getMixedClasses() {
        if (mixedClasses == null) {
            mixedClasses = new HashSet<ObaClass>();
            String adult = "adult";
            String pupa = "pupa";
            String larva = "larva";

            Set<ObaClass> allConcrete = getConcreteClasses();
            String label;
            l1:
            for (ObaClass c : allConcrete) {
//            if (OntologyHelper.getChildren(c).size() > 0){
//                continue;}
                for (ObaClass p : OntologyHelper.getParents(c)) {
//                if (p.getIRI().getFragment().endsWith("0000004")) {
//                    mixed.add(c);
//                    continue l1;
//                }

                    if (isParentDirectGeneric(p)) {
                        continue l1;
                    }
                    label = labelOf(c);
                    if (label.contains(adult)
                            || label.contains(larva) || label.contains(pupa)) {
                        continue l1;
                    }
                }
                mixedClasses.add(c);
            }
        }

        return mixedClasses;
    }

    /**
     * Get all ontology classes of organisms in specific developmental stage.
     * This are all classes below of the node "organism" and their children.
     *
     * @return The developmental stages, or <code>null</code> if the parent node
     * is not found.
     */
    @GET
    @Path("/devStages")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getDevStages() {
        if (devStages == null) {
            devStages = new HashSet<ObaClass>();
            ObaClass devStageCls = ontology.getOntologyClass(getProperties().getProperty("dev_stages_id"),
                    getProperties().getProperty("tribolium_ns"));
            if (devStageCls == null) {
                log.error("Could not get the parent node of the developmental stages");
                return null;
            }
            for (ObaClass child : OntologyHelper.getChildren(devStageCls)) {
                addDevStagesDownstream(child);
            }
        }
        return devStages;
    }

    /**
     * Searches a generic class matching the search pattern. All concrete
     * classes are excluded from the result set. In the search pattern "_" is
     * replaced by a white space.
     *
     * @param searchPattern The pattern to search for.
     * @return Generic classes matching the pattern
     */
    @GET
    @Path("/searchInGeneric/{pattern}")
    @Produces(ALL_TYPES)
    public List<ObaClass> searchInGeneric(@PathParam("pattern") final String searchPattern) {
        Set<ObaClass> toRemove = getConcreteClasses();
        // In the owl representation of the OBO ontology spaces are replace by '_'
        String internalSearchPattern = searchPattern.replaceAll("_", " ");

//        try {
//            StorageHandler storageHandler = new StorageHandler();
//            Set<ObaClass> additional = storageHandler.getStorage("ibeetle",
//                    "addGenCls");
//            if (additional != null) {
//                toRemove.removeAll(additional);
//            }
//        } catch (WebApplicationException we) {
//            // the storage list wasn't found, just use the generic classes.
//        }
        List<ObaClass> hits = ontology.searchCls(internalSearchPattern, null);
        hits.removeAll(toRemove);

        return hits;
    }

    @GET
    @Path("/searchInGenericAndMixed/{pattern}")
    @Produces(ALL_TYPES)

    public List<ObaClass> searchInGenericAndMixed(@PathParam("pattern") String searchPattern) {
        Set<ObaClass> toRemove = new HashSet<ObaClass>();
        toRemove.addAll(getConcreteClasses());
        //mixed classes are also concrete classes, so remove them from removal
        toRemove.removeAll(getMixedClasses());

        List<ObaClass> hits = ontology.searchCls(searchPattern, null);
        hits.removeAll(toRemove);

        return hits;
    }

    @GET
    @Path("/searchInConcrete/{pattern}")
    @Produces(ALL_TYPES)
    public List<ObaClass> searchInConcrete(
            @PathParam("pattern") String searchPattern) {
        List<ObaClass> hits = ontology.searchCls(searchPattern, null);
        System.out.println("1 " + hits);
        System.out.println("2 " + getConcreteClasses());
        hits.retainAll(getConcreteClasses());
        return hits;
    }

    @GET
    @Path("/searchInConcreteAndMixed/{pattern}")
    @Produces(ALL_TYPES)
    public List<ObaClass> searchInConcreteAndMixed(
            @PathParam("pattern") String searchPattern) {
        List<ObaClass> hits = ontology.searchCls(searchPattern, null);
        Set<ObaClass> concreteAndMixed = new HashSet<ObaClass>(); //TODO cache?
        concreteAndMixed.addAll(getConcreteClasses());
        concreteAndMixed.addAll(getMixedClasses());
        hits.retainAll(concreteAndMixed);
        return hits;
    }

    /**
     * Get the concrete classes downstream of a generic class. Therefor a
     * breath-first-search is started at the start class. The graph is traversed
     * down along the class hierarchy and the "hasPart" relations. From each
     * branch the first concrete class is added to the result list. It is not
     * assumed, that a concrete class has further sub classes.
     *
     * @param cls A generic class to start the search from.
     * @param ns The name space of the class.
     * @return Concrete classes downstream of the given generic class.
     */
    @GET
    @Path("/searchConcreteFor/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> findConcreteFor(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        ObaClass startClass = ontology.getOntologyClass(cls, ns);
        Set<ObaClass> concreteCls = findConcrete(startClass);
        return concreteCls;
    }

    /**
     * Get all concrete classes, linked to the given developmental stage,
     * downstream of the start class. See also {@link
     * #findConcreteFor(String, String)}
     *
     * @param genericCls The generic class with optional name space as matrix
     * parameter
     * @param devStage The developmental stage
     * @param ns The optional name space of the developmental stage
     * @return
     */
    @GET
    @Path("/concreteClassInDevStage/{cls}/{devStage}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> devStageOfCls(
            @PathParam("cls") PathSegment genericCls,
            @PathParam("devStage") PathSegment devStage,
            @QueryParam("ns") String ns) {
        // log.info("search concrete for {} in stage {}", genericCls, devStage);
        ObaClass genericClass = getClassFromPathSegement(genericCls);
        ObaClass devStageCls = getClassFromPathSegement(devStage, ns);
        Set<ObaClass> allConcrete = findConcrete(genericClass);
        HashSet<ObaClass> usedDevStages = new HashSet<ObaClass>();
        usedDevStages.add(devStageCls);
        //TODO call recursive, reuse addDevStagesDownstream()
        for (ObaClass child : OntologyHelper.getChildren(devStageCls)) {
            usedDevStages.add(child);
        }
//        StorageHandler stoargeHandler = new StorageHandler();
//        Set<ObaClass> storedList = stoargeHandler.getStorage("ibeetle", "addGenCls"); // see also line 133
//        if (storedList == null) {
//            storedList = new HashSet<ObaClass>();
//        }
//        System.out.println("stored classes added " + storedList);
        Set<ObaClass> result = new HashSet<ObaClass>();
//        System.out.println("downstream classes " + allConcrete.size());
        for (ObaClass c : allConcrete) {
            if (mixedClasses.contains(c)
                    || (concreteClasses.containsKey(c) && usedDevStages.contains(concreteClasses.get(c)))) {
                result.add(c);
//            } else if (storedList.contains(c)) {
//                result.add(c);
            } else {
//                System.out.println("skipping " + c);
            }
        }
//        log.debug("returning {} concrete classes for {} and stage " + devStageCls, result.size(), genericClass);
        return result;
    }

    @GET
    @Path("/devStageOfCls/{concreteClass}")
    @Produces(ALL_TYPES)
    public ObaClass devStageOfCls(
            @PathParam("concreteClass") PathSegment concreteClass,
            @QueryParam("ns") String ns) {

        ObaClass concreteCls = getClassFromPathSegement(concreteClass, ns);

        if (concreteCls == null) {
            // TODO
            return null;
        }
        if (!getConcreteClasses().contains(concreteCls)) {
            // TODO
            log.error("Class {} is not generic ", concreteCls);
            return null;
        }
        return concreteClasses.get(concreteCls);
    }

    @GET
    @Path("/allClasses")
    public Set<ObaClass> getAllClasses() {
        HashSet<ObaClass> all = new HashSet<ObaClass>();
        ObaClass r = ontology.getRoot();
        all.add(r);
        addChildsToSet(r, all);
        return all;
    }

    @GET
    @Path("/clsLoops")
    public Set<ObaClass> getClsLoops() {
        HashSet<ObaClass> all = new HashSet<ObaClass>();
        ObaClass r = ontology.getRoot();
        searchClsLoops(r, all);
        return all;
    }

    @GET
    @Path("/relationLoops/{property}")
    public Set<ObaClass> getRelationLoops(@PathParam("property") String relation) {
        //TODO realy use parameter ;)
        HashSet<ObaClass> all = new HashSet<ObaClass>();
        ObaClass r = ontology.getRoot();
        searchPropertyLoops(r, all, getPartOfRestriction());
        return all;
    }

    private void fixPodomer() {
        ObaClass podomer = ontology.getOntologyClass("TrOn_0000035", null);
        if (genericClasses != null && podomer != null) {
            genericClasses.add(podomer);
        }
        if (concreteClasses != null) {
            concreteClasses.remove(podomer);
        }
    }

    private void addDevStagesDownstream(ObaClass start) {
        devStages.add(start);
        //TODO reenable
        // at the moment we don't find concrete classes for larval head, because
        // we search with larva and all classes are linked to L1
        for (ObaClass child : OntologyHelper.getChildren(start)) {
            addDevStagesDownstream(child);
        }
    }

    /**
     * Do a breath-first search starting at the start node. From each branch the
     * first concrete class is added to the result list. It is not assumed, that
     * a concrete class has further sub classes.
     *
     * @param startClass
     * @return
     */
    private Set<ObaClass> findConcrete(ObaClass startClass) {
        Set<ObaClass> result = new HashSet<ObaClass>();
        findDownToConcrete(startClass, result);
        return result;
    }

    /**
     * @param cls
     */
    private void findConcreteClassesDownstream(ObaClass cls) {

        HashSet<ObaClass> upstreamPartOf = new HashSet<ObaClass>();
        for (ObaObjectPropertyExpression ope : getPartOfRestrictions(cls)) {
            upstreamPartOf.add(ope.getTarget());
        }
        ObaClass devStage = checkIfConcrete(cls, upstreamPartOf);
        if (devStage != null) {
            addToConcreteClasses(cls, devStage);
            return;
        }

        for (ObaClass child : OntologyHelper.getChildren(cls)) {
            findConcreteClassesDownstream(child);
        }
    }

    private void addToConcreteClasses(ObaClass cls, ObaClass devStage) {
        concreteClasses.put(cls, devStage);
        for (ObaClass child : OntologyHelper.getChildren(cls)) {
            if (child.equals(cls)) {
                log.warn("sub class loop {} " + cls);
                return;
            }
            if (cls.equals(child)) {
                // loop
                continue;
            }
            addToConcreteClasses(child, devStage);
        }
    }

    /**
     * Get the partOf restrictions of a class. If no partOf restrictions are
     * found an empty set is returned.
     *
     * @param cls
     * @return
     */
    private Set<ObaObjectPropertyExpression> getPartOfRestrictions(ObaClass cls) {
        HashSet<ObaObjectPropertyExpression> partOf = new HashSet<ObaObjectPropertyExpression>();
        for (ObaObjectPropertyExpression ope : OntologyHelper.getObjectRestrictions(cls)) {
            if (!ope.getRestriction().equals(partOfRestriction)) {
                continue;
            }
            partOf.add(ope);
        }
        return partOf;
    }

    /**
     * Checks if the given class is concrete. A class is concrete if: <ul>
     * <li>If the class was already added to the list of concrete classes (by
     * its parents)</li> <li>The class has a direct partOf relation to one of
     * the stages.</li> <li>recursion</li> </ul> The class is not added to the
     * list of concrete classes, only a boolean is returned.
     *
     * @param start The class to test
     * @param upstreamCls
     * @return
     */
    private ObaClass checkIfConcrete(ObaClass start, Set<ObaClass> upstreamCls) {
        ObaClass devStage = isConcrete(start);
        if (devStage != null) {
            return devStage;
        }

        for (ObaClass cls : upstreamCls) {
            devStage = isConcrete(cls);
            if (devStage != null) {
                return devStage;
            }
        }

        for (ObaClass cls : upstreamCls) {
            HashSet<ObaClass> newUpstream = new HashSet<ObaClass>();
            newUpstream.addAll(OntologyHelper.getParents(cls));
            for (ObaObjectPropertyExpression ope : getPartOfRestrictions(cls)) {
                newUpstream.add(ope.getTarget());
            }
            if (newUpstream.contains(cls)) {
                log.warn("partOf loop {}", cls);
                continue;
            }
            devStage = checkIfConcrete(cls, newUpstream);
            if (devStage != null) {
                return devStage;
            }
        } // for
        return null;
    }

    /**
     * Checks if the given class is a concrete class. A class is concrete, if it
     * was already added to the list of concrete classes (through it parents),
     * has a direct partOf relation to a developmental stage, or has a concrete
     * parent.
     *
     * @param cls
     * @return
     */
    private ObaClass isConcrete(ObaClass cls) {
        if (concreteClasses.containsKey(cls)) {
            return concreteClasses.get(cls);
        }
        for (ObaObjectPropertyExpression ope : getPartOfRestrictions(cls)) {
            if (getDevStages().contains(ope.getTarget())) {
                return ope.getTarget();
            }
        }
        for (ObaClass p : OntologyHelper.getParents(cls)) {
            if (p.equals(cls)) {
                log.warn("subclass loop {}", p);
                break;
            }
            ObaClass x = isConcrete(p);
            if (x != null) {

                return x;
            }

        }
        return null;
    }

    /**
     * If the start class is a concrete class, the class is added to the result
     * list. Otherwise <code>findDownToConcrete</code> is called for each child
     * of the start class and for each class connected. with "hasPart".
     *
     * @param cls The start class
     * @param result
     */
    private void findDownToConcrete(ObaClass cls, Set<ObaClass> result) {
//        if (getConcreteClasses().contains(cls)) {
//        System.out.println("test " + labelOf(cls));
        if (getConcreteClasses().contains(cls) || getMixedClasses().contains(cls)) {
            result.add(cls);
        }
        Set<ObaClass> downstreamClasses = new HashSet<ObaClass>();
        downstreamClasses.addAll(OntologyHelper.getChildren(cls));
        Set<ObaClass> hp = getHasPart(cls);
        if (hp != null) {
            downstreamClasses.addAll(hp);
        }
        for (ObaClass child : downstreamClasses) {
            if (child.equals(cls)) {
                log.error("loop detected " + cls);
                continue;
            }
            findDownToConcrete(child, result);
        }
    }

    /**
     * Pretty prints the label of the class for debugging.
     *
     * @param cls The class to get the label of
     * @return The label of the class
     */
    private String labelOf(ObaClass cls) {
        for (ObaAnnotation annotation : OntologyHelper.getAnnotationProperties(
                cls, ontology.getOntology())) {
            if (annotation.getName().equals("label")) {
                return annotation.getValue();
            }
        }
        return null;
    }

    /**
     * Returns a list of classes with a partOf relation to the given class. If
     * no relations are found, <code>null</code> is returned.
     *
     * @param cls
     * @return
     */
    private Set<ObaClass> getHasPart(ObaClass cls) {
        if (hasParts == null) {
            initHasPartsMap();
            log.info("cached hasPart relations to {} classes", hasParts.size());
        }
        return hasParts.get(cls);
    }

    private void initHasPartsMap() {
        hasParts = new HashMap<ObaClass, Set<ObaClass>>();
        ObaClass root = ontology.getRoot();
        for (ObaClass child : OntologyHelper.getChildren(root)) {
            findHasParts(child);
        }
    }

    private void findHasParts(ObaClass cls) {
        Set<ObaObjectPropertyExpression> partOfRestrictions = getPartOfRestrictions(cls);
        for (ObaObjectPropertyExpression ope : partOfRestrictions) {
            ObaClass target = ope.getTarget();
            if (!hasParts.containsKey(target)) {
                hasParts.put(target, new HashSet<ObaClass>());
            }
            hasParts.get(target).add(cls);
        }

        for (ObaClass child : OntologyHelper.getChildren(cls)) {
            if (cls.equals(child)) {
                log.warn("cls-loop " + cls);
                continue;
            }
            findHasParts(child);
        }
    }

    private void addToGenericClasses(ObaClass parent) {
        if (getConcreteClasses().contains(parent)) {
            return;
        }
        genericClasses.add(parent);
        Set<ObaClass> children = OntologyHelper.getChildren(parent);
        if (children == null) {
            return;
        }
        for (ObaClass child : children) {
            addToGenericClasses(child);
        }
    }

    /**
     * Checks if the children of the class belongs to more than one
     * developmental stage.
     *
     * @param parent
     * @return
     */
    private boolean isParentDirectGeneric(ObaClass parent) {

        Set<ObaClass> childrenDevStages = new HashSet();
        ObaClass multi = null;
        Set<ObaClass> children = OntologyHelper.getChildren(parent);
        for (ObaClass c : children) {
            ObaClass ds = concreteClasses.get(c);
            if (ds == null) {
                continue;
            }
            if (labelOf(ds).endsWith("male")) {
                ds = OntologyHelper.getParents(ds).iterator().next();
            }
            if (childrenDevStages.contains(ds)) {
                if (multi == null) {
                    multi = ds;
                }
                if (multi != ds) {
                    return false;
                }
            }
            childrenDevStages.add(ds);
        }
        return childrenDevStages.size() > 1;
    }

    private void addChildsToSet(ObaClass parent, Set<ObaClass> set) {
        Set<ObaClass> children = OntologyHelper.getChildren(parent);
        if (children == null) {
            return;
        }
        for (ObaClass child : children) {
            if (child.equals(parent)) {
                continue;
            }
            set.add(child);
            addChildsToSet(child, set);
        }
    }

    private void searchClsLoops(ObaClass parent, Set<ObaClass> set) {
        Set<ObaClass> children = OntologyHelper.getChildren(parent);
        if (children == null) {
            return;
        }
        for (ObaClass child : children) {
            if (child.equals(parent)) {
                set.add(child);
                continue;
            }
            searchClsLoops(child, set);
        }
    }

    private void searchPropertyLoops(ObaClass parent, Set<ObaClass> set, OWLObjectProperty restriction) {
        HashSet<ObaObjectPropertyExpression> partOf = new HashSet<ObaObjectPropertyExpression>();
        for (ObaObjectPropertyExpression ope : OntologyHelper.getObjectRestrictions(parent)) {
            if (!ope.getRestriction().equals(restriction)) {
                continue;
            }
            if (ope.getTarget().equals(parent)) {
                set.add(parent);
            }
        }
        Set<ObaClass> children = OntologyHelper.getChildren(parent);
        if (children == null) {
            return;
        }

        for (ObaClass child : children) {
            if (child.equals(parent)) {
                continue;
            }
            searchPropertyLoops(child, set, restriction);
        }
    }

    private OWLObjectProperty getRestriction(String restriction) {
        Set<String> r = new HashSet<String>();
        r.add(restriction);
        return OntologyHelper.getObjectProperties(ontology.getOntology(), r).iterator().next();
    }

    private OWLObjectProperty getPartOfRestriction() {
        if (partOfRestriction == null) {
            partOfRestriction = getRestriction("part_of");
        }
        return partOfRestriction;
    }
}
