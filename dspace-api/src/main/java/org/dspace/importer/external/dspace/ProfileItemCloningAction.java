/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.ItemCorrectionProvider;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterImportAction} that create a clone of the given
 * profile's item.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ProfileItemCloningAction implements AfterImportAction {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemCorrectionProvider itemCorrectionProvider;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private SearchService searchService;

    @Override
    public void applyTo(Context context, Item profileItem, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {

        String externalId = externalDataObject.getId();
        Item personItem = itemService.find(context, UUID.fromString(externalId));
        if (personItem == null) {
            throw new IllegalArgumentException("No item found from external data object with id: " + externalId);
        }

        try {
            cloneProfile(context, profileItem, personItem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void cloneProfile(Context context, Item profileItem, Item personItem) throws Exception {

        Item profileItemClone = createProfileItemClone(context, profileItem);

        Item institutionItem = concytecWorkflowService.findCopiedItem(context, personItem);
        if (institutionItem != null) {
            Item personItemCopy = createCopyAndMergeIn(context, personItem, profileItemClone);
            concytecWorkflowService.createShadowRelationship(context, profileItemClone, personItemCopy);
        } else {
            concytecWorkflowService.createShadowRelationship(context, profileItemClone, personItem);
        }

    }

    private Item createCopyAndMergeIn(Context context, Item personItem, Item profileItemClone) throws Exception {

        WorkspaceItem workspaceItemCopy = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            personItem.getID(), MERGED.getLeftType());

        Item itemCopy = installItemService.installItem(context, workspaceItemCopy);

        itemService.withdraw(context, itemCopy);
        concytecWorkflowService.createOriginatedFromRelationship(context, personItem, profileItemClone);

        return itemCopy;
    }

    private Item createProfileItemClone(Context ctx, Item item) throws Exception {
        Collection collection = findProfileCloneCollection(ctx);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profile clones");
        }
        WorkspaceItem workspaceItem = itemCorrectionProvider.createNewItemAndAddItInWorkspace(ctx, collection, item);
        concytecWorkflowService.createCloneRelationship(ctx, workspaceItem.getItem(), item);
        return installItemService.installItem(ctx, workspaceItem);
    }

    @SuppressWarnings("rawtypes")
    private Collection findProfileCloneCollection(Context context) throws SQLException, SearchServiceException {
        UUID uuid = UUIDUtils.fromString(configurationService.getProperty("cti-vitae.clone.profile-collection-id"));
        if (uuid != null) {
            return collectionService.find(context, uuid);
        }

        String profileType = getProfileCloneType();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.addFilterQueries("search.entitytype:" + profileType);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return null;
        }

        return (Collection) indexableObjects.get(0).getIndexedObject();
    }

    private String getProfileCloneType() {
        return configurationService.getProperty("researcher-profile.clone.type", "CvPersonClone");
    }

}
