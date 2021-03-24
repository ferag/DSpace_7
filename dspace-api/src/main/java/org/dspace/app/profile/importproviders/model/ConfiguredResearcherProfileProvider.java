package org.dspace.app.profile.importproviders.model;

import java.util.Optional;

import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.external.model.ExternalDataObject;

/**
 * A configured, ready to be invoked provider for external metadata used to enrich a Research Profile object.
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

    public Optional<ExternalDataObject> getExternalDataObject() {
        return provider.getExternalDataObject(source);
    }
}
