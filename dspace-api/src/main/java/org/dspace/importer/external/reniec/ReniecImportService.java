/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.reniec;

import java.util.Collection;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This import service does not perform queries against external services, but lookup for a reniec object given a DNI.
 * <p>
 * For metadata field mapping it is mandatory the usage of an instance of {@link ReniecMetadataFieldMapping}
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ReniecImportService extends AbstractImportMetadataSourceService<ReniecDTO> implements QuerySource {

    private final ReniecProvider reniecProvider;

    @Autowired
    public ReniecImportService(ReniecProvider reniecProvider,
                               ReniecMetadataFieldMapping metadataFieldMapping) {
        this.reniecProvider = reniecProvider;
        setMetadataFieldMapping(metadataFieldMapping);
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public String getImportSource() {
        return "Reniec";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        ReniecDTO reniecDTO = this.reniecProvider.getReniecObject(id);
        return transformSourceRecords(reniecDTO);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new UnsupportedOperationException("This service does not support queries");
    }


}
