/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class PeruItemAutorityFilter {

    private static final Logger log = LoggerFactory.getLogger(PeruItemAutorityFilter.class);

    private final RequestService requestService;
    private final CollectionService collectionService;
    private final CommunityService communityService;
    private final String institutionParentCommunity;

    public PeruItemAutorityFilter(DSpace dspace) {
        requestService = dspace.getServiceManager()
            .getServiceByName(RequestService.class.getName(),
                RequestService.class);
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        institutionParentCommunity = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("institution.parent-community-id", "");
    }

    public void addCustomFilters(SolrQuery solrQuery) {
        Request currentRequest = requestService.getCurrentRequest();
        Context context = Optional.ofNullable(currentRequest.getServletRequest())
            .map(rq -> (Context) rq.getAttribute("dspace.context")).orElseGet(Context::new);

        Optional.ofNullable(currentRequest.getHttpServletRequest())
            .map(hsr -> hsr.getParameter("collection"))
            .filter(StringUtils::isNotBlank)
            .map(collectionId -> collectionsFilter(collectionId, context))
            .filter(StringUtils::isNotBlank)
            .ifPresent(solrQuery::addFilterQuery);
    }

    private String collectionsFilter(String collectionId, Context context) {

        List<Community> communities;
        try {
            Collection collection = collectionService.find(context, UUID.fromString(collectionId));
            communities = Objects.isNull(collection) ? Collections.emptyList() :
                communityService.getAllParents(context, collection);
        } catch (SQLException e) {
            log.error("Error while trying to extract communities for collection {}: {}", collectionId, e.getMessage());
            return "";
        }

        return communities.stream()
            .map(c -> c.getID().toString())
            .filter(id -> !institutionParentCommunity.equals(id))
            .map(id -> "location.comm:" + id)
            .collect(Collectors.joining(" OR "));

    }
}
