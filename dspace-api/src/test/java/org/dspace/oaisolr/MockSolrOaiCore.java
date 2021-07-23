/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.oaisolr;///**

import org.apache.solr.client.solrj.SolrClient;
import org.dspace.oai.SolrOaiCore;
import org.dspace.solr.MockSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
//
///**
// * Mock SOLR service for the OAI Core.
// */
@Service
public class MockSolrOaiCore extends SolrOaiCore implements  InitializingBean, DisposableBean {
    private MockSolrServer mockSolrServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        mockSolrServer = new MockSolrServer("oai");
        solr = mockSolrServer.getSolrServer();
    }

    /** Clear all records from the search core. */
    public void reset() {
        mockSolrServer.reset();
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
    public SolrClient getSolrClient() {
        return solr;
    }
}
