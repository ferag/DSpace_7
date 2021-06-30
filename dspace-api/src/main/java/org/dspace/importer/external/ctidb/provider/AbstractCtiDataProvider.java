/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.provider;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.importer.external.ctidb.CtiDatabaseImportFacade;
import org.dspace.importer.external.ctidb.suggestion.AbstractCtiSuggestionLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract implementations for a CtiDataProvider.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public abstract class AbstractCtiDataProvider extends AbstractExternalDataProvider {

    private String sourceIdentifier;

    private CtiDatabaseImportFacade ctiDatabaseImportFacade;

    @Autowired(required = true)
    public void setCtiDatabaseImport(CtiDatabaseImportFacade ctiDatabaseImportFacade) {
        this.ctiDatabaseImportFacade = ctiDatabaseImportFacade;
    }

    /**
     * Generic setter for the sourceIdentifier
     * 
     * @param sourceIdentifier The sourceIdentifier to be set on this CtiDataProvider
     */
    @Autowired(required = true)
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public CtiDatabaseImportFacade getCtiDatabaseImportFacade() {
        return this.ctiDatabaseImportFacade;
    }

    public abstract AbstractCtiSuggestionLoader getCtiSuggestionLoader();

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        throw new NotImplementedException();
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        throw new NotImplementedException();
    }

    protected abstract ExternalDataObject getExternalDataObjectImpl(String ctiId);

    protected Optional<MetadataValueDTO> getCtiProfileHolderMetadata(String suggestionCtiId) {

        final String ctiProfileUuid = getCtiSuggestionLoader().findCtiProfileUUID(suggestionCtiId);

        if (ctiProfileUuid != null) {
            return Optional.of(new MetadataValueDTO("perucris", "holder", "ctiprofile", null, ctiProfileUuid));
        }

        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        ExternalDataObject externalDataObject = getExternalDataObjectImpl(id);

        getCtiProfileHolderMetadata(id).ifPresent(externalDataObject.getMetadata()::add);

        externalDataObject.setId(id);

        externalDataObject.setSource(this.getSourceIdentifier());

        return Optional.of(externalDataObject);
    }

}
