/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.suggestion;

import static org.dspace.app.suggestion.SolrSuggestionStorageService.PROCESSED;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.SOURCE;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.SUGGESTION_ID;
import static org.dspace.app.suggestion.SolrSuggestionStorageService.TARGET_ID;
import static org.dspace.app.suggestion.SuggestionUtils.getAllEntriesByMetadatum;
import static org.dspace.app.suggestion.SuggestionUtils.getFirstEntryByMetadatum;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common implementation of SolrSuggestionProvider for cti suggestions.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public abstract class AbstractCtiSuggestionLoader extends SolrSuggestionProvider {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected boolean isExternalDataObjectPotentiallySuggested(Context context, ExternalDataObject externalDataObject) {
        return StringUtils.equals(externalDataObject.getSource(), getSourceName());
    }

    /**
     * Retrieve the target of a suggestions by its id.
     * @param suggestionId
     * @return
     */
    public String findCtiProfileUUID(String suggestionId) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(1);
        solrQuery.setQuery("*:*");
        solrQuery.addFilterQuery(
                SOURCE + ":" + this.getSourceName(),
//                TARGET_ID + ":" + target.toString(),
                SUGGESTION_ID + ":\"" + suggestionId + "\"",
                PROCESSED + ":false");
        QueryResponse response = null;
        try {
            response = getSolr().query(solrQuery);
            for (SolrDocument solrDoc : response.getResults()) {
                return (String)solrDoc.getFieldValue(TARGET_ID); // convertSolrDoc(context, solrDoc);
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Import ctiSuggestions for the profile target.
     * @param context
     * @param profile
     * @param ctiSuggestions
     * @throws SolrServerException
     * @throws IOException
     */
    public void importSuggestions(Context context, Item profile, List<ExternalDataObject> ctiSuggestions)
            throws SolrServerException, IOException {

        final List<Suggestion> suggestions = ctiSuggestions.stream()
                .filter(object -> isExternalDataObjectPotentiallySuggested(context, object))
                .map(object -> convertToSuggestion(profile, object))
                .collect(Collectors.toList());

        for (Suggestion suggestion : suggestions) {
            getSolrSuggestionStorageService().addSuggestion(suggestion, false, false);
        }

        getSolrSuggestionStorageService().commit();

    }

    protected Suggestion convertToSuggestion(Item profile, ExternalDataObject externalDataObject) {
        Suggestion suggestion = new Suggestion(getSourceName(), profile, externalDataObject.getId());
        suggestion.setDisplay(getFirstEntryByMetadatum(externalDataObject, "dc", "title", null));
        suggestion.getEvidences().add(buildSuggestionEvidence());
        suggestion.setExternalSourceUri(getExternalSourceUri(externalDataObject.getId()));

        buildMetadataValue("dc.title", externalDataObject).ifPresent(suggestion.getMetadata()::add);
        buildMetadataValue("dc.date.issued", externalDataObject).ifPresent(suggestion.getMetadata()::add);
        buildMetadataValue("perucris.identifier.cti", externalDataObject).ifPresent(suggestion.getMetadata()::add);

        suggestion.getMetadata().addAll(buildMetadataValues("dc.source", externalDataObject));
        suggestion.getMetadata().addAll(buildMetadataValues("dc.contributor.author", externalDataObject));

        return suggestion;
    }

    private SuggestionEvidence buildSuggestionEvidence() {
        String notes = "The cti suggestion was retrieved from the CTI database"
                + " searching either by researcher ORCID or DNI.";
        return new SuggestionEvidence(this.getClass().getSimpleName(), 100d, notes);
    }

    protected List<MetadataValueDTO> buildMetadataValues(String metadataField, ExternalDataObject externalDataObject) {
        MetadataFieldName field = new MetadataFieldName(metadataField);
        return getAllEntriesByMetadatum(externalDataObject, field.SCHEMA, field.ELEMENT, field.QUALIFIER).stream()
            .map(value -> new MetadataValueDTO(field.SCHEMA, field.ELEMENT, field.QUALIFIER, null, value))
            .collect(Collectors.toList());
    }

    protected Optional<MetadataValueDTO> buildMetadataValue(String metadataField,
            ExternalDataObject externalDataObject) {
        MetadataFieldName field = new MetadataFieldName(metadataField);
        String value = getFirstEntryByMetadatum(externalDataObject, field.SCHEMA, field.ELEMENT, field.QUALIFIER);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new MetadataValueDTO(field.SCHEMA, field.ELEMENT, field.QUALIFIER, null, value));
    }

    protected String getExternalSourceUri(String recordId) {
        String serverUrl = configurationService.getProperty("dspace.server.url");
        return serverUrl + "/api/integration/externalsources/" + getSourceName() + "/entryValues/" + recordId;
    }



}
