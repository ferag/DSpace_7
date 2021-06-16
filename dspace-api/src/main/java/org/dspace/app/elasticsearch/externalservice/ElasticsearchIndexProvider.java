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
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexProvider {

    private static final Logger log = LogManager.getLogger(ElasticsearchIndexProvider.class);

    @Autowired
    private ElasticsearchConnector elasticsearchConnector;

    public int checkIngex(String index) {
        HttpResponse responce = null;
        try {
            responce = elasticsearchConnector.findIndex(index);
        } catch (IOException e) {
            log.error("Can not find index, caused by: " + e.getMessage());
            return 0;
        }
        return responce.getStatusLine().getStatusCode();
    }

    public boolean indexSingleItem(String index, Item item, String json) {
        HttpResponse responce = null;
        try {
            responce = elasticsearchConnector.create(json, index, item.getID());
        } catch (IOException e) {
            log.error("Can not indexing item with uuid: " + item.getID()  + " , caused by: " + e.getMessage());
            return false;
        }
        int status = responce.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_CREATED) {
            log.error("Can not indexing item with uuid: " + item.getID()  + " , with responce status: " + status);
            return false;
        }
        return true;
    }

    public boolean deleteIndex(String index) {
        HttpResponse responce = null;
        try {
            responce = elasticsearchConnector.deleteIndex(index);
        } catch (IOException e) {
            log.error("Can not delete Index : " + index + " , caused by: " + e.getMessage());
            return false;
        }
        int status = responce.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            log.error("Can not delete Index : " + index + " , with responce status: " + status);
            return false;
        }
        return true;
    }

    public ElasticsearchConnector getElasticsearchConnector() {
        return elasticsearchConnector;
    }

    public void setElasticsearchConnector(ElasticsearchConnector elasticsearchConnector) {
        this.elasticsearchConnector = elasticsearchConnector;
    }

}