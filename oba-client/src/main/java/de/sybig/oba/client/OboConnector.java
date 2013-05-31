/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

/**
 *
 * @author jdo
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
