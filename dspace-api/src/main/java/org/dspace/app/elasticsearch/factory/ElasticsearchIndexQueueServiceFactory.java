/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.factory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public abstract class ElasticsearchIndexQueueServiceFactory {

    public abstract ElasticsearchIndexQueueService getElasticsearchIndexQueueService();

    public static ElasticsearchIndexQueueServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
               "elasticsearchIndexQueueServiceFactory", ElasticsearchIndexQueueServiceFactory.class);
    }

}