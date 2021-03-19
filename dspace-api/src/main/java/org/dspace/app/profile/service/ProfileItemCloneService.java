/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service to handle profile item clones.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface ProfileItemCloneService {

    /**
     * Create a clone of the given profileItem and binds it with the given
     * personItem.
     *
     * @param context     the DSpace context
     * @param profileItem the profile item
     * @param personItem  the person item
     * @throws SQLException              if an SQL error occurs
     * @throws AuthorizeException        if an authorization check fails
     * @throws IOException               if an IO error occurs
     * @throws IllegalArgumentException  if the relationship type of the personItem
     *                                   is not consistent
     * @throws ResourceConflictException if the given profileItem has already a
     *                                   clone
     */
    public void cloneProfile(Context context, Item profileItem, Item personItem)
        throws SQLException, AuthorizeException, IOException;
}
