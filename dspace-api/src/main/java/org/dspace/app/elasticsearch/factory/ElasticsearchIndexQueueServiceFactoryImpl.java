/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.factory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ElasticsearchIndexQueueServiceFactory}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueServiceFactoryImpl extends ElasticsearchIndexQueueServiceFactory {

    @Autowired
    private ElasticsearchIndexQueueService elasticsearchIndexQueueService;

    @Override
    public ElasticsearchIndexQueueService getElasticsearchIndexQueueService() {
        return elasticsearchIndexQueueService;
    }

}