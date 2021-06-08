/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;

import java.io.IOException;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchProvider {

    @Autowired
    private ElasticsearchConnector elasticsearchConnector;

    public void processRecord(Context context, ElasticsearchIndexQueue record, String json) throws IOException {
        elasticsearchConnector.create(json);
    }

}