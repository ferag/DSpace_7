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
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.ctidb.CtiDatabaseImportFacade;
import org.dspace.importer.external.ctidb.suggestion.CtiPatentSuggestionLoader;
import org.dspace.importer.external.ctidb.suggestion.CtiProjectSuggestionLoader;
import org.dspace.importer.external.ctidb.suggestion.CtiPublicationSuggestionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of ResearcherProfileProvider for Cti Source.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileCtiProvider implements ResearcherProfileProvider {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileCtiProvider.class);

    @Autowired
    private CtiDatabaseImportFacade ctiDatabaseImport;

    @Autowired
    private CtiPublicationSuggestionLoader ctiPublicationSuggestionLoader;

    @Autowired
    private CtiProjectSuggestionLoader ctiProjectSuggestionLoader;

    @Autowired
    private CtiPatentSuggestionLoader ctiPatentSuggestionLoader;

    public ResearcherProfileCtiProvider(CtiDatabaseImportFacade ctiDatabaseImport) {
        this.ctiDatabaseImport = ctiDatabaseImport;
    }

    public Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList) {

        ResearcherProfileSource source = new ResearcherProfileSource();

        this.configureDniSource(eperson, source);

        this.configureOrcidSource(eperson, source);

        if (source.getSources().isEmpty()) {
            log.debug("Nor orcid or dni metadata identifier found for ePerson " + eperson.getID().toString());
            return Optional.empty();
        }

        ConfiguredResearcherProfileProvider configured = new ConfiguredResearcherProfileProvider(
                source, this);
        log.debug("Cti profile provider configured for ePerson " + eperson.getID().toString()
                + " with sources " + source.toString());
        return Optional.of(configured);

    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        return Optional.ofNullable(ctiDatabaseImport.getCtiProfile(source));
    }

    private void configureOrcidSource(EPerson eperson, ResearcherProfileSource source) {
        Optional<MetadataValue> metadataOpt = eperson.getMetadata().stream().filter(metadata -> {
            log.debug("Parsing eperson metadata " + metadata.toString());
            return ("perucris".equals(metadata.getSchema()) &&
                    "eperson".equals(metadata.getElement()) &&
                    "orcid".equals(metadata.getQualifier())) ;
        }).findFirst();
        if (metadataOpt.isPresent()) {
            source.addSource("orcid", metadataOpt.get().getValue());
        }
    }

    private void configureDniSource(EPerson eperson, ResearcherProfileSource source) {
        Optional<MetadataValue> metadataOpt = eperson.getMetadata().stream().filter(metadata -> {
            log.debug("Parsing eperson metadata " + metadata.toString());
            return ("perucris".equals(metadata.getSchema()) &&
                    "eperson".equals(metadata.getElement()) &&
                    "dni".equals(metadata.getQualifier())) ;
        }).findFirst();
        if (metadataOpt.isPresent()) {
            source.addSource("dni", metadataOpt.get().getValue());
        }
    }

    public void setCtiDatabaseImport(CtiDatabaseImportFacade ctiDatabaseImport) {
        this.ctiDatabaseImport = ctiDatabaseImport;
    }

    @Override
    public void importSuggestions(Context context, Item profile, ResearcherProfileSource source)
            throws SolrServerException, IOException {

        List<ExternalDataObject> ctiSuggestions = ctiDatabaseImport.getCtiSuggestions(source);
        ctiPublicationSuggestionLoader.importSuggestions(context, profile, ctiSuggestions);
        ctiProjectSuggestionLoader.importSuggestions(context, profile, ctiSuggestions);
        ctiPatentSuggestionLoader.importSuggestions(context, profile, ctiSuggestions);
    }

}
