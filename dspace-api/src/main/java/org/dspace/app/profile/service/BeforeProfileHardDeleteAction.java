/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Contract that defines custom actions to be performed before a researcher
 * profile hard deletion.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface BeforeProfileHardDeleteAction {

    /**
     * Apply the action on the given profile item-
     *
     * @param context     the DSpace context
     * @param profileItem the item related to the researcher profile
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an Authorization error occurs
     */
    void apply(Context context, Item profileItem) throws SQLException, AuthorizeException;
}
