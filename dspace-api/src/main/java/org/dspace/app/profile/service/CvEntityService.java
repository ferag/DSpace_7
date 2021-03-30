/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.sql.SQLException;

import org.dspace.app.profile.CvEntity;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service to handle CV entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface CvEntityService {

    /**
     * Create a new CV entity starting from the given item.
     *
     * @param context the DSpace context
     * @param item    the source item
     * @return the created CV entity
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    CvEntity createFromItem(Context context, Item item) throws SQLException, AuthorizeException;
}
