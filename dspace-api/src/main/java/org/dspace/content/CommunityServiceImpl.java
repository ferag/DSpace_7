/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.eperson.GroupType.SCOPED;
import static org.dspace.util.UUIDUtils.fromString;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupType;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.event.Event;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.Role.Scope;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Service implementation for the Community object.
 * This class is responsible for all business logic calls for the Community object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CommunityServiceImpl extends DSpaceObjectServiceImpl<Community> implements CommunityService {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CommunityServiceImpl.class);

    @Autowired(required = true)
    protected CommunityDAO communityDAO;


    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected SiteService siteService;
    @Autowired(required = true)
    protected IdentifierService identifierService;

    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    @Autowired(required = true)
    protected XmlWorkflowFactory workflowFactory;

    @Autowired(required = true)
    protected CollectionRoleService collectionRoleService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected SubscribeService subscribeService;
    @Autowired(required = true)
    protected CrisMetricsService crisMetricsService;
    protected CommunityServiceImpl() {
        super();

    }

    @Override
    public Community create(Community parent, Context context) throws SQLException, AuthorizeException {
        return create(parent, context, null);
    }

    @Override
    public Community create(Community parent, Context context, String handle) throws SQLException, AuthorizeException {
        return create(parent, context, handle, null);
    }

    @Override
    public Community create(Community parent, Context context, String handle,
                            UUID uuid) throws SQLException, AuthorizeException {
        if (!(authorizeService.isAdmin(context) ||
                (parent != null && authorizeService.authorizeActionBoolean(context, parent, Constants.ADD)))) {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        Community newCommunity;
        if (uuid != null) {
            newCommunity = communityDAO.create(context, new Community(uuid));
        } else {
            newCommunity = communityDAO.create(context, new Community());
        }

        if (parent != null) {
            parent.addSubCommunity(newCommunity);
            newCommunity.addParentCommunity(parent);
        }


        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        authorizeService.createResourcePolicy(context, newCommunity, anonymousGroup, null, Constants.READ, null);

        communityDAO.save(context, newCommunity);

        try {
            if (handle == null) {
                identifierService.register(context, newCommunity);
            } else {
                identifierService.register(context, newCommunity, handle);
            }
        }  catch (IllegalStateException | IdentifierException ex) {
            throw new IllegalStateException(ex);
        }

        context.addEvent(new Event(Event.CREATE, Constants.COMMUNITY, newCommunity.getID(), newCommunity.getHandle(),
                getIdentifiers(context, newCommunity)));

        // if creating a top-level Community, simulate an ADD event at the Site.
        if (parent == null) {
            context.addEvent(new Event(Event.ADD, Constants.SITE, siteService.findSite(context).getID(),
                    Constants.COMMUNITY, newCommunity.getID(), newCommunity.getHandle(),
                    getIdentifiers(context, newCommunity)));
        }

        log.info(LogHelper.getHeader(context, "create_community",
                "community_id=" + newCommunity.getID())
                + ",handle=" + newCommunity.getHandle());

        return newCommunity;
    }

    @Override
    public Community find(Context context, UUID id) throws SQLException {
        return communityDAO.findByID(context, Community.class, id);
    }

    @Override
    public List<Community> findAll(Context context) throws SQLException {
        MetadataField sortField = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(),
                                                                     "title", null);
        if (sortField == null) {
            throw new IllegalArgumentException(
                "Required metadata field '" + MetadataSchemaEnum.DC.getName() + ".title' doesn't exist!");
        }

        return communityDAO.findAll(context, sortField);
    }

    @Override
    public List<Community> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(),
                                                                     "title", null);
        if (nameField == null) {
            throw new IllegalArgumentException(
                "Required metadata field '" + MetadataSchemaEnum.DC.getName() + ".title' doesn't exist!");
        }

        return communityDAO.findAll(context, nameField, limit, offset);
    }

    @Override
    public List<Community> findAllTop(Context context) throws SQLException {
        // get all communities that are not children
        MetadataField sortField = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(),
                                                                     "title", null);
        if (sortField == null) {
            throw new IllegalArgumentException(
                "Required metadata field '" + MetadataSchemaEnum.DC.getName() + ".title' doesn't exist!");
        }

        List<Community> topCommunities = communityDAO.findAllNoParent(context, sortField);

        UUID ctiVitaeCloneCommunityId = fromString(configurationService.getProperty("cti-vitae.clone.root-id"));
        if (ctiVitaeCloneCommunityId == null) {
            return topCommunities;
        }

        return topCommunities.stream()
            .filter(community -> !community.getID().equals(ctiVitaeCloneCommunityId))
            .collect(Collectors.toList());
    }

    @Override
    public void setMetadataSingleValue(Context context, Community community,
            MetadataFieldName field, String language, String value)
            throws MissingResourceException, SQLException {
        if (field.equals(MD_NAME) && (value == null || value.trim().equals(""))) {
            try {
                value = I18nUtil.getMessage("org.dspace.content.untitled");
            } catch (MissingResourceException e) {
                value = "Untitled";
            }
        }

        /*
         * Set metadata field to null if null
         * and trim strings to eliminate excess
         * whitespace.
         */
        if (value == null) {
            clearMetadata(context, community, field.schema, field.element, field.qualifier, Item.ANY);
            community.setMetadataModified();
        } else {
            super.setMetadataSingleValue(context, community, field, null, value);
        }

        community.addDetails(field.toString());
    }

    @Override
    public Bitstream setLogo(Context context, Community community, InputStream is)
            throws AuthorizeException, IOException, SQLException {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && authorizeService.authorizeActionBoolean(
                context, community, Constants.DELETE))) {
            canEdit(context, community);
        }

        // First, delete any existing logo
        Bitstream oldLogo = community.getLogo();
        if (oldLogo != null) {
            log.info(LogHelper.getHeader(context, "remove_logo", "community_id=" + community.getID()));
            community.setLogo(null);
            bitstreamService.delete(context, oldLogo);
        }

        if (is != null) {
            Bitstream newLogo = bitstreamService.create(context, is);
            community.setLogo(newLogo);

            // now create policy for logo bitstream
            // to match our READ policy
            List<ResourcePolicy> policies = authorizeService
                    .getPoliciesActionFilter(context, community, Constants.READ);
            authorizeService.addPolicies(context, policies, newLogo);

            log.info(LogHelper.getHeader(context, "set_logo",
                "community_id=" + community.getID() + "logo_bitstream_id=" + newLogo.getID()));
        }

        return community.getLogo();
    }

    @Override
    public void update(Context context, Community community) throws SQLException, AuthorizeException {
        // Check authorisation
        canEdit(context, community);

        log.info(LogHelper.getHeader(context, "update_community",
                                      "community_id=" + community.getID()));

        super.update(context, community);

        communityDAO.save(context, community);
        if (community.isModified()) {
            context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, community.getID(), null,
                                       getIdentifiers(context, community)));
            community.clearModified();
        }
        if (community.isMetadataModified()) {
            context.addEvent(
                new Event(Event.MODIFY_METADATA, Constants.COMMUNITY, community.getID(), community.getDetails(),
                          getIdentifiers(context, community)));
            community.clearModified();
        }
        community.clearDetails();
    }

    @Override
    public Group createAdministrators(Context context, Community community) throws SQLException, AuthorizeException {
        return createAdministrators(context, community, true);
    }

    @Override
    public Group createAdministrators(Context context, Community community, boolean rethinkCache)
        throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(context, community);

        Group admins = community.getAdministrators();
        if (admins == null) {
            //turn off authorization so that Community Admins can create Sub-Community Admins
            context.turnOffAuthorisationSystem();
            admins = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(admins, "COMMUNITY_" + community.getID() + "_ADMIN");
            groupService.update(context, admins, rethinkCache);
        }

        authorizeService.addPolicy(context, community, Constants.ADMIN, admins);

        // register this as the admin group
        community.setAdmins(admins);
        context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, community.getID(),
                                             null, getIdentifiers(context, community)));
        return admins;
    }

    @Override
    public void removeAdministrators(Context context, Community community) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin of the parent community (or system admin) to delete Admin group
        AuthorizeUtil.authorizeRemoveAdminGroup(context, community);

        // just return if there is no administrative group.
        if (community.getAdministrators() == null) {
            return;
        }

        // Remove the link to the community table.
        community.setAdmins(null);
        context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, community.getID(),
                                             null, getIdentifiers(context, community)));
    }

    @Override
    public List<Community> getAllParents(Context context, Community community) throws SQLException {
        List<Community> parentList = new ArrayList<>();
        Community parent = (Community) getParentObject(context, community);
        while (parent != null) {
            parentList.add(parent);
            parent = (Community) getParentObject(context, parent);
        }
        return parentList;
    }

    @Override
    public List<Community> getAllParents(Context context, Collection collection) throws SQLException {
        List<Community> result = new ArrayList<>();
        List<Community> communities = collection.getCommunities();
        result.addAll(communities);
        for (Community community : communities) {
            result.addAll(getAllParents(context, community));
        }
        return result;
    }

    @Override
    public List<Collection> getAllCollections(Context context, Community community) throws SQLException {
        List<Collection> collectionList = new ArrayList<>();
        return getCollections(context, community, (collection) -> true);
    }

    @Override
    public List<Collection> getCollections(Context context, Community community, Predicate<Collection> predicate) {
        List<Collection> collectionList = new ArrayList<>();
        List<Community> subCommunities = community.getSubcommunities();
        for (Community subCommunity : subCommunities) {
            addCollectionList(subCommunity, collectionList, predicate);
        }

        List<Collection> collections = community.getCollections();
        for (Collection collection : collections) {
            if (predicate.test(collection)) {
                collectionList.add(collection);
            }
        }
        return collectionList;
    }


    /**
     * Internal method to process subcommunities recursively
     *
     * @param community      community
     * @param collectionList list of collections
     * @param predicate      the predicate to evaluate
     */
    protected void addCollectionList(Community community, List<Collection> collectionList,
        Predicate<Collection> predicate) {
        for (Community subcommunity : community.getSubcommunities()) {
            addCollectionList(subcommunity, collectionList, predicate);
        }

        for (Collection collection : community.getCollections()) {
            if (predicate.test(collection)) {
                collectionList.add(collection);
            }
        }
    }

    @Override
    public void addCollection(Context context, Community community, Collection collection)
        throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, community, Constants.ADD);

        log.info(LogHelper.getHeader(context, "add_collection",
                                      "community_id=" + community.getID() + ",collection_id=" + collection.getID()));

        if (!community.getCollections().contains(collection)) {
            community.addCollection(collection);
            collection.addCommunity(community);
        }
        context.addEvent(
            new Event(Event.ADD, Constants.COMMUNITY, community.getID(), Constants.COLLECTION, collection.getID(),
                      community.getHandle(), getIdentifiers(context, community)));
    }

    @Override
    public Community createSubcommunity(Context context, Community parentCommunity)
            throws SQLException, AuthorizeException {
        return createSubcommunity(context, parentCommunity, null);
    }


    @Override
    public Community createSubcommunity(Context context, Community parentCommunity, String handle)
            throws SQLException, AuthorizeException {
        return createSubcommunity(context, parentCommunity, handle, null);
    }

    @Override
    public Community createSubcommunity(Context context, Community parentCommunity, String handle,
                                        UUID uuid) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.ADD);

        Community c;
        c = create(parentCommunity, context, handle, uuid);

        addSubcommunity(context, parentCommunity, c);

        return c;
    }

    @Override
    public void addSubcommunity(Context context, Community parentCommunity, Community childCommunity)
        throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.ADD);

        log.info(LogHelper.getHeader(context, "add_subcommunity",
                                      "parent_comm_id=" + parentCommunity.getID() + ",child_comm_id=" + childCommunity
                                          .getID()));

        if (!parentCommunity.getSubcommunities().contains(childCommunity)) {
            parentCommunity.addSubCommunity(childCommunity);
            childCommunity.addParentCommunity(parentCommunity);
        }
        context.addEvent(new Event(Event.ADD, Constants.COMMUNITY, parentCommunity.getID(), Constants.COMMUNITY,
                                   childCommunity.getID(), parentCommunity.getHandle(),
                                   getIdentifiers(context, parentCommunity)));
    }

    @Override
    public void removeCollection(Context context, Community community, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, community, Constants.REMOVE);

        ArrayList<String> removedIdentifiers = collectionService.getIdentifiers(context, collection);
        String removedHandle = collection.getHandle();
        UUID removedId = collection.getID();

        if (collection.getCommunities().size() == 1) {
            collectionService.delete(context, collection);
        } else {
            community.removeCollection(collection);
            collection.removeCommunity(community);
        }

        log.info(LogHelper.getHeader(context, "remove_collection",
                                      "community_id=" + community.getID() + ",collection_id=" + collection.getID()));

        // Remove any mappings
        context.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, community.getID(),
                                   Constants.COLLECTION, removedId, removedHandle, removedIdentifiers));
    }

    @Override
    public void removeSubcommunity(Context context, Community parentCommunity, Community childCommunity)
        throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.REMOVE);

        ArrayList<String> removedIdentifiers = getIdentifiers(context, childCommunity);
        String removedHandle = childCommunity.getHandle();
        UUID removedId = childCommunity.getID();

        rawDelete(context, childCommunity);

        log.info(LogHelper.getHeader(context, "remove_subcommunity",
                                      "parent_comm_id=" + parentCommunity.getID() + ",child_comm_id=" + childCommunity
                                          .getID()));

        context.addEvent(
            new Event(Event.REMOVE, Constants.COMMUNITY, parentCommunity.getID(), Constants.COMMUNITY, removedId,
                      removedHandle, removedIdentifiers));
    }

    @Override
    public void delete(Context context, Community community) throws SQLException, AuthorizeException, IOException {
        crisMetricsService.deleteByResourceID(context, community);
        // Check authorisation
        // FIXME: If this was a subcommunity, it is first removed from it's
        // parent.
        // This means the parentCommunity == null
        // But since this is also the case for top-level communities, we would
        // give everyone rights to remove the top-level communities.
        // The same problem occurs in removing the logo
        if (!authorizeService.authorizeActionBoolean(context, getParentObject(context, community), Constants.REMOVE)) {
            authorizeService.authorizeAction(context, community, Constants.DELETE);
        }
        ArrayList<String> removedIdentifiers = getIdentifiers(context, community);
        String removedHandle = community.getHandle();
        UUID removedId = community.getID();

        subscribeService.deleteByDspaceObject(context, community);

        // If not a top-level community, have parent remove me; this
        // will call rawDelete() before removing the linkage
        Community parent = (Community) getParentObject(context, community);

        if (parent != null) {
            // remove the subcommunities first
            Iterator<Community> subcommunities = community.getSubcommunities().iterator();
            while (subcommunities.hasNext()) {
                Community subCommunity = subcommunities.next();
                community.removeSubCommunity(subCommunity);
                delete(context, subCommunity);
            }
            // now let the parent remove the community
            removeSubcommunity(context, parent, community);

            return;
        }

        rawDelete(context, community);
        context.addEvent(
            new Event(Event.REMOVE, Constants.SITE, siteService.findSite(context).getID(), Constants.COMMUNITY,
                      removedId, removedHandle, removedIdentifiers));

    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.COMMUNITY;
    }

    /**
     * Internal method to remove the community and all its children from the
     * database, and perform any pre/post-cleanup
     *
     * @param context   context
     * @param community community
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    protected void rawDelete(Context context, Community community)
        throws SQLException, AuthorizeException, IOException {
        log.info(LogHelper.getHeader(context, "delete_community",
                                      "community_id=" + community.getID()));

        context.addEvent(new Event(Event.DELETE, Constants.COMMUNITY, community.getID(), community.getHandle(),
                                   getIdentifiers(context, community)));

        // Remove collections
        Iterator<Collection> collections = community.getCollections().iterator();

        while (collections.hasNext()) {
            Collection collection = collections.next();
            community.removeCollection(collection);
            removeCollection(context, community, collection);
        }
        // delete subcommunities
        Iterator<Community> subCommunities = community.getSubcommunities().iterator();

        while (subCommunities.hasNext()) {
            Community subComm = subCommunities.next();
            community.removeSubCommunity(subComm);
            delete(context, subComm);
        }

        // Remove the logo
        setLogo(context, community, null);

        // Remove any Handle
        handleService.unbindHandle(context, community);

        // Remove the parent-child relationship for the community we want to delete
        Community parent = (Community) getParentObject(context, community);
        if (parent != null) {
            community.removeParentCommunity(parent);
            parent.removeSubCommunity(community);
        }

        Group g = community.getAdministrators();

        // Delete community row
        communityDAO.delete(context, community);

        // Remove administrators group - must happen after deleting community

        if (g != null) {
            groupService.delete(context, g);
        }
    }

    @Override
    public boolean canEditBoolean(Context context, Community community) throws SQLException {
        try {
            canEdit(context, community);

            return true;
        } catch (AuthorizeException e) {
            return false;
        }
    }

    @Override
    public void canEdit(Context context, Community community) throws AuthorizeException, SQLException {
        List<Community> parents = getAllParents(context, community);

        for (Community parent : parents) {
            if (authorizeService.authorizeActionBoolean(context, parent,
                                                        Constants.WRITE)) {
                return;
            }

            if (authorizeService.authorizeActionBoolean(context, parent,
                                                        Constants.ADD)) {
                return;
            }
        }

        authorizeService.authorizeAction(context, community, Constants.WRITE);
    }

    @Override
    public Community findByAdminGroup(Context context, Group group) throws SQLException {
        return communityDAO.findByAdminGroup(context, group);
    }

    @Override
    public List<Community> findAuthorized(Context context, List<Integer> actions) throws SQLException {
        return communityDAO.findAuthorized(context, context.getCurrentUser(), actions);
    }

    @Override
    public List<Community> findAuthorizedGroupMapped(Context context, List<Integer> actions) throws SQLException {
        return communityDAO.findAuthorizedByGroup(context, context.getCurrentUser(), actions);
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Community community, int action) throws SQLException {
        DSpaceObject adminObject = null;
        switch (action) {
            case Constants.REMOVE:
                if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion()) {
                    adminObject = community;
                }
                break;

            case Constants.DELETE:
                if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion()) {
                    adminObject = getParentObject(context, community);
                    if (adminObject == null) {
                        //top-level community, has to be admin of the current community
                        adminObject = community;
                    }
                }
                break;
            case Constants.ADD:
                if (AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation()) {
                    adminObject = community;
                }
                break;
            default:
                adminObject = community;
                break;
        }
        return adminObject;
    }


    @Override
    public DSpaceObject getParentObject(Context context, Community community) throws SQLException {
        List<Community> parentCommunities = community.getParentCommunities();
        if (CollectionUtils.isNotEmpty(parentCommunities)) {
            return parentCommunities.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public void updateLastModified(Context context, Community community) {
        //Also fire a modified event since the community HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY,
                                   community.getID(), null, getIdentifiers(context, community)));

    }

    @Override
    public Community findByIdOrLegacyId(Context context, String id) throws SQLException {
        if (StringUtils.isNumeric(id)) {
            return findByLegacyId(context, Integer.parseInt(id));
        } else {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Community findByLegacyId(Context context, int id) throws SQLException {
        return communityDAO.findByLegacyId(context, id, Community.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return communityDAO.countRows(context);
    }

    @Override
    public Community cloneCommunity(Context context, Community template, Community parent, String name)
        throws SQLException, AuthorizeException {
        Assert.notNull(name, "The name of the new community must be provided");

        Community newCommunity = create(parent, context);
        Map<UUID, Group> scopedRoles = createScopedRoles(context, name, newCommunity);
        newCommunity = cloneCommunity(context, template, newCommunity, scopedRoles);
        setCommunityName(context, newCommunity, name);

        groupService.rethinkGroupCache(context, true);

        return newCommunity;
    }

    @Override
    public Community findDirectorioCommunity(Context context) throws SQLException {

        UUID directorioId = UUIDUtils.fromString(configurationService.getProperty("directorios.community-id"));
        if (directorioId == null) {
            log.warn("The property directorios.community-id is not configured correctly");
            return null;
        }

        return find(context, directorioId);
    }

    private Community cloneCommunity(Context context, Community communityToClone, Community clone,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {

        List<Community> subCommunities = communityToClone.getSubcommunities();
        List<Collection> subCollections = communityToClone.getCollections();
        cloneMetadata(context, this, clone, communityToClone);
        cloneCommunityGroups(context, clone, communityToClone, scopedRoles);

        for (Community c : subCommunities) {
            Community newSubCommunity = create(clone, context);
            cloneCommunity(context, c, newSubCommunity, scopedRoles);
        }

        for (Collection collection : subCollections) {
            Collection newCollection = collectionService.create(context, clone);
            cloneMetadata(context, collectionService, newCollection, collection);
            cloneTemplateItem(context, newCollection, collection);
            cloneCollectionGroups(context, newCollection, collection, scopedRoles);
        }

        return clone;
    }

    private void cloneTemplateItem(Context context, Collection newCollection, Collection collection)
        throws SQLException, AuthorizeException {
        Item item = collection.getTemplateItem();
        if (item != null) {
            collectionService.createTemplateItem(context, newCollection);
            Item newItemTemplate = newCollection.getTemplateItem();
            cloneMetadata(context, itemService, newItemTemplate, item);
        }
    }

    private <T extends DSpaceObject> void cloneMetadata(Context context, DSpaceObjectService<T> service,
        T target, T dsoToClone) throws SQLException {

        List<MetadataValue> metadataValue = dsoToClone.getMetadata();
        for (MetadataValue metadata : metadataValue) {
            service.addMetadata(context, target, metadata.getSchema(), metadata.getElement(),
                metadata.getQualifier(), null, metadata.getValue());
        }
    }

    private Community setCommunityName(Context context, Community community, String name)
        throws SQLException, AuthorizeException {
        List<MetadataValue> metadata = getMetadata(community, "dc", "title", null, Item.ANY);
        if (CollectionUtils.isEmpty(metadata)) {
            addMetadata(context, community, "dc", "title", null, null, name);
        } else {
            MetadataValue dcTitle = metadata.get(0);
            dcTitle.setValue(name);
            update(context, community);
        }
        return community;
    }

    private void cloneCommunityGroups(Context context, Community clone, Community communityToClone,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {

        Group administrators = communityToClone.getAdministrators();
        if (administrators != null) {
            Group newAdministrators = createAdministrators(context, clone, false);
            addInstitutionalScopedRoleMembers(context, administrators, newAdministrators, scopedRoles);
        }

        clonePolicies(context, clone, communityToClone, scopedRoles);

    }

    private void cloneCollectionGroups(Context context, Collection newCollection, Collection collection,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {

        Group administrators = collection.getAdministrators();
        if (administrators != null) {
            Group newAdministrators = collectionService.createAdministrators(context, newCollection, false);
            addInstitutionalScopedRoleMembers(context, administrators, newAdministrators, scopedRoles);
            groupService.update(context, newAdministrators, false);
        }

        Group submitter = collection.getSubmitters();
        if (submitter != null) {
            Group newSubmitter = collectionService.createSubmitters(context, newCollection, false);
            addInstitutionalScopedRoleMembers(context, submitter, newSubmitter, scopedRoles);
            groupService.update(context, newSubmitter, false);
        }

        try {
            cloneWorkflowGroups(context, collection, newCollection, scopedRoles);
        } catch (WorkflowConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        clonePolicies(context, newCollection, collection, scopedRoles);

    }

    private void addInstitutionalScopedRoleMembers(Context context, Group group, Group newGroup,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {

        for (Group subGroup : group.getMemberGroups()) {
            if (scopedRoles.containsKey(subGroup.getID())) {
                groupService.addMember(context, newGroup, scopedRoles.get(subGroup.getID()));
            } else {
                groupService.addMember(context, newGroup, subGroup);
            }
        }
        scopedRoles.put(group.getID(), newGroup);
    }

    private void clonePolicies(Context context, DSpaceObject clone, DSpaceObject objectToClone,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {
        authorizeService.removeAllPolicies(context, clone);
        for (ResourcePolicy policy : objectToClone.getResourcePolicies()) {
            Group group = policy.getGroup();
            if (group != null) {
                if (scopedRoles.containsKey(group.getID())) {
                    authorizeService.addPolicy(context, clone, policy.getAction(), scopedRoles.get(group.getID()));
                } else {
                    authorizeService.addPolicy(context, clone, policy.getAction(), group);
                }
            } else {
                authorizeService.addPolicy(context, clone, policy.getAction(), policy.getEPerson());
            }
        }
    }

    private void cloneWorkflowGroups(Context context, Collection collection, Collection newCollection,
        Map<UUID, Group> scopedRoles) throws WorkflowConfigurationException, SQLException, AuthorizeException {

        List<CollectionRole> collectionRoles = getCollectionRole(context, collection);
        for (CollectionRole collectionRole : collectionRoles) {
            cloneWorkflowGroup(context, newCollection, collectionRole, scopedRoles);
        }

    }

    private List<CollectionRole> getCollectionRole(Context context, Collection collection)
        throws WorkflowConfigurationException {

        Workflow workflow = workflowFactory.getWorkflow(collection);
        List<Step> steps = workflow.getSteps();
        if (CollectionUtils.isEmpty(steps)) {
            return new ArrayList<>();
        }

        return steps.stream()
            .map(step -> step.getRole())
            .filter(role -> role != null && role.getScope() == Scope.COLLECTION)
            .map(role -> findCollectionRole(context, collection, role.getId()))
            .filter(collectionRole -> collectionRole != null)
            .collect(Collectors.toList());

    }

    private CollectionRole findCollectionRole(Context context, Collection collection, String roleId) {
        try {
            return collectionRoleService.find(context, collection, roleId);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void cloneWorkflowGroup(Context context, Collection newCollection, CollectionRole collectionRole,
        Map<UUID, Group> scopedRoles) throws SQLException, AuthorizeException {

        String roleId = collectionRole.getRoleId();
        Group newWorkflowGroup = collectionService.createWorkflowGroup(context, newCollection, roleId, false);
        addInstitutionalScopedRoleMembers(context, collectionRole.getGroup(), newWorkflowGroup, scopedRoles);
        groupService.update(context, newWorkflowGroup, false);

    }

    /**
     * Create a new Institutional Scoped Role for each existing Institutional Role.
     * The community will keep a reference to the institutional scoped role via
     * perucris.community.institutional-scoped-role metadata.
     *
     * @return a map between the institutional roles and the related scopes for the
     *         institution community
     */
    private Map<UUID, Group> createScopedRoles(Context context, String institutionName, Community institution)
        throws SQLException, AuthorizeException {

        Map<UUID, Group> institutionalRoleMap = new HashMap<>();
        List<Group> institutionalRoles = groupService.findByGroupType(context, GroupType.INSTITUTIONAL);
        for (Group institutionalRole : institutionalRoles) {
            Group scopedRole = groupService.create(context);
            String roleName = institutionalRole.getNameWithoutTypePrefix() + ": " + institutionName;
            groupService.setName(scopedRole, SCOPED + ":" + roleName);
            groupService.addMetadata(context, scopedRole, "perucris", "group", "type", null, SCOPED.name());
            groupService.addMember(context, institutionalRole, scopedRole);

            addMetadata(context, institution, "perucris", "community", "institutional-scoped-role", null,
                roleName, scopedRole.getID().toString(), 600);

            institutionalRoleMap.put(institutionalRole.getID(), scopedRole);
        }

        return institutionalRoleMap;

    }
}
