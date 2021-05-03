/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service.impl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authority.service.ItemReferenceResolver;
import org.dspace.authority.service.ItemSearcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemSearcher} that search a shadow copy of the item
 * with the given id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemSearchByShadowCopyRelation implements ItemSearcher, ItemReferenceResolver {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public Item searchBy(Context context, String searchParam) {
        UUID uuid = UUIDUtils.fromString(searchParam);
        if (uuid == null) {
            return null;
        }

        try {
            Item item = itemService.find(context, uuid);
            return concytecWorkflowService.findShadowItemCopy(context, item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void resolveReferences(Context context, Item item) {
        try {

            context.turnOffAuthorisationSystem();

            Item copiedItem = concytecWorkflowService.findCopiedItem(context, item);
            if (copiedItem == null) {
                return;
            }

            String authority = AuthorityValueService.REFERENCE + "SHADOW::" + copiedItem.getID();
            Iterator<Item> itemIterator = findItemsWithAuthority(context, authority, item);

            while (itemIterator.hasNext()) {
                Item itemToUpdate = itemIterator.next();

                itemToUpdate.getMetadata().stream()
                    .filter(metadataValue -> authority.equals(metadataValue.getAuthority()))
                    .forEach(metadataValue -> metadataValue.setAuthority(item.getID().toString()));

                itemService.update(context, itemToUpdate);
            }

        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private Iterator<Item> findItemsWithAuthority(Context context, String authority, Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        if (entityType == null) {
            throw new IllegalArgumentException("The given item has no dspace.entity.type: " + item.getID());
        }

        return itemService.findByAuthorityControlledMetadataFields(context, authority, entityType);
    }

}
