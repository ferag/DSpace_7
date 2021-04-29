/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Service that creates a researcher profile starting from a given source.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface ImportResearcherProfileService {

    /**
     * Return the list of configured researcher profile metadata provider for the eperson and the given uriList.
     * @param eperson
     * @param uriList
     * @return
     */
    List<ConfiguredResearcherProfileProvider> getConfiguredProfileProvider(EPerson eperson, List<URI> uriList);

    /**
     * Import profile from an external source
     * @param context
     * @param source
     * @param collection
     * @return
     */
    Item importProfile(Context context, EPerson eperson, URI source, Collection collection)
            throws AuthorizeException, SQLException;
}
