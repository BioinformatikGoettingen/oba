package de.sybig.oba.server;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObaOntology {

    private OWLOntology onto;
    private Properties properties;
    private static final int MAX_HITS = 500;
    private final Version luceneVersion = Version.LUCENE_24;
    private IRI iri;
    private RAMDirectory idx;
    private OWLDataFactory dataFactory;
    private OWLOntologyManager manager;
    private final Set<ObaClass> orphanChildren = new HashSet<>();
    private final Set<ObaClass> obsoleteClasses = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(ObaOntology.class);
    private List<String> indexAnnotations;
    private final String OBSOLTE = "is_obsolete";
    private final String DEPRECATED = "deprecated";
    private List<ObaClass> allClasses;

    public void setOwlURI(IRI uri) {
        this.iri = uri;
    }

    public OWLOntology getOntology() {
        return onto;
    }

    /**
     * Get the properties used to load the ontology. This properties are usually
     * read from a file in the ontology directory.
     *
     * @return the properties for the ontology
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties that are used to load this ontology.
     *
     * @param properties the properties for the ontology
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Inits the ontology. I.e. the ontology is parsed and the fields of the
     * ontology that are specified in the properties are indexed.
     *
     * @throws OWLOntologyCreationException
     */
    public synchronized void init() throws OWLOntologyCreationException {
        if (iri == null) {
            logger.warn("The IRI must not be null when initiation an ontology");
            throw new OWLOntologyCreationException();
        }
        if (onto != null) {
            logger.warn("Ontology is already initialized");
            return;
        }

        manager = OWLManager.createOWLOntologyManager();
        logger.info("Loading ontology {}", iri);
        onto = manager.loadOntologyFromOntologyDocument(iri);
        dataFactory = manager.getOWLDataFactory();
        idx = new RAMDirectory();
        try {
            scanClasses(onto);
        } catch (CorruptIndexException e) {
            logger.error("Could not index classes from ontology %s", iri, e);
            throw new OWLOntologyCreationException();
        } catch (LockObtainFailedException e) {
            logger.error("Could not index classes from ontology %s", iri, e);
            throw new OWLOntologyCreationException();
        } catch (IOException e) {
            logger.error("Could not index classes from ontology %s", iri, e);
            throw new OWLOntologyCreationException();
        }
    }

    /**
     * Get the root node of the ontology. The root is returned as proxy from
     * Type ObaClass with the ontology set.
     *
     * @return The root of the ontology.
     */
    public ObaClass getRoot() {
        // the root could also be retrieved directly from the ontology with
        // ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing()'
        OWLClass owlRoot = dataFactory.getOWLThing();
        ObaClass root = new ObaClass(owlRoot, onto);

        return root;
    }

    /**
     * Returns classes without explicit super classes. If a class does not have
     * a explicit super classes the API won't return it by
     * {@link OWLClass#getSuperClasses(org.semanticweb.owlapi.model.OWLOntology)}
     * . So the classes are scanned during {@link #init()} and orphaned children
     * are stored.
     *
     * @return
     */
    public Set<ObaClass> getOrphanChildren() {
        return orphanChildren;
    }

    /**
     * Get a list of all obsolete classes. In OBO ontologies, deleted classes
     * are annotated with "is_obsolete = true". During the initial scan all
     * obsolete classes are found and stored. If the children of the root node
     * are queried later on, obsolete classes are subtracted.
     *
     * @return
     */
    public Set<ObaClass> getObsoleteClasses() {
        return obsoleteClasses;
    }

    /**
     * Gets a class from the ontology. If no class with this name in this
     * namespace is found, <code>null</code> is returned. A trailing '#' on the
     * namespace will be deleted before processing.
     *
     * @param cls The name of the class.
     * @param ns The namespace of the class.
     * @return The class object or <code>null</code>
     */
    public ObaClass getOntologyClass(final String cls, final String ns) {
        String namespace = ns != null ? ns : onto.getOntologyID().getOntologyIRI().toString();

        if (namespace.endsWith("#")) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        IRI queryIri = getIri(namespace, cls);
        OWLClass c = getExistingClassFromFactory(queryIri);
        if (c == null){
            if (! namespace.endsWith("/")){
                namespace = namespace +"/";
            }
            queryIri = getIri(namespace, cls, false);
            c  = getExistingClassFromFactory(queryIri);
        }
        if (c == null){
            return null;
        }
        return new ObaClass(c, onto);
    }

    private OWLClass getExistingClassFromFactory(IRI queryIri) {
        OWLClass c = dataFactory.getOWLClass(queryIri);
        if (getOntologyForClass(c) == null && !c.isOWLThing()) {
            // if the class is not in the ontology dataFactory.getOWLClass will
            // return a new class. So we check if the returned class is part of
            // the ontology.
            return null;
        }
        return c;
    }

    /**
     * Get the object properties (relationships between classes) used in this
     * ontology.
     *
     * @return the object properties of the ontology.
     */
    public Set<OWLObjectProperty> getObjectProperties() {
        Set<OWLObjectProperty> restrictions = onto.getObjectPropertiesInSignature();
        return restrictions;
    }

    public OWLObjectProperty getObjectPropertyByName(String name, String ns) {
        String namespace = ns != null ? ns : onto.getOntologyID().getOntologyIRI().toString();

        if (namespace.endsWith("#")) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(getIri(
                namespace, name));
        return property;
    }

    public Set<OWLDataProperty> getDataProperties() {
        return onto.getDataPropertiesInSignature();
    }

    public OWLDataProperty getDataPropertyByName(String name, String ns) {
        String namespace = ns != null ? ns : onto.getOntologyID().getOntologyIRI().toString();
        if (namespace.endsWith("#")) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        return dataFactory.getOWLDataProperty(getIri(namespace, name));
    }

    public OWLOntology getOntologyForClass(OWLClass c) {

        if (c instanceof ObaClass) {
            if (onto.getClassesInSignature().contains(((ObaClass) c).getReal())) {
                return onto;
            }
        } else {
            if (onto.getClassesInSignature().contains(c)) {
                return onto;
            }
        }
        return null;
    }

    /**
     * Searches a class in the ontology. The pattern is searched in the indexed
     * class names. The returned classes are sorted by their relevance. If no
     * matches could be found an empty list is returned.
     *
     * @param pattern The search pattern
     * @return An ordered list of classes matching the pattern.
     */
    public List<ObaClass> searchCls(String pattern, String fields) {

        return searchInIndex(pattern, fields, MAX_HITS);
    }

    public List<ObaClass> searchCls(String pattern, String fields, int maxResults) {

        return searchInIndex(pattern, fields, maxResults);
    }

    public List<ObaClass> getClasses() {
        if (allClasses == null) {
            allClasses = new ArrayList<>();
            Deque<ObaClass> unprocessed = new ArrayDeque<>();
            unprocessed.add(getRoot());

            while (!unprocessed.isEmpty()) {
                ObaClass node = unprocessed.pop();
                if (!allClasses.contains(node)) {
                    allClasses.add(node);
                }
                unprocessed.addAll(OntologyHelper.getChildren(node, getOntology()));
            }
        }
        return allClasses;
    }

    public OWLDataFactory getDataFactory() {
        return dataFactory;
    }

    protected OWLOntology getOntologyForProperty(OWLObjectProperty c) {
        if (onto.getObjectPropertiesInSignature().contains(c)) {
            return onto;
        }
        return null;
    }

    private IRI getIri(String ns, String name) {
        return getIri(ns, name, true);
    }

    private IRI getIri(String ns, String name, boolean withFragment) {
        if (withFragment) {
            return IRI.create(String.format("%s#%s", ns, name));
        }
        return IRI.create(String.format("%s%s", ns, name));
    }

    /**
     * Walk over all ontology classes and index them. Obsolete classes are
     * ignored.
     *
     * @param ontology
     * @throws CorruptIndexException
     * @throws LockObtainFailedException
     * @throws IOException
     */
    private void scanClasses(OWLOntology ontology)
            throws CorruptIndexException, LockObtainFailedException,
            IOException {
        try (IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(
                luceneVersion), true, IndexWriter.MaxFieldLength.UNLIMITED)) {
            Set<OWLDeclarationAxiom> classes = ontology.getAxioms(AxiomType.DECLARATION);
            int counter = 0;
            clsLoop:
            for (OWLDeclarationAxiom c : classes) {

                OWLEntity entity = c.getSignature().iterator().next();
                if (!(entity instanceof OWLClass)) {
                    // skip AnnotationProperties, ObjectProperties, DataProperites
                    continue;
                }
                OWLClass cls = (OWLClass) entity;
                if (cls.getSuperClasses(ontology) == null
                        || cls.getSuperClasses(ontology).size() < 1) {
                    for (ObaAnnotation annotation : OntologyHelper.getAnnotationProperties(cls, onto)) {
                        if (annotation.getName() != null
                                && (annotation.getName().equals(OBSOLTE) || annotation.getName().equals(DEPRECATED))
                                && annotation.getValue().equals("true")) {
                            obsoleteClasses.add(new ObaClass(cls, ontology));
                            continue clsLoop;
                        }
                    }
                    orphanChildren.add(new ObaClass(cls, ontology));
                } // if
                counter++;
                Document doc = createDocumente(cls);
                writer.addDocument(doc);

            }
            if (!dataFactory.getOWLThing().isDefined(ontology)) {
                logger.info("The ontology have no root class defined, creating one");
                OWLClass r = dataFactory.getOWLClass(IRI.create("http://www.w3.org/2002/07/owl" + "#Thing"));

                for (OWLClass c : orphanChildren) {
                    OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(c, r);

                    AddAxiom addAxiom = new AddAxiom(ontology, axiom);
                    manager.applyChange(addAxiom);
                }
            }
            orphanChildren.removeAll(obsoleteClasses);
            logger.info("indexed {} classes", counter);
            logger.info("found {} orphan classes ", orphanChildren.size());
            writer.optimize();
        }
    }

    private Document createDocumente(OWLClass cls) {
        List<String> indexAnnotation = getAnnotationsToIndex();
        Document doc = new Document();
        String name;
        String value;
        doc.add(new Field("luceneName", cls.getIRI().toString(), Store.YES,
                Index.ANALYZED));
        name = "classname";
        value = cls.getIRI().getFragment();
        if (value == null) {
            value = cls.getIRI().toString().replace(cls.getIRI().getStart(), "");
            value = removeSpecialChars(value);
        }
        doc.add(new Field(name, value, Store.NO, Index.ANALYZED));
        if (indexAnnotation != null && indexAnnotation.size() > 0) {
            for (ObaAnnotation annotation : OntologyHelper.getAnnotationProperties(cls, onto)) {
                if (indexAnnotation.contains(annotation.getName())) {
                    name = annotation.getName();
                    value = removeSpecialChars(annotation.getValue());
                    doc.add(new Field(name, value, Store.NO, Index.ANALYZED));
                }
            }
        }
        return doc;
    }

    private String removeSpecialChars(String orig) {
        String out = orig.replaceAll("-", "");
        out = out.replaceAll("_", "");
        return out;
    }

    /**
     * Searches the pattern in the luncene index. <code>null</code> is returned
     * if any exception occurs.
     *
     * @param searchPattern
     * @param fields
     * @return A list of matches or <code>null</code>
     */
    private List<ObaClass> searchInIndex(String searchPattern, String fields, int maxResults) {
        if (idx == null) {
            logger.error("could not search, because the index is null");
            return null;
        }
        // The pattern is split by white spaces and a term should not start with a wildcard.
        searchPattern = searchPattern.replaceAll(" \\*", "\\*");
        searchPattern = removeSpecialChars(searchPattern);
        String[] searchFields;
        List<String> fieldList = null;
        if (fields == null) {
            fieldList = new LinkedList<>();
            fieldList.add("classname");
            fieldList.addAll(getAnnotationsToIndex());

        } else {
            fieldList = Arrays.asList(fields.split(","));
        }
        searchFields = fieldList.toArray(new String[fieldList.size()]);

        List<ObaClass> outClasses = new ArrayList<>();
        Searcher searcher;
        try {
            searcher = new IndexSearcher(idx);
            QueryParser parser = new MultiFieldQueryParser(luceneVersion,
                    searchFields, new StandardAnalyzer(luceneVersion));
            parser.setDefaultOperator(Operator.AND);
            Query query = parser.parse(searchPattern);
            logger.debug("Search pattern: {} ; search query {}", searchPattern, query);
            TopDocs hits = searcher.search(query, maxResults);
            // List<Cls> outClasses = new LinkedList<Cls>();
            if (hits.totalHits > maxResults) {
                logger.warn(
                        "More than {} [{}] hits found for " + searchPattern,
                        maxResults, hits.totalHits);
            }
            for (int x = 0; x < hits.totalHits && x < maxResults; x++) {
                Document doc = searcher.doc(hits.scoreDocs[x].doc);
                String className = doc.getField("luceneName").stringValue();
                OWLClass cls = dataFactory.getOWLClass(IRI.create(className));
                outClasses.add(new ObaClass(cls, onto));
            }
            return outClasses;
            // return outClasses;
        } catch (CorruptIndexException e) {
            logger.error("could not search with lucene because the index is corrupt " + e);
        } catch (IOException e) {
            logger.error("could not search with lucene because of an io error " + e);
        } catch (ParseException e) {
            logger.error("parse error while searching in lucene " + e);
        }

        return null;
    }

    private List<String> getAnnotationsToIndex() {
        if (indexAnnotations == null) {
            indexAnnotations = new LinkedList<>();
            if (properties == null) {
                return indexAnnotations;
            }
            String annotations = properties.getProperty("indexAnnotations");
            if (annotations != null) {

                String[] annotationsArray = annotations.split(",");
                indexAnnotations = Arrays.asList(annotationsArray);
            }
        }
        return indexAnnotations;
    }
}
