package de.sybig.oba.client;

/**
 * This connector converts the ontology clases from the server to the Obo
 * classes. THis connecotr is mostly used to inherited from connectors using
 * ontologies in the OBO format.
 *
 * @author juergen.doenitz@ibeetle-base.uni-goettingen.de
 */
public class OboConnector extends GenericConnector<OboClass, OboClassList, Obo2DClassList> {

    public OboConnector(String ontology) {
        super(ontology);
    }

    @Override
    protected Class getOntologyClass() {
        return (Class) OboClass.class;
    }

    @Override
    protected Class getOntologyClassList() {
        return (Class) OboClassList.class;
    }

    @Override
    protected Class getOntology2DClassList() {
        return Obo2DClassList.class;
    }
}
