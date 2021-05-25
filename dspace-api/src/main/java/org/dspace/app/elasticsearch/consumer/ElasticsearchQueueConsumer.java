/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.consumer;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.app.elasticsearch.factory.ElasticsearchIndexQueueServiceFactory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchQueueConsumer implements Consumer {

    private ItemService itemService;

    private ElasticsearchIndexQueueService elasticsearchIndexQueueService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.elasticsearchIndexQueueService = ElasticsearchIndexQueueServiceFactory.getInstance()
                                                             .getElasticsearchIndexQueueService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        int i = event.getEventType();
        if (event.getEventType() == Event.CREATE  || event.getEventType() == Event.MODIFY ||
            event.getEventType() == Event.DELETE) {
            Object dso = event.getSubject(context);
            if ((dso instanceof Item)) {
                Item item = (Item) dso;
                if (itemsAlreadyProcessed.contains(item)) {
                    return;
                }
                if (isSupportedEntityType(item)) {
                    elasticsearchIndexQueueService.create(context, item, event.getEventType());
                }
                itemsAlreadyProcessed.add(item);
            }
        }
    }


    private boolean isSupportedEntityType(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        return (StringUtils.equals(entityType, "Person") || StringUtils.equals(entityType, "Publication"));
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

}