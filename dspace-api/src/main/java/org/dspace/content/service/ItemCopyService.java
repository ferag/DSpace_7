/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * Service to handle items copies.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface ItemCopyService {

    /**
     * Create a new {@link WorkspaceItem} into the given collection copying the data
     * of the given item.
     * 
     * @param context    the DSpace context
     * @param itemToCopy the item to copy
     * @param collection the own collection of the copied item
     * @return the WOrkspaceItem related to the copied item
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException        if an IO error occurs
     */
    WorkspaceItem copy(Context context, Item itemToCopy, Collection collection)
        throws AuthorizeException, SQLException, IOException;
}
