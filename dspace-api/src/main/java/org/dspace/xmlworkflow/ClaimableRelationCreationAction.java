/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.apache.commons.lang3.ArrayUtils.contains;

import java.sql.SQLException;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.importer.external.dspace.DSpaceItemRelationshipService;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.PostShadowCopyCreationAction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link PostShadowCopyCreationAction} that if the given
 * workspaceItem is related to a Profile entity create an isPersonOwner between
 * that item and the origin CV entity.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ClaimableRelationCreationAction implements PostShadowCopyCreationAction {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private DSpaceItemRelationshipService dSpaceItemRelationshipService;

    @Override
    public void process(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {

        Item item = workspaceItem.getItem();

        if (isNotProfileEntity(item) || alreadyInARelation(context, item)) {
            return;
        }

        Item copiedItem = concytecWorkflowService.findCopiedItem(context, item);
        if (copiedItem == null || isNotCvEntityClone(copiedItem)) {
            return;
        }

        Item clonedItem = concytecWorkflowService.findClonedItem(context, copiedItem);
        if (clonedItem == null) {
            return;
        }

        dSpaceItemRelationshipService.create(context, clonedItem, item);

    }

    private boolean isNotProfileEntity(Item item) {
        String[] claimableEntityTypes = configurationService.getArrayProperty("claimable.entityType");

        if (ArrayUtils.isEmpty(claimableEntityTypes)) {
            return false;
        }

        String itemRelationshipType = getItemRelationshipType(item);
        return itemRelationshipType == null || !contains(claimableEntityTypes, itemRelationshipType);
    }

    private String getItemRelationshipType(Item item) {
        return itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
    }

    private boolean alreadyInARelation(Context context, Item item) throws SQLException {

        String[] relationshipTypes = configurationService.getArrayProperty("claimable.relation.rightwardType");

        if (ArrayUtils.isEmpty(relationshipTypes)) {
            return false;
        }

        return relationshipService.findByItem(context, item).stream()
            .filter(r -> item.equals(r.getRightItem()))
            .anyMatch(r -> ArrayUtils.contains(relationshipTypes, r.getRelationshipType().getRightwardType()));
    }

    private boolean isNotCvEntityClone(Item item) {
        String itemRelationshipType = getItemRelationshipType(item);
        if (itemRelationshipType == null) {
            return true;
        }
        return !(itemRelationshipType.startsWith("Cv") && itemRelationshipType.endsWith("Clone"));
    }

}
