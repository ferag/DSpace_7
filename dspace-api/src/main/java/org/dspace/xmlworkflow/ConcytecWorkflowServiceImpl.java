/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.dspace.xmlworkflow.ConcytecFeedback.FEEDBACK_SEPARATOR;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
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
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
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
    private ItemService itemService;

    @Override
    public Relationship createShadowRelationship(Context context, Item item, Item shadowItemCopy)
        throws AuthorizeException, SQLException {
        RelationshipType shadowRelationshipType = findShadowRelationshipType(context, item, shadowItemCopy);
        if (shadowRelationshipType == null) {
            LOGGER.warn("No shadow copy relationship type found for the left item {} and right item {}",
                item.getID(), shadowItemCopy.getID());
            return null;
        }
        return relationshipService.create(context, item, shadowItemCopy, shadowRelationshipType, true);
    }

    @Override
    public Relationship createMergedInRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException {
        RelationshipType mergedInRelationshipType = findMergedInRelationshipType(context, leftItem, true);
        if (mergedInRelationshipType == null) {
            LOGGER.warn("No merged in relationship type found for the left item {}", leftItem.getID());
            return null;
        }
        return relationshipService.create(context, leftItem, rightItem, mergedInRelationshipType, true);
    }

    @Override
    public Relationship createOriginatedFromRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException {
        RelationshipType originatedFromType = findOriginatedFromRelationshipType(context, leftItem, rightItem);
        if (originatedFromType == null) {
            LOGGER.warn("No originated from relationship type found for the left item {} and right item {}",
                leftItem.getID(), rightItem.getID());
            return null;
        }
        return relationshipService.create(context, leftItem, rightItem, originatedFromType, true);
    }

    @Override
    public Relationship createCloneRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException {
        RelationshipType cloneRelationshipType = findCloneRelationshipType(context, leftItem, true);
        if (cloneRelationshipType == null) {
            LOGGER.warn("No clone relationship type found for the left item {}", leftItem.getID());
            return null;
        }
        return relationshipService.create(context, leftItem, rightItem, cloneRelationshipType, true);
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
    public Item findClone(Context context, Item item) throws SQLException {
        List<Relationship> cloneRelationships = findItemCloneRelationships(context, item, false);

        if (CollectionUtils.isEmpty(cloneRelationships)) {
            return null;
        }

        if (cloneRelationships.size() > 1) {
            throw new IllegalStateException("The item " + item.getID() + " is cloned by more than one item");
        }

        return cloneRelationships.get(0).getLeftItem();
    }

    @Override
    public Item findClonedItem(Context context, Item cloneItem) throws SQLException {
        List<Relationship> cloneRelationships = findItemCloneRelationships(context, cloneItem, true);

        if (CollectionUtils.isEmpty(cloneRelationships)) {
            return null;
        }

        if (cloneRelationships.size() > 1) {
            throw new IllegalStateException("The item " + cloneItem.getID() + " is the clone of more than one item");
        }

        return cloneRelationships.get(0).getRightItem();
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
        List<RelationshipType> originatedFromTypes = findOriginatedFromRelationshipTypes(context, item, true);
        if (CollectionUtils.isEmpty(originatedFromTypes)) {
            return Collections.emptyList();
        }

        return relationshipService.findByItemAndRelationshipTypes(context, item, originatedFromTypes, true).stream()
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

    @Override
    public List<Item> findMergedInItems(Context context, Item item) throws SQLException {
        List<Relationship> relationships = findItemMergeRelationships(context, item, false);

        if (CollectionUtils.isEmpty(relationships)) {
            return Collections.emptyList();
        }

        return relationships.stream().map(Relationship::getLeftItem).collect(Collectors.toList());
    }

    private List<Relationship> findItemShadowRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        List<RelationshipType> shadowRelationshipTypes = findShadowRelationshipTypes(context, item, isLeft);
        if (CollectionUtils.isEmpty(shadowRelationshipTypes)) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipTypes(context, item, shadowRelationshipTypes, isLeft);
    }

    private List<Relationship> findItemCloneRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType cloneRelationshipType = findCloneRelationshipType(context, item, isLeft);
        if (cloneRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, cloneRelationshipType, isLeft);
    }

    private List<RelationshipType> findShadowRelationshipTypes(Context context, Item item, boolean isLeft)
        throws SQLException {
        return relationshipTypeService.findByItemAndTypeNames(context, item, isLeft, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType());
    }

    private RelationshipType findShadowRelationshipType(Context context, Item item, Item shadowCopy)
        throws SQLException {
        return relationshipTypeService.findByItemsAndTypeNames(context, item, shadowCopy, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType());
    }

    private RelationshipType findOriginatedFromRelationshipType(Context context, Item leftItem, Item rightItem)
        throws SQLException {
        return relationshipTypeService.findByItemsAndTypeNames(context, leftItem, rightItem, ORIGINATED.getLeftType(),
            ORIGINATED.getRightType());
    }

    private RelationshipType findMergedInRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return findSingleRelationshipType(context, item, isLeft, MERGED.getLeftType(), MERGED.getRightType());
    }

    private List<RelationshipType> findOriginatedFromRelationshipTypes(Context ctx, Item item, boolean isLeft)
        throws SQLException {
        return relationshipTypeService.findByItemAndTypeNames(ctx, item, isLeft, ORIGINATED.getLeftType(),
            ORIGINATED.getRightType());
    }

    private RelationshipType findCloneRelationshipType(Context ctx, Item item, boolean isLeft)
        throws SQLException {
        return findSingleRelationshipType(ctx, item, isLeft, CLONE.getLeftType(), CLONE.getRightType());
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
        return findSingleRelationshipType(context, item, isLeft, WITHDRAW.getLeftType(), WITHDRAW.getRightType());
    }

    private List<Relationship> findItemReinstateRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        List<RelationshipType> reinstateRelationshipTypes = findReinstateRelationshipType(context, item, isLeft);
        if (CollectionUtils.isEmpty(reinstateRelationshipTypes)) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipTypes(context, item, reinstateRelationshipTypes, isLeft);
    }

    private List<Relationship> findItemMergeRelationships(Context context, Item item, boolean isLeft)
        throws SQLException {
        RelationshipType mergeRelationshipType = findMergedInRelationshipType(context, item, isLeft);
        if (mergeRelationshipType == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(context, item, mergeRelationshipType, isLeft);
    }

    private List<RelationshipType> findReinstateRelationshipType(Context context, Item item, boolean isLeft)
        throws SQLException {
        return relationshipTypeService.findByItemAndTypeNames(context, item, isLeft, REINSTATE.getLeftType(),
            REINSTATE.getRightType());
    }

    private RelationshipType findSingleRelationshipType(Context context, Item item, boolean isLeft, String leftType,
        String rightType) throws SQLException {

        List<RelationshipType> relationships = relationshipTypeService.findByItemAndTypeNames(context, item, isLeft,
            leftType, rightType);

        if (CollectionUtils.isEmpty(relationships)) {
            LOGGER.warn("No relationship type found with leftward {} and rightward {} for item with id {}", leftType,
                rightType, item.getID());
            return null;
        }

        if (relationships.size() > 1) {
            throw new IllegalStateException(
                "Multiple " + leftType + " relationship types found for item " + item.getID());
        }

        return relationships.get(0);
    }

}
