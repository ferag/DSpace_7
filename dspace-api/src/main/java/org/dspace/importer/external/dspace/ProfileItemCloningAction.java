/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import static java.util.Arrays.asList;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.exception.ResourceConflictException;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.app.profile.service.ProfileItemCloneService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
public class ProfileItemCloningAction implements AfterImportAction, ProfileItemCloneService {

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

    @Override
    public void applyTo(Context context, Item profileItem, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {

        Optional<UUID> uuid = uuid(externalDataObject);
        // in case external data object hasn't the uuid, it is not sourced from DSpace, no cloning action needed.
        if (uuid.isEmpty()) {
            return;
        }
        Item personItem = itemService.find(context, uuid.get());
        if (personItem == null) {
            throw new IllegalArgumentException("No item found from external data object with id: " +
                UUIDUtils.toString(uuid.get()));
        }

        try {
            cloneProfile(context, profileItem, personItem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void cloneProfile(Context context, Item profileItem, Item personItem)
        throws SQLException, AuthorizeException, IOException {

        if (unClaimableEntityType(personItem)) {
            throw new IllegalArgumentException("The given item is not claimable: " + personItem.getID());
        }

        Item preExistingClone = concytecWorkflowService.findClone(context, profileItem);
        if (preExistingClone != null) {
            throw new ResourceConflictException("The given profile item is already cloned", preExistingClone);
        }

        try {

            context.turnOffAuthorisationSystem();

            Item profileItemClone = createProfileItemClone(context, profileItem);

            Item institutionItem = concytecWorkflowService.findCopiedItem(context, personItem);
            if (institutionItem != null) {
                Item personItemCopy = createCopyAndMergeIn(context, personItem, profileItemClone);
                concytecWorkflowService.createShadowRelationship(context, profileItemClone, personItemCopy);
            } else {
                concytecWorkflowService.createShadowRelationship(context, profileItemClone, personItem);
            }

        } finally {
            context.restoreAuthSystemState();
        }

    }

    private Item createCopyAndMergeIn(Context context, Item personItem, Item profileItemClone)
        throws SQLException, AuthorizeException {

        WorkspaceItem workspaceItemCopy = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            personItem.getID(), MERGED.getLeftType());

        Item itemCopy = installItemService.installItem(context, workspaceItemCopy);

        itemService.withdraw(context, itemCopy);
        concytecWorkflowService.createOriginatedFromRelationship(context, personItem, profileItemClone);

        return itemCopy;
    }

    private Item createProfileItemClone(Context ctx, Item item) throws SQLException, AuthorizeException, IOException {
        Collection collection = findProfileCloneCollection(ctx);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profile clones");
        }
        WorkspaceItem workspaceItem = itemCorrectionProvider.createNewItemAndAddItInWorkspace(ctx, collection, item);
        concytecWorkflowService.createCloneRelationship(ctx, workspaceItem.getItem(), item);
        return installItemService.installItem(ctx, workspaceItem);
    }

    private Collection findProfileCloneCollection(Context context) throws SQLException {
        return collectionService.find(context,
            UUIDUtils.fromString(configurationService.getProperty("cti-vitae.clone.person-collection-id")));
    }

    private boolean unClaimableEntityType(Item item) {
        List<String> claimableEntityTypes = asList(configurationService.getArrayProperty("claimable.entityType"));
        return itemService.getMetadataByMetadataString(item, "dspace.entity.type").stream()
            .noneMatch(mv -> claimableEntityTypes.contains(mv.getValue()));
    }


    /**
     * UUID is defined for DSpace objects, thus it is set in ExternalDataObject passed as
     * param only if object comes from DSpace or it has DSpace as part of its sources.
     * @param externalDataObject
     * @return
     */
    private Optional<UUID> uuid(ExternalDataObject externalDataObject) {
        MergedExternalDataObject mergedExternalDataObject = MergedExternalDataObject.from(externalDataObject);
        if (!mergedExternalDataObject.isMerged()) {
            return Optional.ofNullable(UUIDUtils.fromString(externalDataObject.getId()));
        }
        return mergedExternalDataObject.getDSpaceObjectUUID();
    }
}
