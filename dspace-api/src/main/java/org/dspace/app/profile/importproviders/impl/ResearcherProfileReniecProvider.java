/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.impl;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource.SourceId;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileReniecProvider implements ResearcherProfileProvider {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileReniecProvider.class);

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
            log.debug("Reniec profile provider configured for ePerson " + eperson.getID().toString()
                + " with dni " + metadataIdentifier.get().getValue());
            ConfiguredResearcherProfileProvider configured = new ConfiguredResearcherProfileProvider(
                    new ResearcherProfileSource(getSourceIdentifier(), metadataIdentifier.get().getValue()), this);
            return Optional.of(configured);
        }
        log.debug("Reniec metadata identifier not found for ePerson " + eperson.getID().toString());
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        final SourceId sourceId = source.selectSource(getSourceIdentifier()).get();
        return externalDataService.getExternalDataObject(sourceId.getSource(), sourceId.getId());
    }

    private Optional<MetadataValue> getMetadataIdentifier(EPerson eperson) {
        return eperson.getMetadata().stream().filter(metadata -> {
            log.debug("Parsing eperson metadata " + metadata.toString());
            return "perucris".equals(metadata.getMetadataField().getMetadataSchema().getName()) &&
                    "eperson".equals(metadata.getMetadataField().getElement()) &&
                    "dni".equals(metadata.getMetadataField().getQualifier());
        }).findFirst();
    }

    public void setExternalDataService(ExternalDataService externalDataService) {
        this.externalDataService = externalDataService;
    }

    @Override
    public void importSuggestions(Context context, Item profile, ResearcherProfileSource source)
            throws SolrServerException, IOException {
        // no suggestions for this provider
    }

}
