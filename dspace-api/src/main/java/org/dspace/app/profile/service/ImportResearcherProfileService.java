/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.net.URI;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service that creates a researcher profile starting from a given source.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface ImportResearcherProfileService {

    /**
     * @param context
     * @param source
     * @param collection
     * @return
     */
    Item importProfile(Context context, URI source, Collection collection) throws AuthorizeException, SQLException;
}
