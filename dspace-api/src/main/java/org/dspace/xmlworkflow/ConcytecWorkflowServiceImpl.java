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
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item, shadowItemCopy);
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

    private List<Relationship> findItemShadowRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item);
        return relationshipService.findByItemAndRelationshipType(context, item, shadowRelationshipType, isLeft);
    }

    private RelationshipType findShadowRelationshipType(Context context, Item item) throws SQLException {
        return findShadowRelationshipType(context, item, item);
    }

    private RelationshipType findShadowRelationshipType(Context context, Item item, Item shadowItem)
        throws SQLException {
        return findRelationshipType(context, item,
            shadowItem, HAS_SHADOW_COPY_RELATIONSHIP, IS_SHADOW_COPY_RELATIONSHIP);
    }

    private RelationshipType findRelationshipType(Context context, Item item, Item shadowItem,
                                                  String leftwardType, String rightwardType)
        throws SQLException {

        // FIXME: refactor this step since entities are different. This is a temporary workaround
//        EntityType entityType = entityTypeService.findByItem(context, item);
//        if (entityType == null) {
//            throw new IllegalArgumentException("No entity type found for the item " + item.getID());
//        }


//        EntityType shadowEntityType = item.equals(shadowItem) ?
//            findByName(context, entityType.getLabel()) : entityTypeService.findByItem(context, shadowItem);
//
//        if (shadowEntityType == null) {
//            throw new IllegalArgumentException("No entity type found for the item " + shadowItem.getID());
//        }
        // FIXME: temporary workaround start

        EntityType entityType = getEntityType(context, item, rn -> {
            if (!rn.startsWith("Institution")) {
                return "Institution" + rn;
            }
            return rn;
        });
        if (entityType == null) {
            throw new IllegalArgumentException("No entity type found for the item " + item.getID());
        }
        EntityType shadowEntityType = getEntityType(context, shadowItem, rn -> {
            if (rn.startsWith("Institution")) {
                return rn.replaceAll("Institution", "");
            }
            return rn;
        });

        if (shadowEntityType == null) {
            throw new IllegalArgumentException("No entity type found for the item " + shadowItem.getID());
        }

        // FIXME: temporary workaround end

        RelationshipType relationshipType = relationshipTypeService.findbyTypesAndTypeName(context, entityType,
            shadowEntityType, leftwardType, rightwardType);

        if (relationshipType == null) {
            throw new IllegalStateException("No " + leftwardType + " relationship type found for type " + entityType);
        }

        return relationshipType;
    }

    private EntityType getEntityType(Context context, Item item, Function<String, String> relationNameFix)
        throws SQLException {
        String relationship = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        if (StringUtils.isBlank(relationship)) {
            return null;
        }
        return entityTypeService.findByEntityType(context, relationNameFix.apply(relationship));
    }

    private void replaceMetadata(Context context, Item item, String schema, String element, String qualifier,
        String value) throws SQLException {
        itemService.removeMetadataValues(context, item, schema, element, qualifier, Item.ANY);
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
    }

}
