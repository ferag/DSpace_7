/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * {@link SolrServiceSearchPlugin} that adds filter queries in order
 * to exclude from result items made hidden by their owner.
 *
 * Filter queries are added in case current user is not an admin and has an id different
 * than the scope of discovery query
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class SolrServiceHiddenRelationsRestrictionPlugin implements SolrServiceSearchPlugin {


    private final ResearcherProfileService researcherProfileService;

    private final RelationshipTypeService relationshipTypeService;

    private final EntityTypeService entityTypeService;

    private final AuthorizeService authorizeService;

    @Autowired
    public SolrServiceHiddenRelationsRestrictionPlugin(final ResearcherProfileService researcherProfileService,
                                                       final RelationshipTypeService relationshipTypeService,
                                                       final EntityTypeService entityTypeService,
                                                       final AuthorizeService authorizeService) {
        this.researcherProfileService = researcherProfileService;
        this.relationshipTypeService = relationshipTypeService;
        this.entityTypeService = entityTypeService;
        this.authorizeService = authorizeService;
    }

    @Override
    public void additionalSearchParameters(final Context context, final DiscoverQuery discoveryQuery,
                                           final SolrQuery solrQuery) {

        final Optional<String> scope = Optional.ofNullable(discoveryQuery.getScopeObject())
                                               .map(dso -> dso.getID().toString());
        if (scope.isEmpty() || currentUserIsScopeOrAdmin(scope.get(), context)) {
            return;
        }

        final List<String> relations = relations(context);
        if (relations == null || relations.isEmpty()) {
            return;
        }
        relations.stream()
                 .map(r -> "-relation." + r + ":" + scope.get())
                 .forEach(solrQuery::addFilterQuery);

    }

    private boolean currentUserIsScopeOrAdmin(final String scope, final Context context) {
        final EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            if (authorizeService.isAdmin(context)) {
                return true;
            }

            final ResearcherProfile researcherProfile = researcherProfileService.findById(context, currentUser.getID());

            if (researcherProfile == null || researcherProfile.getItem() == null) {
                return false;
            }

            return scope.equals(UUIDUtils.toString(researcherProfile.getItem().getID()));
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<String> relations(final Context context) {
        try {
            return relationshipTypeService
                       .findByEntityType(context,
                                         entityTypeService.findByEntityType(context, "Person"),
                                         false)
                       .stream()
                       .filter(rt -> rt.getLeftwardType().contains("HiddenFor"))
                       .map(RelationshipType::getLeftwardType)
                       .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
