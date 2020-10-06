/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.alicia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import javax.el.MethodNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.dspace.content.Item;
import org.dspace.importer.external.alicia.callable.CountByQueryCallable;
import org.dspace.importer.external.alicia.callable.FindMatchingRecordsCallable;
import org.dspace.importer.external.alicia.callable.GetByAliciaIdCallable;
import org.dspace.importer.external.alicia.callable.SearchByQueryCallable;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;

public class AliciaImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements QuerySource {

//    private final String aliciaSearchUrl = "https://alicia.concytec.gob.pe/vufind/api/v1/search";
//  private final String aliciaSearchUrl = "https://alicia.concytec.gob.pe/vufind/api/v1/search";

    private String fields;

    private final WebTarget searchWebTarget;
    private final WebTarget getWebTarget;

    public AliciaImportMetadataSourceServiceImpl(String aliciaSearchAddress, String aliciaGetAddress, String fields) {
        Client client = ClientBuilder.newClient();
        searchWebTarget = client.target(aliciaSearchAddress);
        getWebTarget = client.target(aliciaGetAddress);
        this.fields = fields;
    }

    @Override
    public String getImportSource() {
        return "Alicia";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        String records = retry(new GetByAliciaIdCallable(id, getWebTarget, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query, searchWebTarget));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query, searchWebTarget));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, count, start, searchWebTarget, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, searchWebTarget, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, searchWebTarget, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        String records = retry(new FindMatchingRecordsCallable(query, getWebTarget));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        // FIXME: we need this method?
        throw new MethodNotFoundException("This method is not implemented for Alicia");
    }

    @Override
    public void init() throws Exception {

    }

    private List<ImportRecord> extractMetadataFromRecordList(String records) {
        List<ImportRecord> recordsResult = new ArrayList<>();
        ReadContext ctx = JsonPath.parse(records);
        try {
            Object o = ctx.read("$.records[*]");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray)o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    recordsResult.add(transformSourceRecords(innerJson));
                }
            } else {
                recordsResult.add(transformSourceRecords(o.toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error reading data from alicia");
        }
        return recordsResult;
    }

}