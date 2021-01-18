/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import java.sql.SQLException;

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
    public static final String IS_WITHDRAW_OF_ITEM_RELATIONSHIP = "isWithdrawOfItem";

    /**
     * Right type of the withdrawn relationship.
     */
    public static final String IS_WITHDRAWN_BY_ITEM_RELATIONSHIP = "isWithdrawnByItem";

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

}
