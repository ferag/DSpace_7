/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a Relationship.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class RelationshipMatcher extends TypeSafeMatcher<Relationship> {

    private final Item leftItem;

    private final Item rightItem;

    private final String leftwardType;

    private final String rightwardType;

    private RelationshipMatcher(Item leftItem, Item rightItem, String leftwardType, String rightwardType) {
        this.leftItem = leftItem;
        this.rightItem = rightItem;
        this.leftwardType = leftwardType;
        this.rightwardType = rightwardType;
    }

    public static RelationshipMatcher with(Item leftItem, Item rightItem, ConcytecWorkflowRelation relation) {
        return new RelationshipMatcher(leftItem, rightItem, relation.getLeftType(), relation.getRightType());
    }

    public static RelationshipMatcher withRightItem(Item rightItem, ConcytecWorkflowRelation relation) {
        return new RelationshipMatcher(null, rightItem, relation.getLeftType(), relation.getRightType());
    }

    public static RelationshipMatcher withLeftItem(Item leftItem, ConcytecWorkflowRelation relation) {
        return new RelationshipMatcher(leftItem, null, relation.getLeftType(), relation.getRightType());
    }

    public static RelationshipMatcher with(Item leftItem, Item rightItem, String leftwardType, String rightwardType) {
        return new RelationshipMatcher(leftItem, rightItem, leftwardType, rightwardType);
    }

    public static RelationshipMatcher withRightItem(Item rightItem, String leftwardType, String rightwardType) {
        return new RelationshipMatcher(null, rightItem, leftwardType, rightwardType);
    }

    public static RelationshipMatcher withLeftItem(Item leftItem, String leftwardType, String rightwardType) {
        return new RelationshipMatcher(leftItem, null, leftwardType, rightwardType);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Relationship with the following attributes: [");
        if (leftItem != null) {
            description.appendText(" leftItem: " + leftItem.getID() + ", ");
        }
        if (rightItem != null) {
            description.appendText(" rightItem: " + rightItem.getID() + ", ");
        }

        description.appendText(" leftwardType: " + leftwardType + ", ");
        description.appendText(" rightwardType: " + rightwardType + "]");
    }

    @Override
    protected boolean matchesSafely(Relationship relationship) {
        return (leftItem == null || leftItem.equals(relationship.getLeftItem()))
            && (rightItem == null || rightItem.equals(relationship.getRightItem()))
            && leftwardType.equals(relationship.getRelationshipType().getLeftwardType())
            && rightwardType.equals(relationship.getRelationshipType().getRightwardType());
    }

}
