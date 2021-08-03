/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.oai.SolrOaiCore;

/**
 * @author Alba Aliu
 * Generates SolrClient instance for oai core
 */
public class DSpaceSolrCoreServer {
    private static final Logger log = LogManager.getLogger(DSpaceSolrCoreServer.class);


    /**
     * Default constructor
     */
    private DSpaceSolrCoreServer() { }

    public static SolrClient getServer() throws SolrServerException {
        return SolrOaiCore.getSolr();
    }
}
