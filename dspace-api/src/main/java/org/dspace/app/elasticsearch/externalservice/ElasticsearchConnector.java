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

    public HttpResponse create(String json, String index, String docId) throws IOException;

    public HttpResponse update(String json, String index, String docId) throws IOException;

    public HttpResponse delete(String index, String docId) throws IOException;

    public HttpResponse searchByIndexAndDoc(String index, String docId) throws IOException;

    public HttpResponse deleteIndex(String index) throws IOException;

    public HttpResponse findIndex(String index) throws IOException;

    public HttpResponse searchByFieldAndValue(String index, String field, String value) throws IOException;

}