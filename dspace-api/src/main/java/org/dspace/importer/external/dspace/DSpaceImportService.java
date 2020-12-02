/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.dspace;

import java.util.Collection;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * This import service does not perform queries against external services, but lookup for items
 * into same DSpace instance.
 * <p>
 * For metadata field mapping it is mandatory the usage of an instance of {@link DSpaceInternalMetadataFieldMapping}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceImportService extends AbstractImportMetadataSourceService<Item> implements QuerySource {

    private final ItemService itemService;
    private final RequestService requestService;

    @Autowired
    public DSpaceImportService(ItemService itemService, RequestService requestService,
                               DSpaceInternalMetadataFieldMapping metadataFieldMapping) {
        this.itemService = itemService;
        this.requestService = requestService;
        setMetadataFieldMapping(metadataFieldMapping);
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        Context context = (Context) requestService.getCurrentRequest().getAttribute("context");
        try {
            Item item = itemService.find(context, UUID.fromString(id));
            return transformSourceRecords(item);
        } catch (Exception e) {
            throw new MetadataSourceException(e);
        }
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

    @Override
    public String getImportSource() {
        return "DSpace";
    }
}
