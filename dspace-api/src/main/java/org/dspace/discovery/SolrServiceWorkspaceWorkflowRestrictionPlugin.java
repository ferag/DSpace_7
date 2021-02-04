/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupType;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plugin to restrict or grant access to workspace and workflow items
 * based on the discovery configuration used.
 */
public class SolrServiceWorkspaceWorkflowRestrictionPlugin implements SolrServiceSearchPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceWorkspaceWorkflowRestrictionPlugin.class);

    /**
     * The name of the discover configuration used to search for inprogress submission in the mydspace
     */
    public static final String DISCOVER_WORKSPACE_CONFIGURATION_NAME = "workspace";

    /**
     * The name of the discover configuration used to search for workflow tasks in the mydspace
     */
    public static final String DISCOVER_WORKFLOW_CONFIGURATION_NAME = "workflow";

    /**
     * The name of the discover configuration used by administrators to search for workflow tasks
     */
    public static final String DISCOVER_WORKFLOW_ADMIN_CONFIGURATION_NAME = "workflowAdmin";

    @Autowired(required = true)
    protected GroupService groupService;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected CommunityService communityService;

    @Override
    public void additionalSearchParameters(
            Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery
    ) throws SearchServiceException {
        boolean isWorkspace = StringUtils.startsWith(
                discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKSPACE_CONFIGURATION_NAME
        );
        boolean isWorkflow = StringUtils.startsWith(
                discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKFLOW_CONFIGURATION_NAME
        );
        boolean isWorkflowAdmin = isAdmin(context)
                && DISCOVER_WORKFLOW_ADMIN_CONFIGURATION_NAME.equals(discoveryQuery.getDiscoveryConfigurationName());
        EPerson currentUser = context.getCurrentUser();

        // extra security check to avoid the possibility that an anonymous user
        // get access to workspace or workflow
        if (currentUser == null && (isWorkflow || isWorkspace)) {
            throw new IllegalStateException(
                    "An anonymous user cannot perform a workspace or workflow search");
        }
        if (isWorkspace) {
            addWorkspaceFilters(context, solrQuery);
        } else if (isWorkflow && !isWorkflowAdmin) {
            // Retrieve all the groups the current user is a member of !
            Set<Group> groups;
            try {
                groups = groupService.allMemberGroupsSet(context, currentUser);
            } catch (SQLException e) {
                throw new SearchServiceException(e.getMessage(), e);
            }

            // insert filter by controllers
            StringBuilder controllerQuery = new StringBuilder();
            controllerQuery.append("taskfor:(e").append(currentUser.getID());
            for (Group group : groups) {
                controllerQuery.append(" OR g").append(group.getID());
            }
            controllerQuery.append(")");
            solrQuery.addFilterQuery(controllerQuery.toString());
        }
    }

    private void addWorkspaceFilters(Context context, SolrQuery solrQuery) throws SearchServiceException {

        EPerson currentUser = context.getCurrentUser();

        Group institutionalScopedRole = findInstitutionalScopedRole(context);
        if (institutionalScopedRole == null) {
            addSubmitterFilterQuery(solrQuery, currentUser);
            return;
        }

        Community institution = findInstitutionByScopedRole(context, institutionalScopedRole);
        if (institution == null) {
            LOGGER.warn("No institution found for the scoped role with id " + institutionalScopedRole.getID());
            addSubmitterFilterQuery(solrQuery, currentUser);
        } else {
            solrQuery.addFilterQuery("location.comm:(" + institution.getID() + ")");
        }

    }

    private Group findInstitutionalScopedRole(Context context) throws SearchServiceException {
        try {
            return groupService.allMemberGroups(context, context.getCurrentUser()).stream()
                .filter(group -> GroupType.SCOPED == groupService.getGroupType(group))
                .findFirst().orElse(null);
        } catch (SQLException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }
    }

    private Community findInstitutionByScopedRole(Context context, Group institutionalScopedRole)
        throws SearchServiceException {

        String institutionRootId = configurationService.getProperty("institution.parent-community-id");

        try {

            Community institutionRoot = communityService.find(context, UUIDUtils.fromString(institutionRootId));
            if (institutionRoot == null) {
                throw new IllegalStateException("No institutions parent community found");
            }

            return institutionRoot.getSubcommunities().stream()
                .filter(institution -> isRelatedToScopedRole(institution, institutionalScopedRole))
                .findFirst().orElse(null);

        } catch (SQLException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }

    }

    private boolean isRelatedToScopedRole(Community institution, Group institutionalScopedRole) {
        return communityService.getMetadata(institution, "perucris", "community", "institutional-scoped-role", ANY)
            .stream().anyMatch(metadata -> institutionalScopedRole.getID().toString().equals(metadata.getAuthority()));
    }

    private void addSubmitterFilterQuery(SolrQuery solrQuery, EPerson submitter) {
        solrQuery.addFilterQuery("submitter_authority:(" + submitter.getID() + ")");
    }

    private boolean isAdmin(Context context) throws SearchServiceException {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }
    }
}
