/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.oai;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrOaiCore {

    protected static SolrClient solr;
    private static final Logger log = LoggerFactory.getLogger(SolrOaiCore.class);

    public static SolrRequest.METHOD REQUEST_METHOD = SolrRequest.METHOD.POST;

    public SolrClient getSolrClient() throws SolrServerException {
        return getSolr();
    }

    // TODO: refactor it: remove static methods and use a Spring bean . see CSTPER-807
    public static SolrClient getSolr() throws SolrServerException {
        if (solr == null) {
            initSolr();
        }
        // If we are running Integration Tests using the EmbeddedSolrServer, we MUST override our default HTTP request
        // method to use GET instead of POST (the latter is what we prefer).  Unfortunately, EmbeddedSolrServer does not
        // current work well with POST requests (see https://issues.apache.org/jira/browse/SOLR-12858). When that bug is
        // fixed, we should remove this 'if' statement so that tests also use POST.
        if (solr.getClass().getSimpleName().equals("EmbeddedSolrServer")) {
            REQUEST_METHOD = SolrRequest.METHOD.GET;
        }
        return solr;
    }
    private static void initSolr() throws SolrServerException {
        if (solr == null) {
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            String serverUrl = configurationService.getProperty("oai.solr.url");
            try {
                solr = new HttpSolrClient.Builder(serverUrl).build();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
