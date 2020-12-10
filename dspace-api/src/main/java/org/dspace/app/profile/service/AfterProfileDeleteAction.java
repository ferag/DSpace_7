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
 * Contract that defines custom actions to be performed after a researcher profile deletion
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public interface AfterProfileDeleteAction {
    /**
     *
     *
     * @param context current context
     * @param profileItem item where actions should be executed
     */
    void apply(Context context, Item profileItem) throws SQLException, AuthorizeException;
}
