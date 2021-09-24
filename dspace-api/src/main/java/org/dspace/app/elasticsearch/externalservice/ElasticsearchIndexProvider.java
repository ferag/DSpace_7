/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
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

    public int checkIndex(String index) {
        HttpResponse response = null;
        try {
            response = elasticsearchConnector.findIndex(index);
        } catch (IOException e) {
            log.error("Can not find index, caused by: " + e.getMessage());
            return 0;
        }
        return response.getStatusLine().getStatusCode();
    }

    public boolean indexSingleItem(String index, Item item, String json) {
        HttpResponse response = null;
        try {
            response = elasticsearchConnector.create(json, index, StringUtils.EMPTY);
        } catch (IOException e) {
            log.error("Cannot index item with uuid: " + item.getID()  + " , caused by: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_CREATED) {
            log.error("Cannot index item with uuid: " + item.getID()  + " , response status: " + status);
            // TODO: remove this after live test
            try {
                log.error("response: {}", EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                log.error("Unable to log response");
            }
            return false;
        }
        return true;
    }

    public boolean deleteIndex(String index) {
        HttpResponse response = null;
        try {
            response = elasticsearchConnector.deleteIndex(index);
        } catch (IOException e) {
            log.error("Can not delete Index : " + index + " , caused by: " + e.getMessage());
            return false;
        }
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            log.error("Can not delete Index : " + index + " , with response status: " + status);
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
