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
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;

/**
 * Service that handle the relationships between the item related to the
 * CONCYTEC workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface ConcytecWorkflowService {

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
    void addConcytecFeedback(Context context, Item item, ConcytecFeedback feedback) throws SQLException;

    /**
     * Add the Concytec feedback on the given item.
     *
     * @param context  the DSpace context
     * @param item     the item
     * @param feedback the feedback
     * @throws SQLException if an SQL error occurs
     */
    void addConcytecFeedback(Context context, Item item, String feedback) throws SQLException;

    /**
     * Add the Concytec feedback on the given item.
     *
     * @param context  the DSpace context
     * @param item     the item
     * @param relation the relation between the given item and the item related to
     *                 the concytec feedback
     * @param feedback the feedback
     * @param comment  an optional concytec comment
     * @throws SQLException if an SQL error occurs
     */

    void addConcytecFeedback(Context context, Item item, ConcytecWorkflowRelation relation, ConcytecFeedback feedback,
        String comment) throws SQLException;

    /**
     * Add the Concytec feedback on the given item.
     *
     * @param context  the DSpace context
     * @param item     the item
     * @param feedback the feedback
     * @param comment  an optional concytec comment
     * @throws SQLException if an SQL error occurs
     */

    void addConcytecFeedback(Context context, Item item, ConcytecFeedback feedback, String comment)
        throws SQLException;

    /**
     * Returns the last Concytec feedback regarding the given item, or
     * {@link ConcytecFeedback}.NONE if no feedback was already given.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return the last Concytec feedback on the given item
     * @throws SQLException if an SQL error occurs
     */
    ConcytecFeedback getLastConcytecFeedback(Context context, Item item) throws SQLException;

    /**
     * Add a Concytec comment on the given item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @param comment the comment to add
     * @throws SQLException if an SQL error occurs
     */
    void addConcytecComment(Context context, Item item, String comment) throws SQLException;

    /**
     * Returns the last Concytec comment regarding the given item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return the last Concytec comment on the given item
     * @throws SQLException if an SQL error occurs
     */
    String getLastConcytecComment(Context context, Item item) throws SQLException;

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
