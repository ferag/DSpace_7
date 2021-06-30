/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;

/**
 * Provider of Metadata for the enrichment of a Researcher Profile object.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public interface ResearcherProfileProvider {

    /**
     * Configure the provider with context.
     * @param eperson owner of the researcher profile
     * @param uriList uri lists containing sources
     * @return
     */
    Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList);

    /**
     * Get the ResearcherProfile externalDataObject according to the configured source.
     * @param source the configured source
     * @return
     */
    Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source);

    /**
     * Import suggestions with profile as a target according to the configured source.
     * @param context context
     * @param profile target profile
     * @param source the configured source
     * @throws SolrServerException
     * @throws IOException
     */
    void importSuggestions(Context context, Item profile, ResearcherProfileSource source)
            throws SolrServerException, IOException;

}
