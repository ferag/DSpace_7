/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.dspace.xmlworkflow.ConcytecFeedback.FEEDBACK_SEPARATOR;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.REINSTATE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ConcytecWorkflowService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ConcytecWorkflowServiceImpl implements ConcytecWorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcytecWorkflowServiceImpl.class);

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
        if (shadowRelationshipType == null) {
            return null;
        }
        return relationshipService.create(context, item, shadowItemCopy, shadowRelationshipType, true);
    }

    @Override
    public Relationship createMergedInRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException {
        RelationshipType mergedInRelationshipType = findMergedInRelationshipType(context, leftItem, true);
        if (mergedInRelationshipType == null) {
            return null;
        }
        return relationshipService.create(context, leftItem, rightItem, mergedInRelationshipType, true);
    }

    @Override
    public Relationship createOriginatedFromRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException {
        RelationshipType originatedFromRelationshipType = findOriginatedFromRelationshipType(context, leftItem, true);
        if (originatedFromRelationshipType == null) {
            return null;
        }
        return relationshipService.create(context, leftItem, rightItem, originatedFromRelationshipType, true);
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
    public void addConcytecFeedback(Context context, Item item, ConcytecFeedback feedback) throws SQLException {
        addConcytecFeedback(context, item, feedback.name());
    }

    @Override
    public void addConcytecFeedback(Context context, Item item, String feedback) throws SQLException {
        itemService.addMetadata(context, item, "perucris", "concytec", "feedback", null, feedback);
    }

    @Override
    public void addConcytecFeedback(Context context, Item item, ConcytecWorkflowRelation relation,
        ConcytecFeedback feedback, String concytecComment) throws SQLException {

        addConcytecFeedback(context, item, relation.name() + FEEDBACK_SEPARATOR + feedback.name());
        if (StringUtils.isNotBlank(concytecComment)) {
            addConcytecComment(context, item, relation.name() + FEEDBACK_SEPARATOR + concytecComment);
        }

    }

    @Override
    public void addConcytecFeedback(Context context, Item item, ConcytecFeedback feedback, String comment)
        throws SQLException {

        addConcytecFeedback(context, item, feedback);
        if (StringUtils.isNotBlank(comment)) {
            addConcytecComment(context, item, comment);
        }

    }

    @Override
    public ConcytecFeedback getLastConcytecFeedback(Context context, Item item) {
        List<MetadataValue> feedbacks = itemService.getMetadata(item, "perucris", "concytec", "feedback", null);
        if (CollectionUtils.isEmpty(feedbacks)) {
            return ConcytecFeedback.NONE;
        }

        return ConcytecFeedback.fromString(feedbacks.get(feedbacks.size() - 1).getValue());
    }

    @Override
    public void addConcytecComment(Context context, Item item, String comment) throws SQLException {
        itemService.addMetadata(context, item, "perucris", "concytec", "comment", null, comment);
    }

    @Override
    public String getLastConcytecComment(Context context, Item item) throws SQLException {
        List<MetadataValue> comments = itemService.getMetadata(item, "perucris", "concytec", "comment", null);
        if (CollectionUtils.isEmpty(comments)) {
            return null;
        }

        String lastComment = comments.get(comments.size() - 1).getValue();

        return lastComment.contains(FEEDBACK_SEPARATOR)
            ? lastComment.substring(lastComment.indexOf(FEEDBACK_SEPARATOR) + FEEDBACK_SEPARATOR.length())
            : lastComment;
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

    @Override
    public Item findReinstateItem(Context context, Item item) throws SQLException {
        List<Relationship> relationships = findItemReinstateRelationships(context, item, true);

        if (CollectionUtils.isEmpty(relationships)) {
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException("The item " + item.getID() + " is a reinstate of more than one item");
        }

        return relationships.get(0).getRightItem();
    }

    @Override
    public List<Item> findOriginatedFromItems(Context context, Item item) throws SQLException {
        RelationshipType originatedFromType = findOriginatedFromRelationshipType(context, item, true);
        if (originatedFromType == null) {
            return Collections.emptyList();
        }

        return relationshipService.findByItemAndRelationshipType(context, item, originatedFromType, true).stream()
            .map(relationship -> relationship.getRightItem())
            .collect(Collectors.toList());
    }

    @Override
    public Item findMergeOfItem(Context context, Item item) throws SQLException {
        List<Relationship> relationships = findItemMergeRelationships(context, item, true);

        if (CollectionUtils.isEmpty(relationships)) {
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException("The item " + item.getID() + " is a reinstate of more than one item");
        }

        return relationships.get(0).getRightItem();
    }

    private List<Relationship> findItemShadowRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item, isLeft);
        if (shadowRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, shadowRelationshipType, isLeft);
    }

    private RelationshipType findShadowRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, SHADOW_COPY.getLeftType(), SHADOW_COPY.getRightType());
    }

    private RelationshipType findMergedInRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, MERGED.getLeftType(), MERGED.getRightType());
    }

    private RelationshipType findOriginatedFromRelationshipType(Context ctx, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(ctx, item, isLeft, ORIGINATED.getLeftType(), ORIGINATED.getRightType());
    }

    private List<Relationship> findItemWithdrawnRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType withdrawRelationshipType = findWithdrawnRelationshipType(context, item, isLeft);
        if (withdrawRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, withdrawRelationshipType, isLeft);
    }

    private RelationshipType findWithdrawnRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, WITHDRAW.getLeftType(), WITHDRAW.getRightType());
    }

    private List<Relationship> findItemReinstateRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType reinstateRelationshipType = findReinstateRelationshipType(context, item, isLeft);
        if (reinstateRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, reinstateRelationshipType, isLeft);
    }

    private List<Relationship> findItemMergeRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType mergeRelationshipType = findMergedInRelationshipType(context, item, isLeft);
        if (mergeRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, mergeRelationshipType, isLeft);
    }

    private RelationshipType findReinstateRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findRelationshipType(context, item, isLeft, REINSTATE.getLeftType(), REINSTATE.getRightType());
    }

    private RelationshipType findRelationshipType(Context context, Item item, boolean isLeft, String leftwardType,
        String rightwardType) throws SQLException {

        EntityType type = entityTypeService.findByItem(context, item);
        if (type == null) {
            return null;
        }

        List<RelationshipType> relationshipTypes = relationshipTypeService.findByTypeAndTypeNames(context, type, isLeft,
            leftwardType, rightwardType);

        if (CollectionUtils.isEmpty(relationshipTypes)) {
            LOGGER.warn("No relationship type found with leftward {} and rightward {} for type", leftwardType,
                rightwardType, type.getLabel());
            return null;
        }

        if (relationshipTypes.size() > 1) {
            throw new IllegalStateException(
                "Multiple " + leftwardType + " relationship types found for entity type " + type.getLabel());
        }

        return relationshipTypes.get(0);
    }

}
