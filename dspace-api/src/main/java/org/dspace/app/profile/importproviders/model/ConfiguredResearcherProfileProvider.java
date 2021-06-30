/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.model;

import java.io.IOException;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;

/**
 * A configured, ready to be invoked provider for external metadata used to enrich a Research Profile object
 * and import related suggestions.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ConfiguredResearcherProfileProvider {

    /**
     * Contains the information of the targeted profile.
     */
    private ResearcherProfileSource source;

    /**
     * The profile provider to be used.
     */
    private ResearcherProfileProvider provider;

    public ConfiguredResearcherProfileProvider(ResearcherProfileSource source, ResearcherProfileProvider provider) {
        this.source = source;
        this.provider = provider;
    }

    public ResearcherProfileSource getSource() {
        return source;
    }

    public ResearcherProfileProvider getProvider() {
        return provider;
    }

    public Optional<ExternalDataObject> getExternalDataObject() {
        return provider.getExternalDataObject(source);
    }

    public void importSuggestions(Context context, Item profile)
            throws SolrServerException, IOException {
        provider.importSuggestions(context, profile, source);
    }
}
