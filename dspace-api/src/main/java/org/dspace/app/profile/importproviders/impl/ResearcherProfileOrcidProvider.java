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
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileOrcidProvider implements ResearcherProfileProvider {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileOrcidProvider.class);

    @Autowired
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    public Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList) {
        Optional<MetadataValue> metadataIdentifier = getMetadataIdentifier(eperson);
        if (metadataIdentifier.isPresent()) {
            log.debug("Orcid profile provider configured for ePerson " + eperson.getID().toString()
                + " with orcid " + metadataIdentifier.get().getValue());
            ConfiguredResearcherProfileProvider configured = new ConfiguredResearcherProfileProvider(
                    new ResearcherProfileSource("orcid", metadataIdentifier.get().getValue()), this);
            return Optional.of(configured);
        }
        log.debug("Orcid metadata identifier not found for ePerson " + eperson.getID().toString());
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        final SourceId sourceId = source.selectSource("orcid").get();
        try {
            return orcidV3AuthorDataProvider.getExternalDataObject(sourceId.getId());
        } catch (Exception e) {
            log.warn("Unable to create external data object from orcid id {} : {}", sourceId,
                     e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<MetadataValue> getMetadataIdentifier(EPerson eperson) {
        return eperson.getMetadata().stream().filter(metadata -> {
            log.debug("Parsing eperson metadata " + metadata.toString());
            return "perucris".equals(metadata.getMetadataField().getMetadataSchema().getName()) &&
                    "eperson".equals(metadata.getMetadataField().getElement()) &&
                    "orcid".equals(metadata.getMetadataField().getQualifier());
        }).findFirst();
    }

    public void setOrcidV3AuthorDataProvider(OrcidV3AuthorDataProvider orcidV3AuthorDataProvider) {
        this.orcidV3AuthorDataProvider = orcidV3AuthorDataProvider;
    }

    @Override
    public void importSuggestions(Context context, Item profile, ResearcherProfileSource source)
            throws SolrServerException, IOException {
        // no suggestions for this provider
    }

}
