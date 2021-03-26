/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.impl;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class ResearcherProfileDspaceProvider implements ResearcherProfileProvider {

    @Autowired
    private ExternalDataService externalDataService;

    private String sourceIdentifier;

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getSourceIdentifier() {
        return this.sourceIdentifier;
    }

    public Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList) {
        for (URI uri : uriList) {
            ResearcherProfileSource source = new ResearcherProfileSource(uri);
            if (source.getSource().equals(getSourceIdentifier())) {
                ConfiguredResearcherProfileProvider configured =
                        new ConfiguredResearcherProfileProvider(source, this);
                return Optional.of(configured);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        return externalDataService.getExternalDataObject(source.getSource(), source.getId());
    }




}
