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

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * Implementation of {@link CustomAuthorityFilter} that restricts searches for
 * Institutional Entity Authorities into the community they are
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class PeruItemAuthorityFilter extends EntityTypeAuthorityFilter {

    private static final Logger log = LoggerFactory.getLogger(PeruItemAuthorityFilter.class);

    private final RequestService requestService;
    private final CollectionService collectionService;

    @Autowired
    public PeruItemAuthorityFilter(RequestService requestService,
        CollectionService collectionService) {
        this.requestService = requestService;
        this.collectionService = collectionService;
    }

    @Override
    protected List<String> createFilterQueries() {
        Request currentRequest = requestService.getCurrentRequest();
        Context context = Optional.ofNullable(currentRequest.getServletRequest())
            .map(rq -> (Context) rq.getAttribute("dspace.context")).orElseGet(Context::new);

        return Optional.ofNullable(currentRequest.getHttpServletRequest())
            .map(hsr -> hsr.getParameter("collection"))
            .filter(StringUtils::isNotBlank)
            .map(collectionId -> collectionsFilter(collectionId, context))
            .filter(StringUtils::isNotBlank)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
    }

    private String collectionsFilter(String collectionId, Context context) {

        List<Community> communities;

        try {
            Collection collection = collectionService.find(context, UUID.fromString(collectionId));
            communities = collection.getCommunities();
        } catch (SQLException e) {
            log.error("Error while trying to extract communities for collection {}: {}", collectionId, e.getMessage());
            return "";
        }

        if (Objects.isNull(communities)) {
            return "";
        }
        if (communities.size() != 1) {
            log.warn("Collection {} has {} communities, unable to proceed", collectionId,
                communities.size());
            return "";
        }
        return "location.comm:" + communities.get(0).getID();
    }

}
