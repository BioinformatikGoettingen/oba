/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonEntity;
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
public class OboClassList extends AbstractOntologyClassList<OboClass> {
public OboClassList() {
		super();
	}

	public OboClassList(JsonClsList c) {
		fillWithTemplate(c);
	}

	@Override
	public List<OboClass> getEntities() {

		if (_entities == null) {
			return null;
		}
		if (ontologyEntities == null) {
			ontologyEntities = new LinkedList<OboClass>();

			for (JsonEntity c : _entities) {
				if (c instanceof OboClass) {
					ontologyEntities.add((OboClass) c);
					continue;
				}
				OboClass nc = (OboClass) new OboClass((JsonCls) c);
				nc.setConnector(connector);
				ontologyEntities.add(nc);
			}
		}
		return ontologyEntities;
	}

	public OboClassList clone() {
		OboClassList newList = new OboClassList();
		newList.setRawEntities(getRawEntities());
		newList.setConnector(connector);
		return newList;
	}

	private void fillWithTemplate(JsonClsList list) {
		this._entities = list.getEntities();
	}
}
