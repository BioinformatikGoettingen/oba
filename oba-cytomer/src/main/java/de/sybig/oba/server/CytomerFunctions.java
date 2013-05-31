/*
 * Created on Apr 16, 2010
 *
 */
package de.sybig.oba.server;

import java.io.IOException;
import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.PathSegment;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CytomerFunctions extends OntologyFunctions implements
        OntologyFunction {

    private Logger log = LoggerFactory.getLogger(CytomerFunctions.class);
    private Properties cytomerProps;
    private ObaClass organCls;
    private Set<ObaClass> organList;
    private Set<ObaObjectPropertyExpression> organRestrictions;
    private ObaClass physiologicalSystemClass;
    private Set<ObaClass> nonOrgans;


    /**
     * A class providing ontology functions specific for Cytomer.
     *
     */
    public CytomerFunctions() {
        super();
        cytomerProps = new Properties();
        try {
            cytomerProps.load(getClass().getResourceAsStream(
                    "/cytomer.properties"));
        } catch (IOException e) {
            log.error("could not load properties for cytomer function class");
            e.printStackTrace();
        }
    }
    @Override
    public String getVersion(){
        return "1.3";
    }
    // @Override
    // public void setOntology(ObaOntology ontology) {
    // this.ontology = ontology;
    // }
    /**
     * Gets a short documentation of the implemented functions in html.
     */
    @GET
    @Path("/")
    @Produces("text/html")
    @Override
    public String getRoot() {
        StringBuffer out = new StringBuffer();
        out.append("<h1>Available functions</h1>\n");
        out.append("<dl>");
        out.append("<dt>/organList</dt><dd>Gets a list of all organs</dd>");
        out.append("<dt>/organsOf/{cls}</dt><dd>Gets a list of organs this class is associated with. The class may be a subclass of an organ or is connected to the organ through the restrictions 'isPartOf', 'isCellOf' and 'isPartOfOrgan'</dd>");
        out.append("<dt>/systemsOf/{cls}</dt><dd>similar to 'getOrgansOf' but returns the list of physiological systems a class belongs to.</dd>");
        out.append("<dt>/findUpstreamInSet/{cls}/{partition}/{set}</dt><dd>From the given starting class an upstream search is started until a class of the stored list is found. Besides of the class hierarchy the following relations are used: 'isPartOf', 'isCellOf' and 'isPartOfOrgan'. All classes found are returned.</dd>");
        out.append("<dt>/findDownstreamInSet/{cls}/{partition}/{set}</dt><dd>From the given starting class an downs search is started until a class of the stored list is found. Besides of the class hierarchy the following relations are used: 'hasPart', 'hasCell' and 'hasOrganPart'. All classes found are returned.</dd>");
        out.append("</dl>");
        return out.toString();
    }

    /**
     * Gets the list of organs defined in the ontology. This are the direct or
     * indirect children of the class 'organ'. From organ the class hierarchy is
     * descended until a class is found, that is not annotated with
     * 'abstract=true'. The list of organs may also contain organs specific for
     * embryonal stages.
     *
     * @return The list of organs
     */
    @GET
    @Path("/organList")
    @Produces("text/plain, text/html, application/json")
    @HtmlBase("../../cls/")
    public Set<ObaClass> getOrganList() {
        log.info("getting the list of organs");
        return getOrgans();
    }

    /**
     * Gets the organs the class belongs to. All ancestors of the class are
     * tested, if they belong to the set of organs ({@link CytomerFunctions#getOrgans()).
     *
     * @param cls
     * @param ns
     * @return
     */
    @GET
    @Path("/organsOf/{cls}")
    @Produces("text/plain, text/html, application/json")
    @HtmlBase("../../../cls/")
    public Set<ObaClass> getOrgansFor(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        // http://localhost:9998/cytomer/functions/cytomer/organsOf/left_medial_segment_of_liver?ns=http://cytomer.bioinf.med.uni-goettingen.de/organ#
        // cuboidal_epithelial_cell
        log.info("getting organs for {} in namespace {}", cls, ns);

        ObaClass startClass = ontology.getOntologyClass(cls, ns);
        if (startClass == null) {
            log.warn("The start class could not be found in the ontology");
            throw new WebApplicationException(404);
        }
        Set<ObaClass> organSet = findOrgans(startClass);
        log.info("organs found " + organSet.size());
        return organSet;
    }

    @GET
    @Path("/systemsOf/{cls}")
    @Produces("text/plain, text/html, application/json")
    @HtmlBase("../../../cls/")
    public Set<ObaClass> getsystemsFor(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        // http://localhost:9998/cytomer/functions/cytomer/organsOf/left_medial_segment_of_liver?ns=http://cytomer.bioinf.med.uni-goettingen.de#
        // cuboidal_epithelial_cell
        log.info("getting systems for {} in namespace {}", cls, ns);

        ObaClass startClass = ontology.getOntologyClass(cls, ns);
        if (startClass == null) {
            log.warn("The start class could not be found in the ontology");
            throw new WebApplicationException(404);
        }
        Set<ObaClass> organSet = findSystems(startClass);
        return organSet;
    }

    /**
     * Starting form the given class a upstream search is started until a class
     * stored in the referenced set is found. If the start class itself a direct
     * or indirect ancestor of it is in the reference set, this class is
     * returned. Otherwise the graph is searched along the properties retrieved
     * by the {@link #getSearchUpToSetRestrictions()} from the property file.
     *
     * <ul> <li>If the start class is not found a web exception with status code
     * 404 is thrown.</li> <li>If the reference is not found a web exception
     * with status code 404 is thrown.</li> <li>If no class is found in the
     * reference set an empty list is returned.</li> </ul>
     *
     * @param startCls The start class
     * @param partition The partition the list is stored in
     * @param set The name of the list with the stored classes
     * @return The matching class from the reference list
     */
    @GET
    @Path("/findUpstreamInSet/{cls}/{partition}/{set}")
    @Produces("text/plain, text/html, application/json")
    public Set<ObaClass> findUpstreamInSet(
            @PathParam("cls") PathSegment startCls,
            @PathParam("partition") String partition,
            @PathParam("set") String set) {
        ObaClass startClass;
        try {
            startClass = this.getClassFromPathSegement(startCls, null);
        } catch (IllegalArgumentException ex) {
            log.error("could not get the start class for 'findUpstreamInSet'");
            throw new WebApplicationException(404);
        }

        // is the class itself member of the refernce set?
        StorageHandler storageHandler = new StorageHandler();
        Set<ObaClass> referenceSet = storageHandler.getStorage(partition, set);

//        Set<ObaClass> resultSet = new HashSet<ObaClass>();
//        if (referenceSet.contains(startClass)) {
//            resultSet.add(startClass);
//            return resultSet;
//        }
//
//        // first test if we have an ancestors of a class of the set
//        Set<ObaClass> startClasses = new HashSet<ObaClass>();
//        startClasses.add(startClass);
//        resultSet = searchUpStreamToSet(startClasses, referenceSet);
//        if (resultSet.size() > 0) {
//            return resultSet;
//        }
//
//        // search along the properties
//        Set<OWLObjectProperty> searchRestrictions = getSearchUpToSetRestrictions();
//        Set<ObaClass> found = new HashSet<ObaClass>();
//        searchAlongRelationsToSet(startClasses, referenceSet, found,
//                searchRestrictions, true, false, new HashSet<ObaClass>());
        Set<ObaClass> startClasses = new HashSet<ObaClass>();
        startClasses.add(startClass);

        // search along the properties
        Set<OWLObjectProperty> searchRestrictions = getSearchUpToSetRestrictions();
        Set<ObaClass> found = new HashSet<ObaClass>();
        searchAlongRelationsToSet(startClasses, referenceSet, found,
                searchRestrictions, false, true, new HashSet<ObaClass>());
        return found;
    }

    /**
     * @see #findUpstreamInSet(PathSegment, String, String)
     * @param startCls
     * @param partition
     * @param set
     * @return
     */
    @GET
    @Path("/findDownstreamInSet/{cls}/{partition}/{set}")
    @Produces("text/plain, text/html, application/json")
    public Set<ObaClass> findDownstreamInSet(
            @PathParam("cls") PathSegment startCls,
            @PathParam("partition") String partition,
            @PathParam("set") String set) {
        ObaClass startClass;
        try {
            startClass = this.getClassFromPathSegement(startCls, null);
        } catch (IllegalArgumentException ex) {
            log.error("could not get the start class for 'findUpstreamInSet'");
            throw new WebApplicationException(400);
        }

        // is the class in the reference set?
        StorageHandler storageHandler = new StorageHandler();
        Set<ObaClass> referenceSet = storageHandler.getStorage(partition, set);
        Set<ObaClass> startClasses = new HashSet<ObaClass>();
        startClasses.add(startClass);

        // search along the properties
        Set<OWLObjectProperty> searchRestrictions = getSearchDownToSetRestrictions();
        Set<ObaClass> found = new HashSet<ObaClass>();
        searchAlongRelationsToSet(startClasses, referenceSet, found,
                searchRestrictions, true, false, new HashSet<ObaClass>());
        return found;
    }

    /**
     *
     * @param startClasses
     * @param searchRestrictions
     * @param referenceSet
     * @return
     */
    private void searchAlongRelationsToSet(Set<ObaClass> startClasses, Set<ObaClass> referenceSet, Collection<ObaClass> resultSet,
            Set<OWLObjectProperty> searchRestrictions, boolean includeChildren, boolean includeParents, Collection<ObaClass> visited) {

        Set<ObaClass> children = new HashSet<ObaClass>();
        for (ObaClass startClass : startClasses) {
            for (ObaObjectPropertyExpression relation : OntologyHelper.getObjectRestrictions(startClass)) {
                if (!searchRestrictions.contains(relation.getRestriction())) {
                    continue;
                }
                if (referenceSet.contains(relation.getTarget())) {
                    resultSet.add(relation.getTarget());
                }
                if (!visited.contains(relation.getTarget())) {
                    children.add(relation.getTarget());
                }
            }
            if (includeChildren) {
                for (ObaClass child : OntologyHelper.getChildren(startClass)) {
                    if (referenceSet.contains(child)) {
                        resultSet.add(child);
                    }
                    if (!visited.contains(child)) {
                        children.add(child);
                    }
                }
            } // if children
            if (includeParents) {
                for (ObaClass parent : OntologyHelper.getParents(startClass)) {
                    if (referenceSet.contains(parent)) {
                        resultSet.add(parent);
                    }

                    if (!visited.contains(parent)) {
                        children.add(parent);
                    }
                }
            } // if parents

        }// for start classes

//        if (resultSet.size() > 0 || children.size() < 1) {
//            return resultSet;
//        }
        visited.addAll(children);
        if (children.size() > 0) {
            searchAlongRelationsToSet(children, referenceSet, resultSet, searchRestrictions, includeChildren, includeParents, visited);
        }
    }

    /**
     * Searches downstream using the is_a relations. In each recursive call the
     * children of the classes in the list of classes given as first parameter
     * are tested. If at least one hit is found, a list of children which are
     * part of the reference set are returned. Otherwise the function is called
     * recursive starting with the children.
     *
     * @param startClasses
     * @param referenceSet
     * @return
     */
    private Set<ObaClass> searchDownStreamToSet(Set<ObaClass> startClasses,
            Set<ObaClass> referenceSet) {
        Set<ObaClass> resultSet = new HashSet<ObaClass>();
        Set<ObaClass> children = new HashSet<ObaClass>();

        for (ObaClass startClass : startClasses) {
            for (ObaClass c : OntologyHelper.getChildren(startClass)) {
                if (referenceSet.contains(c)) {
                    resultSet.add(c);
                } else {
                    children.add(c);
                }
            }
        }
        if (resultSet.size() > 0 || children.size() < 1) {
            return resultSet;
        }
        return searchDownStreamToSet(children, referenceSet);
    }

    /**
     * Searches upstream using the is_a relations
     *
     * @param startClasses
     * @param referenceSet
     * @return
     */
    private Set<ObaClass> searchUpStreamToSet(Set<ObaClass> startClasses,
            Set<ObaClass> referenceSet) {
        Set<ObaClass> resultSet = new HashSet<ObaClass>();
        Set<ObaClass> parents = new HashSet<ObaClass>();

        for (ObaClass startClass : startClasses) {
            for (ObaClass c : OntologyHelper.getParents(startClass)) {
                if (referenceSet.contains(c)) {
                    resultSet.add(c);
                } else {
                    parents.add(c);
                }
            }
        }
        if (resultSet.size() > 0 || parents.size() < 1) {
            return resultSet;
        }
        return searchUpStreamToSet(parents, referenceSet);
    }

    protected Set<ObaClass> getOrgans() {
        if (organList == null) {
            Set<ObaClass> organs = new HashSet<ObaClass>();
            OWLClass organRoot = getOrganCls();
            Set<ObaClass> children = OntologyHelper.getChildren(organRoot,
                    ontology.getOntology());

            organs.addAll(findConcreteOrgans(children));
            organList = organs;
        }
        return organList;
    }

    private HashSet<ObaClass> findConcreteOrgans(Set<ObaClass> classes) {
        HashSet<ObaClass> organs = new HashSet<ObaClass>();
        clsloop:
        for (ObaClass cls : classes) {
            Set<ObaAnnotation> annotations = OntologyHelper.getAnnotationProperties(cls, ontology.getOntology());

            for (ObaAnnotation annotation : annotations) {
                if ("abstract".equals(annotation.getName())) {
                    if (annotation.getValue().equals("false")) {
                        organs.add(cls);
                    } else {
                        Set<ObaClass> children = OntologyHelper.getChildren(
                                cls, ontology.getOntology());
                        organs.addAll(findConcreteOrgans(children));
                    }
                    continue clsloop;
                }
            } // for annotations
            organs.add(cls);
        } // for classes
        return organs;
    }

    //
    private boolean isClsOrgan(OWLClass cls) {
        return getOrgans().contains(cls);
    }

    private boolean isClsPhysiologicalSystem(ObaClass cls) {
        Set<ObaClass> parents = OntologyHelper.getParents(cls);
        if (parents == null) {
            return false;
        }
        if (parents.contains(getPhysiologicalSystemCls())) {
            return true;
        }
        return false;
    }

    public ObaClass getOrganCls() {
        if (organCls == null) {
            organCls = ontology.getOntologyClass(cytomerProps.getProperty(
                    "organ_name", "organ"), cytomerProps.getProperty(
                    "organ_ns",
                    "http://cytomer.bioinf.med.uni-goettingen.de#"));
            //"http://protege.stanford.edu/plugins/owl/protege#"));
            log.info("getting organ cls with {} in NS {}",
                    cytomerProps.getProperty("organ_name"),
                    cytomerProps.getProperty("organ_ns"));
        }
        return organCls;
    }

    public ObaClass getPhysiologicalSystemCls() {
        if (physiologicalSystemClass == null) {
            physiologicalSystemClass = ontology.getOntologyClass(cytomerProps.getProperty("physiological_system_name",
                    "physiological_system"), cytomerProps.getProperty(
                    "physiological_system_ns",
                    "http://protege.stanford.edu/plugins/owl/protege#"));
        }
        return physiologicalSystemClass;
    }

    /**
     * Searches the organs the entity belongs to. First it is tested, if the
     * start class is a direct successor of the organ class. In this case the
     * real organ is searched in the paths from the start class to the organ
     * class. The found organs are returned and the search is stopped.
     *
     * If the start class is not a descendant of the organ class, the start
     * class is expanded upstream using the object property restrictions
     *
     * @param startClass
     * @return
     */
    private Set<ObaClass> findOrgans(ObaClass startClass) {
        Set<ObaClass> organSet = upstreamOrgans(startClass);
        if (organSet.size() > 0) {
            return organSet;
        }
        Set<ObaObjectPropertyExpression> restrictions = OntologyHelper.getObjectRestrictions(startClass, ontology.getOntology());
        for (ObaObjectPropertyExpression restriction : restrictions) {
            if (isOrganRestriction(restriction.getRestriction())) {
                Set<ObaClass> foundOrgans = findOrgans(restriction.getTarget());
                if (foundOrgans != null) {
                    organSet.addAll(foundOrgans);
                }
            }
        }
        return organSet;
    }

    private Set<ObaClass> upstreamOrgans(ObaClass startClass) {
        Set<ObaClass> organSet = new HashSet<ObaClass>();
        // search organs in the class hierarchies
        if (getNonOrgans().contains(startClass)) {
            return organSet;
        }
        List<List<ObaClass>> pathsToOrgan = searchXdownstreamOfY(startClass,
                getOrganCls());
        if (pathsToOrgan != null && pathsToOrgan.size() > 0) {
            for (List<ObaClass> path : pathsToOrgan) {
                for (ObaClass cls : path) {
                    if (isClsOrgan(cls)) {
                        organSet.add(cls);
                    }
                }
            }
        } else {
            List<List<ObaClass>> pathsToRoot = getAllPathsToRoot(startClass);
            for (List<ObaClass> path : pathsToRoot) {
                getNonOrgans().addAll(path);
            }

            getNonOrgans().add(startClass);
        }
        return organSet;
    }

    private Set<ObaClass> getNonOrgans() {
        if (nonOrgans == null) {
            nonOrgans = new HashSet<ObaClass>();
        }
        return nonOrgans;
    }
    // merge with findOrgans?

    private Set<ObaClass> findSystems(ObaClass startClass) {
        Set<ObaClass> systemSet = new HashSet<ObaClass>();
        List<List<ObaClass>> pathsToSystem = searchXdownstreamOfY(startClass,
                getPhysiologicalSystemCls());
        if (pathsToSystem != null) {
            for (List<ObaClass> path : pathsToSystem) {
                for (ObaClass cls : path) {
                    if (isClsPhysiologicalSystem(cls)) {
                        systemSet.add(cls);
                    }
                }
            }
        }
        Set<ObaObjectPropertyExpression> restrictions = OntologyHelper.getObjectRestrictions(startClass, ontology.getOntology());
        for (ObaObjectPropertyExpression restriction : restrictions) {
            if (isOrganRestriction(restriction.getRestriction())) {  // organRestrictions?
                if (restriction.getTarget().equals(startClass)) {
                    // we have a loop
                    continue;
                }
                Set<ObaClass> foundSystems = findSystems(restriction.getTarget());
                if (foundSystems != null) {
                    systemSet.addAll(foundSystems);
                }
            }
        }

        return systemSet;
    }

    private boolean isOrganRestriction(OWLObjectProperty prop) {
        if (getOrganRestrictions().contains(prop)) {
            return true;
        }
        return false;
    }

    private Set<OWLObjectProperty> getOrganRestrictions() {
        // if (!organRestrictionsMap.containsKey(ontology)) {
        String organRestrictionsString = cytomerProps.getProperty(
                "organ_restrictions", "isPartOf, isCellOf, isPartOfOrgan");
        return getRestrictionsSet(organRestrictionsString);
        // }
    }

    private Set<OWLObjectProperty> getSearchUpToSetRestrictions() {
        String restrictionsString = cytomerProps.getProperty(
                "set_upstream_restrictions",
                "isPartOf, isCellOf, isPartOfOrgan");
        return getRestrictionsSet(restrictionsString);
    }

    private Set<OWLObjectProperty> getSearchDownToSetRestrictions() {
        String restrictionsString = cytomerProps.getProperty("set_downstream_restrictions",
                "hasPart, hasCell, hasOrganPart");
        return getRestrictionsSet(restrictionsString);
    }

    private Set<OWLObjectProperty> getRestrictionsSet(String restrictionsString) {
        String trimmedRestrictionsString = restrictionsString.replace(" ", "");
        String[] restrictionsArray = trimmedRestrictionsString.split(",");
        Set<OWLObjectProperty> restrictions = OntologyHelper.getObjectProperties(ontology.getOntology(),
                Arrays.asList(restrictionsArray));
        // organRestrictionsMap.put(ontology, organRestrictions);
        // Set<OWLObjectProperty> r = organRestrictionsMap.get(ontology);
        return restrictions;
    }
}
