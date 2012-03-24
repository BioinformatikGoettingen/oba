package de.sybig.oba.server.tribolium;

import de.sybig.oba.server.*;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.PathSegment;
import java.util.*;

public class TriboliumFunctions extends OntologyFunctions {

    private static final String TRIBOLIUM_NS = "http://purl.org/obo/owlapi/tribolium.anatomy"; //TODO move to config
    private static final String DEV_STAGES_ID = "TrOn_0000024";
    private static final Logger log = LoggerFactory.getLogger(TriboliumFunctions.class);

    private volatile Map<ObaClass, ObaClass> concreteClasses;
    private Set<ObaClass> devStages;
    private OWLObjectProperty partOfRestriction;
    private Map<ObaClass, Set<ObaClass>> hasParts;
    private Set<ObaClass> genericClasses;

      @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
          return "";
      }
    
    /**
     * Get all concrete classes. A concrete class has, direct or indirect, a partOf relation to a developmental stage.
     *
     * @return The concrete classes
     */
    @GET
    @Path("/concreteClasses")
    @Produces(ALL_TYPES)
    Set<ObaClass> getConcreteClasses() {

        if (concreteClasses == null) {
            log.info("creating the list of concrete classes");
            ObaClass root = ontology.getRoot();
            concreteClasses = new HashMap<ObaClass, ObaClass>();
            HashSet<String> restrictions = new HashSet<String>();

            restrictions.add("part_of");
            partOfRestriction = OntologyHelper.getObjectProperties(root.getOntology(), restrictions).iterator().next();

            for (ObaClass child : OntologyHelper.getChildren(root)) {
                findConcreteClassesDownstream(child);
            }
            // for (ObaClass child : OntologyHelper.getChildren(root)) {
            // findConcreteClassesDownstream(child);
            // }
            log.info("{} classes found", concreteClasses.size());
        }

        return concreteClasses.keySet();
    }

    @GET
    @Path("/genericClasses")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getGenericClasses() {
        if (genericClasses == null) {
            genericClasses = new HashSet<ObaClass>();
            addToGenericClasses(ontology.getRoot());
        }
        return genericClasses;
    }

    /**
     * Get all ontology classes of organisms in specific developmental stage. This are all classes below of the node
     * "organism" and their children.
     *
     * @return The developmental stages.
     */
    @GET
    @Path("/devStages")
    @Produces(ALL_TYPES)
    Set<ObaClass> getDevStages() {
        if (devStages == null) {
            devStages = new HashSet<ObaClass>();
            ObaClass devStageCls = ontology.getOntologyClass(DEV_STAGES_ID,
                    TRIBOLIUM_NS);
            for (ObaClass child : OntologyHelper.getChildren(devStageCls)) {
                addDevStagesDownstream(child);
            }
        }
        return devStages;
    }

    /**
     * Searches a generic class matching the search pattern. All concrete classes are excluded from the result set.
     *
     * @param searchPattern The pattern to search for.
     * @return Generic classes matching the pattern
     */
    @GET
    @Path("/searchGeneric/{pattern}")
    @Produces(ALL_TYPES)
    public List<ObaClass> findGeneric(@PathParam("pattern") String searchPattern) {
        Set<ObaClass> toRemove = getConcreteClasses();
        // In the owl representation of the OBO ontology spaces are replace by '_'
        searchPattern = searchPattern.replaceAll("_", " ");

        StorageHandler storageHandler = new StorageHandler();
        Set<ObaClass> additional = storageHandler.getStorage("ibeetle",
                "addGenCls");
        if (additional != null) {
            toRemove.removeAll(additional);
        }

        List<ObaClass> hits = ontology.searchCls(searchPattern, null);
        hits.removeAll(toRemove);

        return hits;
    }

    @GET
    @Path("/searchConcrete/{pattern}")
    @Produces(ALL_TYPES)
    public List<ObaClass> findConcrete(
            @PathParam("pattern") String searchPattern) {
        List<ObaClass> hits = ontology.searchCls(searchPattern, null);
        hits.retainAll(getConcreteClasses());
        return hits;
    }

    /**
     * Get the concrete classes downstream of a generic class. Therefor a breath-first-search is started at the start
     * class. The graph is traversed down along the class hierarchy and the "hasPart" relations. From each branch the
     * first concrete class is added to the result list. It is not assumed, that a concrete class has further sub
     * classes.
     *
     * @param cls A generic class to start the search from.
     * @param ns  The name space of the class.
     * @return Concrete classes downstream of the given generic class.
     */
    @GET
    @Path("/searchConcreteFor/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> findConcreteFor(@PathParam("cls") String cls,
                                         @QueryParam("ns") String ns) {
        //TODO use also hasPart relations.
        ObaClass startClass = ontology.getOntologyClass(cls, ns);

        return findConcrete(startClass);
    }

    /**
     * Get all concrete classes, linked to the given developmental stage, downstream of the start class. See also {@link
     * #findConcreteFor(String, String)}
     *
     * @param genericCls The generic class with optional name space as matrix parameter
     * @param devStage   The developmental stage
     * @param ns         The optional name space of the developmental stage
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
        ObaClass concreteCls = getClassFromPathSegement(genericCls);
        ObaClass devStageCls = getClassFromPathSegement(devStage, ns);
        Set<ObaClass> allConcrete = findConcrete(concreteCls);

        Set<ObaClass> result = new HashSet<ObaClass>();
        for (ObaClass c : allConcrete) {
            if (concreteClasses.containsKey(c)
                    && concreteClasses.get(c).equals(devStageCls)) {
                result.add(c);
            }
        }
        // log.info("returning {} results", result.size());
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


    private void addDevStagesDownstream(ObaClass start) {
        devStages.add(start);
        for (ObaClass child : OntologyHelper.getChildren(start)) {
            addDevStagesDownstream(child);
        }
    }

    /**
     * Do a breath-first search starting at the start node. From each branch the first concrete class is added to the
     * result list. It is not assumed, that a concrete class has further sub classes.
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
     * Get the partOf restrictions of a class. If no partOf restrictions are found an empty set is returned.
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
     * Checks if the given class is concrete. A class is concrete if: <ul> <li>If the class was already added to the
     * list of concrete classes (by its parents)</li> <li>The class has a direct partOf relation to one of the
     * stages.</li> <li>recursion</li> </ul> The class is not added to the list of concrete classes, only a boolean is
     * returned.
     *
     * @param start       The class to test
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
     * Checks if the given class is a concrete class. A class is concrete, if it was already added to the list of
     * concrete classes (through it parents), has a direct partOf relation to a developmental stage, or has a concrete
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
     * If the start class is a concrete class, the class is added to the result list. Otherwise
     * <code>findDownToConcrete</code> is called for each child of the start class and for each class connected. with
     * "hasPart".
     *
     * @param cls    The start class
     * @param result
     */
    private void findDownToConcrete(ObaClass cls, Set<ObaClass> result) {
        if (getConcreteClasses().contains(cls)) {
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
     * Returns a list of classes with a partOf relation to the given class. If no relations are found, <code>null</code>
     * is returned.
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
}
