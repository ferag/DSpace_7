/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.sql.SQLException;
import java.util.List;

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

    /**
     * Find all the CV entities related to the given profile item.
     *
     * @param context     the DSpace context
     * @param profileItem the profile item to search for
     * @return the CV entities related to the profile
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    List<CvEntity> findByProfileItem(Context context, Item profileItem) throws SQLException, AuthorizeException;

    /**
     * Delete all the CV entities related to the given profile item and create a
     * withdrawal request for each of the relative items on directorio, if present
     * and if already archived. If the items on the directory side are still in
     * workflow they are completely deleted.
     * 
     * @param context     the DSpace context
     * @param profileItem the profile item to search for
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    void deleteByProfileItem(Context context, Item profileItem) throws SQLException, AuthorizeException;

    /**
     * Delete the CVEntity and create a withdrawal request for each of the relative
     * items on directorio, if present and if already archived. If the items on the
     * directory side are still in workflow they are completely deleted.
     * 
     * @param context     the DSpace context
     * @param profileItem the profile item to search for
     * @throws SQLException       if an SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    void delete(Context context, CvEntity cvEntity) throws SQLException, AuthorizeException;

}
