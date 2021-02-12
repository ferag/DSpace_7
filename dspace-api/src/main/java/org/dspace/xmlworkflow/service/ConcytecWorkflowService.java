/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.ConcytecFeedback;

/**
 * Service that handle the relationships between the item related to the
 * CONCYTEC workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface ConcytecWorkflowService {

    /**
     * Left type of the shadow copy relationship.
     */
    public static final String HAS_SHADOW_COPY_RELATIONSHIP = "hasShadowCopy";

    /**
     * Right type of the shadow copy relationship.
     */
    public static final String IS_SHADOW_COPY_RELATIONSHIP = "isShadowCopy";

    /**
     * Left type of the withdrawn relationship.
     */
    public static final String IS_CORRECTION_OF_ITEM_RELATIONSHIP = "isCorrectionOfItem";

    /**
     * Right type of the withdrawn relationship.
     */
    public static final String IS_CORRECTED_BY_ITEM_RELATIONSHIP = "isCorrectedByItem";

    /**
     * Left type of the withdrawn relationship.
     */
    public static final String IS_WITHDRAW_OF_ITEM_RELATIONSHIP = "isWithdrawOfItem";

    /**
     * Right type of the withdrawn relationship.
     */
    public static final String IS_WITHDRAWN_BY_ITEM_RELATIONSHIP = "isWithdrawnByItem";

    /**
     * Left type of the reinstate relationship.
     */
    public static final String IS_REINSTATEMENT_OF_ITEM_RELATIONSHIP = "isReinstatementOfItem";

    /**
     * Right type of the reinstate relationship.
     */
    public static final String IS_REINSTATED_BY_ITEM_RELATIONSHIP = "isReinstatedByItem";

    /**
     * Left type of the merge relationship.
     */
    public static final String IS_MERGED_IN_RELATIONSHIP = "isMergedIn";

    /**
     * Right type of the merge relationship.
     */
    public static final String IS_MERGE_OF_RELATIONSHIP = "isMergeOf";

    /**
     * Left type of the originated from relationship.
     */
    public static final String IS_ORIGINATED_FROM_IN_RELATIONSHIP = "isOriginatedFrom";

    /**
     * Right type of the originated from relationship.
     */
    public static final String IS_ORIGIN_OF_RELATIONSHIP = "isOriginOf";

    /**
     * Create a shadow copy relationship between the two given items
     * 
     * @param context        the DSpace context
     * @param item           the item that has a shadow copy
     * @param shadowItemCopy the item that is the shadow copy
     * @return the created relationship
     * @throws AuthorizeException if an authorization error occurs
     * @throws SQLException       if an SQL error occurs
     */
    Relationship createShadowRelationship(Context context, Item item, Item shadowItemCopy)
        throws AuthorizeException, SQLException;

    /**
     * Create a merged in relationship between the two given items
     * 
     * @param context   the DSpace context
     * @param leftItem  the item that is merged in
     * @param rightItem the item that is enriched
     * @return the created relationship
     * @throws AuthorizeException if an authorization error occurs
     * @throws SQLException       if an SQL error occurs
     */
    Relationship createMergedInRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException;

    /**
     * Create an originated from relationship between the two given items
     * 
     * @param context   the DSpace context
     * @param leftItem  the directorio's item that is originated from the
     *                  institution item
     * @param rightItem the institution's item that is the origin of the other item
     * @return the created relationship
     * @throws AuthorizeException if an authorization error occurs
     * @throws SQLException       if an SQL error occurs
     */
    Relationship createOriginatedFromRelationship(Context context, Item leftItem, Item rightItem)
        throws AuthorizeException, SQLException;

    /**
     * Find the shadow copy of the given item.
     *
     * @param context the DSpace context
     * @param item    the item that has a shadow copy
     * @return the item shadow copy, if any
     * @throws SQLException if an SQL error occurs
     */
    Item findShadowItemCopy(Context context, Item item) throws SQLException;

    /**
     * Find the item copied from the given shadow copy item.
     *
     * @param context        the DSpace context
     * @param shadowItemCopy the shadow copy item
     * @return the copied item, if any
     * @throws SQLException if an SQL error occurs
     */
    Item findCopiedItem(Context context, Item shadowItemCopy) throws SQLException;

    /**
     * Find the item that are origin of the given item.
     *
     * @param context the DSpace context
     * @param item    the item to search for
     * @return the copied item, if any
     * @throws SQLException if an SQL error occurs
     */
    List<Item> findOriginatedFromItems(Context context, Item item) throws SQLException;

    /**
     * Find the item where the given item is merged in.
     *
     * @param context the DSpace context
     * @param item    the item to search for
     * @return the item where the given item is merged in
     * @throws SQLException
     */
    Item findMergeOfItem(Context context, Item item) throws SQLException;

    /**
     * Add the Concytec feedback on the given item.
     *
     * @param context  the DSpace context
     * @param item     the item
     * @param feedback the feedback
     * @throws SQLException if an SQL error occurs
     */
    void setConcytecFeedback(Context context, Item item, ConcytecFeedback feedback) throws SQLException;

    /**
     * Returns the Concytec feedback regarding the given item, or
     * {@link ConcytecFeedback}.NONE if no feedback was already given.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return the Concytec feedback on the given item
     * @throws SQLException if an SQL error occurs
     */
    ConcytecFeedback getConcytecFeedback(Context context, Item item) throws SQLException;

    /**
     * Add a Concytec comment on the given item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @param comment the comment to add
     * @throws SQLException if an SQL error occurs
     */
    void setConcytecComment(Context context, Item item, String comment) throws SQLException;

    /**
     * Returns the Concytec comment regarding the given item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return the Concytec comment on the given item
     * @throws SQLException if an SQL error occurs
     */
    String getConcytecComment(Context context, Item item) throws SQLException;

    /**
     * Find the item to be withdraw related to the given item.
     *
     * @param context the DSpace context
     * @param item    the item that represents the withdraw request
     * @return the withdrawn item, if any
     * @throws SQLException if an SQL error occurs
     */
    Item findWithdrawnItem(Context context, Item item) throws SQLException;

    /**
     * Find the item to be reinstate related to the given item.
     *
     * @param context the DSpace context
     * @param item    the item that represents the reinstate request
     * @return the withdrawn item, if any
     * @throws SQLException if an SQL error occurs
     */
    Item findReinstateItem(Context context, Item item) throws SQLException;

}
