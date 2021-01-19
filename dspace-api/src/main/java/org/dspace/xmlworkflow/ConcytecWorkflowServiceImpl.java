/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.dspace.xmlworkflow.ConcytecFeedback.fromString;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ConcytecWorkflowService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ConcytecWorkflowServiceImpl implements ConcytecWorkflowService {

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ItemService itemService;

    @Override
    public Relationship createShadowRelationship(Context context, Item item, Item shadowItemCopy)
        throws AuthorizeException, SQLException {
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item, true);
        return relationshipService.create(context, item, shadowItemCopy, shadowRelationshipType, true);
    }

    @Override
    public Item findShadowItemCopy(Context context, Item item) throws SQLException {
        List<Relationship> relationships = findItemShadowRelationships(context, item, true);

        if (CollectionUtils.isEmpty(relationships)) {
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException("The item " + item.getID() + " has more than one shadow copy");
        }

        return relationships.get(0).getRightItem();
    }

    @Override
    public Item findCopiedItem(Context context, Item shadowItemCopy) throws SQLException {
        List<Relationship> relationships = findItemShadowRelationships(context, shadowItemCopy, false);

        if (CollectionUtils.isEmpty(relationships)) {
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException("The item " + shadowItemCopy.getID() + " is a copy of more than one item");
        }

        return relationships.get(0).getLeftItem();

    }

    @Override
    public void setConcytecFeedback(Context context, Item item, ConcytecFeedback feedback) throws SQLException {
        replaceMetadata(context, item, "perucris", "concytec", "feedback", feedback.name());
    }

    @Override
    public ConcytecFeedback getConcytecFeedback(Context context, Item item) {
        return fromString(itemService.getMetadataFirstValue(item, "perucris", "concytec", "feedback", null));
    }

    @Override
    public void setConcytecComment(Context context, Item item, String comment) throws SQLException {
        replaceMetadata(context, item, "perucris", "concytec", "comment", comment);
    }

    @Override
    public String getConcytecComment(Context context, Item item) throws SQLException {
        return itemService.getMetadataFirstValue(item, "perucris", "concytec", "comment", null);
    }

    @Override
    public Item findWithdrawnItem(Context context, Item item) throws SQLException {
        List<Relationship> relationships = findItemWithdrawnRelationships(context, item, true);

        if (CollectionUtils.isEmpty(relationships)) {
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException("The item " + item.getID() + " is a withdrawn of more than one item");
        }

        return relationships.get(0).getRightItem();
    }

    private List<Relationship> findItemShadowRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item, isLeft);
        return relationshipService.findByItemAndRelationshipType(context, item, shadowRelationshipType, isLeft);
    }

    private RelationshipType findShadowRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, HAS_SHADOW_COPY_RELATIONSHIP, IS_SHADOW_COPY_RELATIONSHIP);
    }

    private List<Relationship> findItemWithdrawnRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType shadowRelationshipType = findWithdrawnRelationshipType(context, item, isLeft);
        return relationshipService.findByItemAndRelationshipType(context, item, shadowRelationshipType, isLeft);
    }

    private RelationshipType findWithdrawnRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, IS_WITHDRAW_OF_ITEM_RELATIONSHIP,
            IS_WITHDRAWN_BY_ITEM_RELATIONSHIP);
    }

    private RelationshipType findRelationshipType(Context context, Item item, boolean isLeft, String leftwardType,
        String rightwardType) throws SQLException {

        EntityType type = entityTypeService.findByItem(context, item);
        if (type == null) {
            throw new IllegalArgumentException("No entity type found for the item with id: " + item.getID());
        }

        List<RelationshipType> relationshipTypes = relationshipTypeService.findByTypeAndTypeNames(context, type, isLeft,
            leftwardType, rightwardType);

        if (CollectionUtils.isEmpty(relationshipTypes)) {
            throw new IllegalStateException("No " + leftwardType + " relationship type found for type " + type);
        }

        if (relationshipTypes.size() > 1) {
            throw new IllegalStateException("Multiple " + leftwardType + " relationship type found for type " + type);
        }

        return relationshipTypes.get(0);
    }

    private void replaceMetadata(Context context, Item item, String schema, String element, String qualifier,
        String value) throws SQLException {
        itemService.removeMetadataValues(context, item, schema, element, qualifier, Item.ANY);
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
    }

}
