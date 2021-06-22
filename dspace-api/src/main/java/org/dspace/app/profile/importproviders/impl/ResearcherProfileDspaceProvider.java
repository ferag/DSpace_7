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
public class ResearcherProfileDspaceProvider implements ResearcherProfileProvider {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileDspaceProvider.class);

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
            if (source.selectSource(getSourceIdentifier()).isPresent()) {
                log.debug("Matching ResearcherProfileSource found for uri=" + uri + ", " + source.toString());
                ConfiguredResearcherProfileProvider configured =
                        new ConfiguredResearcherProfileProvider(source, this);
                return Optional.of(configured);
            }
        }
        log.debug("No Dspace item found for the provided uriList");
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        final SourceId sourceId = source.selectSource(getSourceIdentifier()).get();
        return externalDataService.getExternalDataObject(sourceId.getSource(), sourceId.getId());
    }

    @Override
    public void importSuggestions(Context context, Item profile, ResearcherProfileSource source)
            throws SolrServerException, IOException {
        // no suggestions for this provider
    }




}
