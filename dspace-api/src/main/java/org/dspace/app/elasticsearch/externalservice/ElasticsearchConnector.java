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
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchConnector {

    public HttpResponse create(String json, String index, ElasticsearchIndexQueue record) throws IOException;

    public HttpResponse update(String json, String index, ElasticsearchIndexQueue record) throws IOException;

    public HttpResponse delete(String index, ElasticsearchIndexQueue record) throws IOException;

    public HttpResponse searchByIndexAndDoc(String index, ElasticsearchIndexQueue record) throws IOException;

}