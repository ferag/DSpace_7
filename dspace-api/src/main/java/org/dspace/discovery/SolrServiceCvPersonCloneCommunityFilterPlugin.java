/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@SolrServiceSearchPlugin} to filter the CvPersonClone
 * community and all it's collections.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class SolrServiceCvPersonCloneCommunityFilterPlugin implements SolrServiceSearchPlugin {

    private static final String COMMUNITY_FILTER_QUERY_FORMAT = "NOT(location.comm:%s OR search.resourceid:%s)";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery)
        throws SearchServiceException {

        String communityId = configurationService.getProperty("cti-vitae.clone.root-id");
        if (isNotBlank(communityId)) {
            solrQuery.addFilterQuery(format(COMMUNITY_FILTER_QUERY_FORMAT, communityId, communityId));
        }
    }

}
