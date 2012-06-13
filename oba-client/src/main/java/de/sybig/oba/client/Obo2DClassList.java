/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

import de.sybig.oba.server.JsonClsList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
@XmlType
@XmlRootElement
public class Obo2DClassList extends AbstractOntology2DClassList<OboClassList, OboClass>{

    
    private List<OboClassList> listEntities;

	@Override
	public List getEntities() {
		if (_entities == null) {
			if (entities == null) {
				return null;
			}
			_entities = entities;
		}
		if (listEntities == null) {
			listEntities = new LinkedList<OboClassList>();
			for (JsonClsList c : _entities) {
				if (c instanceof OntologyClassList) {
					listEntities.add((OboClassList) c);
				}
				OboClassList nc = (OboClassList) new OboClassList(
						(JsonClsList) c);
				listEntities.add(nc);
			}
		}
		return listEntities;
	}
}
