package de.sybig.oba.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	private Set<ObaClass> orphanChildren = new HashSet<ObaClass>();

	private Logger logger = LoggerFactory.getLogger(ObaOntology.class);
	private List<String> indexAnnotations;

	// private boolean initializing = false;

	public void setOwlURI(IRI uri) {
		this.iri = uri;
	}

	public OWLOntology getOntology() {
		return onto;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public synchronized void init() throws OWLOntologyCreationException {
		if (iri == null) {
			throw new OWLOntologyCreationException();
		}
		if (onto != null) {
			System.out.println("already initialized");
			return;
		}
		// while(initializing) {
		// try {
		// Thread.sleep(100);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		// initializing = true;
		manager = OWLManager.createOWLOntologyManager();

		onto = manager.loadOntologyFromOntologyDocument(iri);
		dataFactory = manager.getOWLDataFactory();
		idx = new RAMDirectory();
		try {
			scanClasses(onto);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
			throw new OWLOntologyCreationException();
		} catch (LockObtainFailedException e) {
			e.printStackTrace();
			throw new OWLOntologyCreationException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OWLOntologyCreationException();
		}
		// initializing = false;
	}

	/**
	 * Get the root node of the ontology. The root is returned as proxy from
	 * Type ObaClass with the ontology set.
	 * 
	 * @return The root of the ontology.
	 */
	public ObaClass getRoot() {
		// the root could also be retrieved directly from the ontology with '
		// ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing()'
		OWLClass owlRoot = dataFactory.getOWLThing();
		ObaClass root = new ObaClass(owlRoot, onto);

		// if (!root.isDefined(onto)) {
		// // dataFactory will return always a class with the iri for
		// // owl:Thing, also the class is not defined in the ontology. Later
		// // on we need to get the ontology for the orphan root node, to get
		// // the (orphan) children. So save the root node and use it in
		// // #getOntologyForClass
		// this.rootNode = root;
		// }
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
	 * Gets a class from the ontology. If no class with this name in this
	 * namespace is found, <code>null</code> is returned. A trailing '#' on the
	 * namespace will be deleted before processing.
	 * 
	 * @param cls
	 *            The name of the class.
	 * @param ns
	 *            The namespace of the class.
	 * @return The class object or <code>null</code>
	 */
	public ObaClass getOntologyClass(final String cls, final String ns) {
		String namespace = ns != null ? ns : onto.getOntologyID()
				.getOntologyIRI().toString();

		if (namespace.endsWith("#")) {
			namespace = namespace.substring(0, namespace.length() - 1);
		}
		// if (!namespace.endsWith("/")) {
		// namespace = namespace+"/";
		// }
		OWLClass c = dataFactory.getOWLClass(getIri(namespace, cls));

		if (getOntologyForClass(c) == null && !c.isOWLThing()) {
			// if the class is not in the ontology dataFactory.getOWLClass will
			// return a new class. So we check if the returned class is part of
			// the ontology.
			return null;
		}
		return new ObaClass(c, onto);
	}

	//
	// public OWLNamedIndividual getIndividual(String ns, String name) {
	// OWLNamedIndividual individual = dataFactory
	// .getOWLNamedIndividual(getIri(ns, name));
	// return individual;
	// }

	public Set<OWLObjectProperty> getObjectProperties() {
		Set<OWLObjectProperty> restrictions = onto
				.getObjectPropertiesInSignature();
		return restrictions;
	}

	public OWLObjectProperty getPropertyByName(String name, String ns) {
		String namespace = ns != null ? ns : onto.getOntologyID()
				.getOntologyIRI().toString();

		if (namespace.endsWith("#")) {
			namespace = namespace.substring(0, namespace.length() - 1);
		}
		OWLObjectProperty property = dataFactory.getOWLObjectProperty(getIri(
				namespace, name));
		return property;
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

	protected OWLOntology getOntologyForProperty(OWLObjectProperty c) {
		if (onto.getObjectPropertiesInSignature().contains(c)) {
			return onto;
		}
		return null;
	}

	/**
	 * Searches a class in the ontology. The pattern is searched in the indexed
	 * class names. The returned classes are sorted by their relevance. If no
	 * matches could be found an empty list is returned.
	 * 
	 * @param pattern
	 *            The search pattern
	 * @return An ordered list of classes matching the pattern.
	 */
	public List<OWLClass> searchCls(String pattern, String fields) {

		return searchInIndex(pattern, fields);
	}

	private IRI getIri(String ns, String name) {
		return IRI.create(String.format("%s#%s", ns, name));
	}

	private void scanClasses(OWLOntology ontology)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		IndexWriter writer = new IndexWriter(idx, new StandardAnalyzer(
				luceneVersion), true, IndexWriter.MaxFieldLength.UNLIMITED);

		Set<OWLDeclarationAxiom> classes = ontology
				.getAxioms(AxiomType.DECLARATION);

		for (OWLDeclarationAxiom c : classes) {

			OWLEntity entity = c.getSignature().iterator().next();
			if (!(entity instanceof OWLClass)) {
				// skip AnnotationProperties, ObjectProperties, DataProperites
				continue;
			}
			OWLClass cls = (OWLClass) entity;
			if (cls.getSuperClasses(ontology) == null
					|| cls.getSuperClasses(ontology).size() < 1) {
				orphanChildren.add(new ObaClass(cls, ontology));
			}

			Document doc = createDocumente(cls);
			writer.addDocument(doc);

		}
		if (!dataFactory.getOWLThing().isDefined(ontology)) {
			logger.info("The ontology have no root class defined, creating one");
			OWLClass r = dataFactory.getOWLClass(IRI
					.create("http://www.w3.org/2002/07/owl" + "#Thing"));

			for (OWLClass c : orphanChildren) {
				OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(c, r);

				AddAxiom addAxiom = new AddAxiom(ontology, axiom);
				manager.applyChange(addAxiom);
			}
		}

		logger.info("found {} orphan classes ", orphanChildren.size());
		writer.optimize();
		writer.close();
	}

	private Document createDocumente(OWLClass cls) {
		List<String> indexAnnotation = getAnnotationsToIndex();
		Document doc = new Document();
		String name;
		String value;
		doc.add(new Field("luceneName", cls.getIRI()
				.toString(), Store.YES, Index.ANALYZED));
		name = "classname";
		value = cls.getIRI().getFragment();
		doc.add(new Field(name, value, Store.NO, Index.ANALYZED));
		if (indexAnnotation != null && indexAnnotation.size() > 0) {
			for (ObaAnnotation annotation : OntologyHelper
					.getAnnotationProperties(cls, onto)) {
				if (indexAnnotation.contains(annotation.getName())) {
					name = annotation.getName();
					value = annotation.getValue();
					doc.add(new Field(name, value, Store.NO, Index.ANALYZED));
				}
			}
		}
		return doc;

	}

	private List<OWLClass> searchInIndex(String searchPattern, String fields) {
		if (idx == null) {
			logger.error("could not search, because the index is null");
			return null;
		}
		String[] searchFields;
		List<String> fieldList = null;
		if (fields == null) {
			fieldList = new LinkedList<String>();
			fieldList.add("classname");
			fieldList.addAll(getAnnotationsToIndex());

		} else {
			fieldList = Arrays.asList(fields.split(","));
		}
		searchFields = fieldList.toArray(new String[fieldList.size()]);

		List<OWLClass> outClasses = new ArrayList<OWLClass>();
		Searcher searcher;
		try {
			searcher = new IndexSearcher(idx);

			QueryParser parser = new MultiFieldQueryParser(luceneVersion,
					searchFields, new StandardAnalyzer(luceneVersion));
			parser.setDefaultOperator(Operator.AND);
			System.out.println(searchPattern);
			Query query = parser.parse(searchPattern);
			TopDocs hits = searcher.search(query, MAX_HITS);
			// List<Cls> outClasses = new LinkedList<Cls>();
			if (hits.totalHits > MAX_HITS) {
				logger.warn(
						"More than {} [{}] hits found for " + searchPattern,
						MAX_HITS, hits.totalHits);
			}
			for (int x = 0; x < hits.totalHits && x < MAX_HITS; x++) {
				Document doc = searcher.doc(hits.scoreDocs[x].doc);
				// System.out.println(hits.scoreDocs[x].score);
				String className = doc.getField("luceneName").stringValue();
				OWLClass cls = dataFactory.getOWLClass(IRI.create(className));
				outClasses.add(new ObaClass(cls, onto));
			}
			return outClasses;
			// return outClasses;
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private List<String> getAnnotationsToIndex() {
		if (indexAnnotations == null) {
			indexAnnotations = new LinkedList<String>();
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
