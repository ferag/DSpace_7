/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.dspace;

import java.util.Collection;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;


/**
 * This import service does not perform queries against external services, but lookup for items
 * into same DSpace instance.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceImportService extends AbstractImportMetadataSourceService<Item> implements QuerySource {

    @Override
    public void init() throws Exception {

    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return null;
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return 0;
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return 0;
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return null;
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        return null;
    }

    @Override
    public String getImportSource() {
        return null;
    }
}
