/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import static java.util.Arrays.asList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the current user can change an item linkable to his profile.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = ProfileRelatedEntityChangeFeature.NAME,
    description = "Used to verify if the current user can change an item linkable to his profile.")
public class ProfileRelatedEntityChangeFeature implements AuthorizationFeature {

    public static final String NAME = "canChangeProfileRelatedEntity";

    @Autowired
    private ItemService itemService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!(object instanceof ItemRest) || context.getCurrentUser() == null) {
            return false;
        }

        if (currentUserHasNotProfile(context)) {
            return false;
        }

        Item item = findItem(context, (ItemRest) object);
        if (item == null) {
            return false;
        }

        return isItemEntityTypeIncludableInProfile(context, item) && isItemNotAlreadyIncludedInProfile(context, item);
    }

    private boolean currentUserHasNotProfile(Context context) throws SQLException {
        try {
            return researcherProfileService.findById(context, context.getCurrentUser().getID()) == null;
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private Item findItem(Context context, ItemRest itemRest) throws SQLException {
        return itemService.find(context, UUID.fromString(itemRest.getId()));
    }

    private boolean isItemEntityTypeIncludableInProfile(Context context, Item item) throws SQLException {
        String entityType = getEntityType(item);
        if (entityType == null) {
            return false;
        }

        List<String> claimableEntityTypes = asList(configurationService.getArrayProperty("claimable.entityType"));
        if (claimableEntityTypes.contains(entityType)) {
            return false;
        }

        return entityTypeService.findByEntityType(context, "Cv" + entityType) != null;
    }

    private boolean isItemNotAlreadyIncludedInProfile(Context context, Item item) throws SQLException {

        List<Item> originItems = new ArrayList<Item>();

        Item copiedItem = concytecWorkflowService.findCopiedItem(context, item);
        if (copiedItem != null) {
            originItems.add(copiedItem);
        }

        originItems.addAll(concytecWorkflowService.findOriginatedFromItems(context, item));

        return originItems.stream()
            .filter(originItem -> isCvClone(originItem))
            .map(originItem -> getClonedItem(context, originItem))
            .filter(cvItem -> cvItem != null)
            .noneMatch(cvItem -> isCurrentUserCrisOwnerOfItem(cvItem));
    }

    private boolean isCvClone(Item item) {
        String entityType = getEntityType(item);
        return entityType != null && entityType.startsWith("Cv") && entityType.endsWith("Clone");
    }

    private Item getClonedItem(Context context, Item cloneItem) {
        try {
            return concytecWorkflowService.findClonedItem(context, cloneItem);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isCurrentUserCrisOwnerOfItem(Item item) {
        return itemService.getMetadataByMetadataString(item, "cris.owner").stream()
            .anyMatch(metadataValue -> item.getID().toString().equals(metadataValue.getAuthority()));
    }

    private String getEntityType(Item item) {
        return itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }

}