/*
 * Created on May 4, 2010
 *
 */
package de.sybig.oba.client;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.WebResource;

import de.sybig.oba.server.Json2DClsList;
import de.sybig.oba.server.JsonCls;

/**
 * A connector for the oba ontology server specific for cytomer. The connector
 * implements the functions of the oba service specific for the Cytomer ontology
 * and inherits the basic functions from the {@link GenericConnector}.
 * 
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * 
 */

public class CytomerConnector
		extends
		GenericConnector<CytomerClass, CytomerClassList, Json2DClsList<CytomerClassList, CytomerClass>> {

	protected final String SUB_RESOURCE = "functions/cytomer";
	private Logger logger = LoggerFactory.getLogger(CytomerConnector.class);

	public CytomerConnector() {
		super("cytomer");
	}

	/**
	 * Get the list of organs from the ontology.
	 * 
	 * @return All organs.
	 */
	public CytomerClassList getOrganList() {
		String path = String.format("%s/%s/organList", getOntology(),
				SUB_RESOURCE);
		WebResource webResource = getWebResource().path(path);

		CytomerClassList organlist = getResponse(webResource,
				CytomerClassList.class);
		organlist.setConnector(this);
		return organlist;
	}

	/**
	 * Get the organs the given anatomical entity belongs to.
	 * 
	 * @see CytomerFunctions#getOrgansFor(String, String)
	 * @param cls
	 *            The ontology class for which the organs should be searched.
	 * @return A list of organs the given ontology class belongs to.
	 */
	public CytomerClassList getOrgansForClass(JsonCls cls) {
		String path = String.format("%s/%s/organsOf/%s", getOntology(),
				SUB_RESOURCE, cls.getName());
		WebResource webResource = getWebResource().path(path);
		webResource = webResource.queryParam("ns", cls.getNamespace());
		return getResponse(webResource, CytomerClassList.class);
	}

	/**
	 * 
	 * @param cls
	 * @param partition
	 * @param listName
	 * @return
	 */
	public CytomerClassList findUpstreamInSet(JsonCls cls, String partition,
			String listName) {
		return findUpstreamInSet(cls.getName(), cls.getNamespace(), partition,
				listName);
	}

	/**
	 * 
	 * @param clsName
	 * @param clsNS
	 * @param partition
	 * @param listName
	 * @return
	 */
	public CytomerClassList findUpstreamInSet(String clsName, String clsNS,
			String partition, String listName) {
		// /findUpstreamInSet/{cls}/{partition}/{set}
		String path = String.format("%s/%s/findUpstreamInSet/", getOntology(),
				SUB_RESOURCE);
		WebResource webResource = getWebResource();
		UriBuilder uriBuilder = webResource.getUriBuilder();
		uriBuilder = uriBuilder.path(path);
		uriBuilder = uriBuilder.segment(clsName);
		if (clsNS != null && clsNS.trim().length() > 0) {
			String namespace = clsNS.replace("/", "$");
			uriBuilder = uriBuilder.matrixParam("ns", namespace);
		}
		uriBuilder = uriBuilder.segment(partition);
		uriBuilder = uriBuilder.segment(listName);
		URI uri = uriBuilder.build();
		webResource = webResource.uri(uri);
		CytomerClassList list = (CytomerClassList) webResource.accept(
				MediaType.APPLICATION_JSON).get(CytomerClassList.class);
		list.setConnector(this);
		return list;
	}

	@Override
	protected Class getOntologyClass() {
		return (Class) CytomerClass.class;
	}

	protected Class getOntologyClassList() {
		return (Class) CytomerClassList.class;
	}

	protected Class getOntology2DClassList() {
		return Cytomer2DClassList.class;
	}

}
