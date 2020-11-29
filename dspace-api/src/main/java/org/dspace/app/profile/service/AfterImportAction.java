/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;

/**
 * Defines a contract that applies on items created with {@link ImportResearcherProfileService}
 * in case of need provide one or many implementations and inject them into {@link ImportResearcherProfileServiceImpl}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface AfterImportAction {
    /**
     * Manipulates and performs actions on a given item.
     *
     * @param context
     * @param item item to be manipulated by this action implementation
     * @param externalDataObject external data object used as source of created item
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    void applyTo(Context context, Item item, ExternalDataObject externalDataObject) throws SQLException,
        AuthorizeException;
}
