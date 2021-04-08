/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static java.lang.String.format;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.profile.service.CvEntityService;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.ItemCorrectionProvider;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link CvEntityService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityServiceImpl implements CvEntityService {

    private final static String CV_COLLECTION_PROPERTY_FORMAT = "researcher-profile.%s.collection.uuid";
    private final static String CV_CLONE_COLLECTION_PROPERTY_FORMAT = "cti-vitae.clone.%s-collection-id";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemCorrectionProvider itemCorrectionProvider;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public CvEntity createFromItem(Context context, Item item) throws SQLException, AuthorizeException {
        Assert.notNull(item, "A item is required to create a CV entity");

        try {

            context.turnOffAuthorisationSystem();

            Item cvCloneItem = createCvCloneItem(context, item);
            Item cvItem = createCvItem(context, item);

            concytecWorkflowService.createCloneRelationship(context, cvCloneItem, cvItem);

            linkCvCloneToItem(context, item, cvCloneItem);

            return buildCvEntity(cvItem);

        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public List<CvEntity> findByProfileItem(Context context, Item profileItem) throws SQLException, AuthorizeException {
        Assert.notNull(profileItem, "An item is required to search related CV entities");

        Iterator<Item> itemIterator = itemService.findByAuthorityValue(context, "perucris", "ctivitae", "owner",
            profileItem.getID().toString());

        return stream(spliteratorUnknownSize(itemIterator, ORDERED), false)
            .filter(this::hasCvEntityType)
            .map(this::buildCvEntity)
            .collect(Collectors.toList());

    }

    @Override
    public void deleteByProfileItem(Context context, Item profileItem) throws SQLException, AuthorizeException {
        List<CvEntity> cvEntities = findByProfileItem(context, profileItem);
        for (CvEntity cvEntity : cvEntities) {
            delete(context, cvEntity);
        }
    }

    @Override
    public void delete(Context context, CvEntity cvEntity) throws SQLException, AuthorizeException {
        concytecWorkflowService.deleteClone(context, cvEntity.getItem());
        deleteItem(context, cvEntity.getItem());
    }

    private Item createCvCloneItem(Context context, Item item) throws SQLException, AuthorizeException {

        Collection collection = findCollectionByItemEntityType(context, item, CV_CLONE_COLLECTION_PROPERTY_FORMAT);
        if (collection == null) {
            throw new IllegalArgumentException("No compliance CV clone collection found for the item: " + item.getID());
        }

        return createItemCopy(context, collection, item);
    }

    private Item createCvItem(Context context, Item item) throws SQLException, AuthorizeException {

        Collection collection = findCollectionByItemEntityType(context, item, CV_COLLECTION_PROPERTY_FORMAT);
        if (collection == null) {
            throw new IllegalArgumentException("No compliance CV collection found for the item: " + item.getID());
        }

        Item cvItem = createItemCopy(context, collection, item);

        EPerson currentUser = context.getCurrentUser();
        itemService.addMetadata(context, cvItem, "cris", "owner", null, null, currentUser.getName(),
            currentUser.getID().toString(), 600);

        ResearcherProfile researcherProfile = researcherProfileService.findById(context, currentUser.getID());
        if (researcherProfile != null) {
            itemService.addMetadata(context, cvItem, "perucris", "ctivitae", "owner", null,
                researcherProfile.getFullName(), researcherProfile.getItemId().toString(), 600);
        }

        return cvItem;
    }

    private Collection findCollectionByItemEntityType(Context context, Item item, String propertyFormat)
        throws SQLException {

        String entityType = findEntityType(item);
        if (entityType == null) {
            return null;
        }

        String collectionId = configurationService.getProperty(format(propertyFormat, entityType.toLowerCase()));
        return collectionService.find(context, UUIDUtils.fromString(collectionId));

    }

    private String findEntityType(Item item) {
        return itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
    }

    private Item createItemCopy(Context context, Collection collection, Item item)
        throws SQLException, AuthorizeException {

        try {
            WorkspaceItem wsItem = itemCorrectionProvider.createNewItemAndAddItInWorkspace(context, collection, item);
            return installItemService.installItem(context, wsItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void linkCvCloneToItem(Context context, Item item, Item cloneItem) throws SQLException, AuthorizeException {

        Item institutionItem = concytecWorkflowService.findCopiedItem(context, item);
        if (institutionItem != null) {
            Item personItemCopy = createCopyAndMergeIn(context, item, cloneItem);
            concytecWorkflowService.createShadowRelationship(context, cloneItem, personItemCopy);
        } else {
            concytecWorkflowService.createShadowRelationship(context, cloneItem, item);
        }

    }

    private Item createCopyAndMergeIn(Context context, Item item, Item cloneItem)
        throws SQLException, AuthorizeException {

        WorkspaceItem workspaceItemCopy = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            item.getID(), MERGED.getLeftType());

        Item itemCopy = installItemService.installItem(context, workspaceItemCopy);

        itemService.withdraw(context, itemCopy);
        concytecWorkflowService.createOriginatedFromRelationship(context, item, cloneItem);

        return itemCopy;
    }

    private CvEntity buildCvEntity(Item item) {
        return new CvEntity(item);
    }

    private boolean hasCvEntityType(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return entityType != null && entityType.startsWith("Cv") && !entityType.endsWith("Clone");
    }

    private void deleteItem(Context context, Item item) throws SQLException, AuthorizeException {
        try {
            itemService.delete(context, item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
