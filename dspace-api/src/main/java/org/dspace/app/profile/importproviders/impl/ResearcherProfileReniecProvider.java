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
import org.dspace.content.MetadataValue;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class ResearcherProfileReniecProvider implements ResearcherProfileProvider {

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
        Optional<MetadataValue> metadataIdentifier = getMetadataIdentifier(eperson);
        if (metadataIdentifier.isPresent()) {
            ConfiguredResearcherProfileProvider configured = new ConfiguredResearcherProfileProvider(
                    new ResearcherProfileSource(getSourceIdentifier(), metadataIdentifier.get().getValue()), this);
            return Optional.of(configured);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        return externalDataService.getExternalDataObject(source.getSource(), source.getId());
    }

    private Optional<MetadataValue> getMetadataIdentifier(EPerson eperson) {
        return eperson.getMetadata().stream().filter(metadata -> {
            return "perucris".equals(metadata.getMetadataField().getMetadataSchema().getName()) &&
                    "eperson".equals(metadata.getMetadataField().getElement()) &&
                    "dni".equals(metadata.getMetadataField().getQualifier());
        }).findFirst();
    }

    public void setExternalDataService(ExternalDataService externalDataService) {
        this.externalDataService = externalDataService;
    }

}
