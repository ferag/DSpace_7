/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.WRITE;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.exception.ResourceConflictException;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.service.AfterProfileDeleteAction;
import org.dspace.app.profile.service.BeforeProfileHardDeleteAction;
import org.dspace.app.profile.service.ImportResearcherProfileService;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ResearcherProfileService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ResearcherProfileServiceImpl implements ResearcherProfileService {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileServiceImpl.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ImportResearcherProfileService importResearcherProfileService;

    @Autowired(required = false)
    private List<AfterProfileDeleteAction> afterProfileDeleteActionList;

    @Autowired(required = false)
    private List<BeforeProfileHardDeleteAction> beforeProfileHardDeleteActionList;

    @PostConstruct
    public void postConstruct() {

        if (afterProfileDeleteActionList == null) {
            afterProfileDeleteActionList = Collections.emptyList();
        }

        if (beforeProfileHardDeleteActionList == null) {
            beforeProfileHardDeleteActionList = Collections.emptyList();
        }

    }

    @Override
    public ResearcherProfile findById(Context context, UUID id) throws SQLException, AuthorizeException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem == null) {
            return null;
        }

        return new ResearcherProfile(profileItem);
    }

    @Override
    public ResearcherProfile createAndReturn(Context context, EPerson ePerson)
        throws AuthorizeException, SQLException, SearchServiceException {

        Item profileItem = findResearcherProfileItemById(context, ePerson.getID());
        if (profileItem != null) {
            ResearcherProfile profile = new ResearcherProfile(profileItem);
            throw new ResourceConflictException("A profile is already linked to the provided User", profile);
        }

        Collection collection = findProfileCollection(context);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profiles");
        }

        List<ConfiguredResearcherProfileProvider> configuredProfileProvider =
            importResearcherProfileService.getConfiguredProfileProvider(ePerson, new ArrayList<URI>());
        if (!configuredProfileProvider.isEmpty()) {
            return this.createFromSource(context, ePerson, null);
        }

        context.turnOffAuthorisationSystem();
        Item item = createProfileItem(context, ePerson, collection);
        context.restoreAuthSystemState();
        return new ResearcherProfile(item);
    }

    @Override
    public ResearcherProfile createFromSource(Context context, EPerson ePerson, URI source)
        throws SQLException, AuthorizeException, SearchServiceException {

        Item profileItem = findResearcherProfileItemById(context, ePerson.getID());
        if (profileItem != null) {
            ResearcherProfile profile = new ResearcherProfile(profileItem);
            throw new ResourceConflictException("A profile is already linked to the provided User", profile);
        }

        Collection collection = findProfileCollection(context);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profiles");
        }

        context.turnOffAuthorisationSystem();
        Item item = importResearcherProfileService.importProfile(context, ePerson, source, collection);
        itemService.addMetadata(context, item, "cris", "owner", null, null, ePerson.getName(),
            ePerson.getID().toString(), CF_ACCEPTED);
        fillDefaultMetadata(context, ePerson, item);

        setPolicies(context, ePerson, item);
        context.restoreAuthSystemState();
        return new ResearcherProfile(item);
    }

    private void setPolicies(Context context, EPerson ePerson, Item item) throws SQLException, AuthorizeException {
        Group anonymous = groupService.findByName(context, ANONYMOUS);
        authorizeService.removeGroupPolicies(context, item, anonymous);
        authorizeService.addPolicy(context, item, READ, ePerson);
        authorizeService.addPolicy(context, item, WRITE, ePerson);
    }

    @Override
    public void deleteById(Context context, UUID id) throws SQLException, AuthorizeException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem == null) {
            return;
        }

        if (isHardDeleteEnabled()) {
            deleteItem(context, profileItem);
        } else {
            removeCrisOwnerMetadata(context, profileItem);
        }

        for (AfterProfileDeleteAction action : afterProfileDeleteActionList) {
            action.apply(context, profileItem);
        }
    }

    @Override
    public void changeVisibility(Context context, ResearcherProfile profile, boolean visible)
        throws AuthorizeException, SQLException {

        if (profile.isVisible() == visible) {
            return;
        }

        EPerson owner = ePersonService.find(context, profile.getId());
        Group anonymous = groupService.findByName(context, ANONYMOUS);
        addVisibility(context, profile.getItem(), anonymous, owner, visible);
        try {
            Iterator<Item> itemIterator = findItems(context, profile.getId());
            while (itemIterator.hasNext()) {
                addVisibility(context, itemIterator.next(), anonymous, owner, visible);
            }
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addIfNotPresent(Context context, Item item, String schema, String element,
                                 String qualifier, String value) throws SQLException {
        String metadataFirstValue = metadataFirstValue(item, "dc", "title");
        if (Objects.isNull(metadataFirstValue)) {
            itemService.addMetadata(context, item, schema, element, qualifier, null, value);
        }
    }

    private void addVisibility(Context context, Item item, Group anonymous,EPerson owner, boolean visible)
        throws SQLException, AuthorizeException {
        if (visible) {
            authorizeService.addPolicy(context, item, READ, anonymous);
        } else {
            authorizeService.removeGroupPolicies(context, item, anonymous);
            authorizeService.addPolicy(context, item, READ, owner);
        }
    }

    private Iterator<Item> findItems(Context context, UUID ownerUuid)
        throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        setFilter(discoverQuery, ownerUuid);
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void setFilter(DiscoverQuery discoverQuery, UUID ownerUuid) {
        String filter = "dspace.entity.type:CvPublication OR dspace.entity.type:CvProject OR dspace.entity.type:CvPatent";
        discoverQuery.addFilterQueries(filter);
        discoverQuery.addFilterQueries("cris.owner_authority:" + ownerUuid.toString());
    }

    private Item findResearcherProfileItemById(Context context, UUID id) throws SQLException, AuthorizeException {

        String profileType = getProfileType();

        Iterator<Item> items = itemService.findByAuthorityValue(context, "cris", "owner", null, id.toString());
        while (items.hasNext()) {
            Item item = items.next();
            if (hasEntityTypeMetadataEqualsTo(item, profileType)) {
                return item;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Collection findProfileCollection(Context context) throws SQLException, SearchServiceException {
        UUID uuid = UUIDUtils.fromString(configurationService.getProperty("researcher-profile.collection.uuid"));
        if (uuid != null) {
            return collectionService.find(context, uuid);
        }

        String profileType = getProfileType();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.addFilterQueries("search.entitytype:" + profileType);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return null;
        }

        if (indexableObjects.size() > 1) {
            log.warn("Multiple " + profileType + " type collections were found during profile creation");
            return null;
        }

        return (Collection) indexableObjects.get(0).getIndexedObject();
    }

    private Item createProfileItem(Context context, EPerson ePerson, Collection collection)
        throws AuthorizeException, SQLException {

        String id = ePerson.getID().toString();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "dc", "title", null, null, ePerson.getFullName());
        itemService.addMetadata(context, item, "cris", "sourceId", null, null, id);
        itemService.addMetadata(context, item, "cris", "owner", null, null, ePerson.getFullName(), id, CF_ACCEPTED);

        item = installItemService.installItem(context, workspaceItem);

        setPolicies(context, ePerson, item);

        return item;
    }

    private boolean hasEntityTypeMetadataEqualsTo(Item item, String entityType) {
        return item.getMetadata().stream().anyMatch(metadataValue -> {
            return "dspace.entity.type".equals(metadataValue.getMetadataField().toString('.')) &&
                entityType.equals(metadataValue.getValue());
        });
    }

    private boolean isHardDeleteEnabled() {
        return configurationService.getBooleanProperty("researcher-profile.hard-delete.enabled");
    }

    private void removeCrisOwnerMetadata(Context context, Item profileItem) throws SQLException {
        List<MetadataValue> metadata = itemService.getMetadata(profileItem, "cris", "owner", null, Item.ANY);
        itemService.removeMetadataValues(context, profileItem, metadata);
    }

    private void deleteItem(Context context, Item profileItem) throws SQLException, AuthorizeException {
        try {

            context.turnOffAuthorisationSystem();

            for (BeforeProfileHardDeleteAction action : beforeProfileHardDeleteActionList) {
                action.apply(context, profileItem);
            }

            itemService.delete(context, profileItem);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private String getProfileType() {
        return configurationService.getProperty("researcher-profile.type", "CvPerson");
    }

    private void fillDefaultMetadata(Context context, EPerson ePerson, Item item) throws SQLException {
        String title = titleFromCreatedItem(item);
        if (StringUtils.isNotBlank(title)) {
            addIfNotPresent(context, item, "dc", "title", null, title);
        }
        addIfNotPresent(context, item, "dc", "title", null, ePerson.getFullName());
        addIfNotPresent(context, item, "cris", "sourceId", null, ePerson.getID().toString());
    }

    private String titleFromCreatedItem(Item item) {
        return Stream.of(
            metadataFirstValue(item, "person", "familyName"),
            metadataFirstValue(item,"person", "givenName"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" ")).trim();
    }

    private String metadataFirstValue(Item item, String person, String familyName) {
        return itemService.getMetadataFirstValue(item, person, familyName,
            null, null);
    }

}
