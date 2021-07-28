/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.pgc.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.pgc.solr.exceptions.DSpaceSolrCoreException;


/**
 * @author Alba Aliu
 */
public class DSpaceSolrCoreSearch {

    /**
     * Default constructor
     */
    private DSpaceSolrCoreSearch() { }
    /**
     *  creates a query for solr oai core  and returns all the list of solrDocuments found.
     */
    public static SolrDocumentList query(SolrClient server, SolrQuery solrParams)
        throws DSpaceSolrCoreException, IOException {
        try {
            solrParams.addSort("item.id", ORDER.asc);
            QueryResponse response = server.query(solrParams);
            return response.getResults();
        } catch (SolrServerException ex) {
            throw new DSpaceSolrCoreException(ex.getMessage(), ex);
        }
    }
}
