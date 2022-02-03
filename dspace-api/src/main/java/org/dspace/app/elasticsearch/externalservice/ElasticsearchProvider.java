/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
import org.dspace.app.elasticsearch.exception.ElasticsearchException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The scope of this class is to manage sending of requests to Elasticsearch.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchProvider {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ElasticsearchConnector elasticsearchConnector;

    @Autowired
    private ElasticsearchIndexManager elasticsearchIndexManager;

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchProvider.class);

    /**
     * Processes record according to the operation type
     * 
     * @param context        DSpace context object
     * @param record         ElasticsearchIndexQueue object
     * @param jsons           Json representations of item related to the record
     * @throws IOException   if IO error
     * @throws SQLException  If there's a database problem
     */
    public void processRecord(Context context, ElasticsearchIndexQueue record, List<String> jsons)
            throws IOException, SQLException {
        String index = getIndex(context, record);
        if (StringUtils.isBlank(index)) {
            logger.warn("Unable to find an ElasticSearch index for item {}, item not indexed", record.getID());
        }
        switch (record.getOperationType()) {
            case Event.CREATE : addDocument(record, jsons, index);
                break;
            case Event.DELETE : deleteDocument(record, index);
                break;
            case Event.MODIFY : updateDocument(record, jsons, index);
                break;
            case Event.MODIFY_METADATA : updateDocument(record, jsons, index);
                break;
            default:
                throw new RuntimeException("The operation type : " + record.getOperationType() +
                                     " for ElasticsearchProvider with uuid: " + record.getId() + " is not supported!");
        }
    }

    private void addDocument(ElasticsearchIndexQueue record, List<String> jsons, String index) throws IOException {
        for (String json : jsons) {
            HttpResponse response = elasticsearchConnector.create(json, index, StringUtils.EMPTY);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_CREATED) {
                logger.warn("It was not possible to CREATE document with uuid: " + record.getId()
                        + "  Elasticsearch returned status code : " + status);
                logger.warn("document: " + json);
            }
        }
    }

    private void updateDocument(ElasticsearchIndexQueue record, List<String> jsons, String index) throws IOException {
        deleteDocument(record, index);
        addDocument(record, jsons, index);
    }

    private void deleteDocument(ElasticsearchIndexQueue record, String index) throws IOException {
        List<String> docIDs = getDocIdByField(index, record.getId().toString());
        for (String docID : docIDs) {
            HttpResponse response = elasticsearchConnector.delete(index, docID);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new ElasticsearchException("It was not possible to DELETE document with record uuid: "
                        + record.getId() + " and document id: " + docID
                        + "  Elasticsearch returned status code : " + status);
            }
        }
    }

    private String getIndex(Context context, ElasticsearchIndexQueue record) throws SQLException, IOException {
        if (record.getOperationType() == Event.DELETE) {
            for (String index : elasticsearchIndexManager.getEntityType2Index().values()) {
                List<String> docIDs = getDocIdByField(index, record.getId().toString());
                if (!docIDs.isEmpty()) {
                    return index;
                }
            }
        }
        Item item = itemService.find(context, record.getId());
        if (Objects.nonNull(item)) {
            String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
            return elasticsearchIndexManager.getEntityType2Index().containsKey(entityType)
                         ? elasticsearchIndexManager.getEntityType2Index().get(entityType) : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    private List<String> getDocIdByField(String index, String id) throws IOException {
        ElasticSearchResponse response = elasticsearchConnector.searchByFieldAndValue(index, "resourceId", id);
        int status = response.getStatusCode();
        if (status != HttpStatus.SC_OK || response.getBody().isEmpty()) {
            throw new ElasticsearchException("It was not possible to retrieve document by field 'resourceId'"
                    + " and value: " + id + "  Elasticsearch returned status code : " + status);
        }
        return getDocumentIdFromResponse(response.getBody().get());
    }

    private List<String> getDocumentIdFromResponse(JSONObject json) throws IOException {
        List<String> ids = new LinkedList<String>();
        if (json.has("hits")) {
            json = new JSONObject(json.get("hits").toString());
            if (json.has("hits")) {
                JSONArray array = json.getJSONArray("hits");
                for (Object obj : array) {
                    json = new JSONObject(obj.toString());
                    if (json.has("_id")) {
                        ids.add(json.get("_id").toString());
                    }
                }
            }
        }
        return ids;
    }

    public ElasticsearchConnector getElasticsearchConnector() {
        return elasticsearchConnector;
    }

    public void setElasticsearchConnector(ElasticsearchConnector elasticsearchConnector) {
        this.elasticsearchConnector = elasticsearchConnector;
    }

}
