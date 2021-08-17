/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;
import java.io.IOException;

import org.apache.http.HttpResponse;

/**
 * This class deals with logic management to connect to the Elasticsearch external service
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchConnector {

    /**
     * Add new document into the index provided
     * 
     * @param json            Elasticsearch document in json format
     * @param index           Elasticsearch index
     * @param docId           Id we want to associate to the document, if it is not provided Elasticsearch generates it
     * @return                HttpResponse
     * @throws IOException    if IO error
     */
    public HttpResponse create(String json, String index, String docId) throws IOException;

    /**
     * Delete the document by ID into the index provided
     * 
     * @param index           Elasticsearch index
     * @param docId           Id of the Elasticsearch document we want to delete
     * @return                HttpResponse
     * @throws IOException    if IO error
     */
    public HttpResponse delete(String index, String docId) throws IOException;

    /**
     * Delete index in Elasticsearch
     * 
     * @param index           Elasticsearch index
     * @return                HttpResponse
     * @throws IOException    if IO error
     */
    public HttpResponse deleteIndex(String index) throws IOException;

    /**
     * 
     * @param index           Index of the Elasticsearch we want to delete
     * @return                HttpResponse
     * @throws IOException    if IO error
     */
    public HttpResponse findIndex(String index) throws IOException;

    /**
     * Find document for a specific field and value
     * 
     * @param index           Elasticsearch index
     * @param field           document field
     * @param value           value
     * @return                HttpResponse
     * @throws IOException    if IO error
     */
    public HttpResponse searchByFieldAndValue(String index, String field, String value) throws IOException;

}