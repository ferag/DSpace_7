/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.consumer;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.factory.ElasticsearchIndexQueueServiceFactory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Consumer responsible for inserting events performed on items in the ElasticsearchIndexQueue table.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchQueueConsumer implements Consumer {

    private ItemService itemService;

    private ConfigurationService configurationService;

    private ElasticsearchIndexQueueService elasticsearchIndexQueueService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.elasticsearchIndexQueueService = ElasticsearchIndexQueueServiceFactory.getInstance()
                .getElasticsearchIndexQueueService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }
        int eventType = event.getEventType();
        if (eventType == Event.CREATE || eventType == Event.MODIFY) {
            Item item = (Item) event.getSubject(context);
            if (itemsAlreadyProcessed.contains(item) || !isSupportedEntityType(item)) {
                return;
            }
            elasticsearchIndexQueueService.create(context, item.getID(), event.getEventType());
            itemsAlreadyProcessed.add(item);
        }
        ElasticsearchIndexQueue elasticIndex = elasticsearchIndexQueueService.find(context, event.getSubjectID());
        if (eventType == Event.MODIFY_METADATA) {
            DSpaceObject obj =  event.getSubject(context);
            if (Objects.isNull(obj)) {
                return;
            }
            Item item = (Item) obj;
            if (itemsAlreadyProcessed.contains(item) || !isSupportedEntityType(item)) {
                return;
            }
            // if the item has been withdrawn, update record with DELETE type
            if (item.isWithdrawn() && Objects.nonNull(elasticIndex)) {
                elasticIndex.setOperationType(Event.DELETE);
                elasticIndex.setInsertionDate(new Date());
                elasticsearchIndexQueueService.update(context, elasticIndex);
            } else if (Objects.nonNull(elasticIndex)) {
                elasticIndex.setOperationType(eventType);
                elasticIndex.setInsertionDate(new Date());
                elasticsearchIndexQueueService.update(context, elasticIndex);
            }
            itemsAlreadyProcessed.add((Item) obj);
        }
        if (eventType == Event.DELETE) {
            if (Objects.nonNull(elasticIndex)) {
                elasticIndex.setOperationType(event.getEventType());
                elasticIndex.setInsertionDate(new Date());
                elasticsearchIndexQueueService.update(context, elasticIndex);
            }
        }
    }

    private boolean isSupportedEntityType(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        List<String> supportedEntities = Arrays.asList(
                                      configurationService.getArrayProperty("elasticsearch.entity"));
        return supportedEntities.contains(entityType);
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

}