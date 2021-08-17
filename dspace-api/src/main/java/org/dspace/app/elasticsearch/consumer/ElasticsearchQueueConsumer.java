/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.consumer;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.factory.ElasticsearchIndexQueueServiceFactory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 * Consumer responsible for inserting events performed on items in the ElasticsearchIndexQueue table.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchQueueConsumer implements Consumer {

    private ElasticsearchIndexQueueService elasticsearchIndexQueueService;

    private ElasticsearchIndexManager elasticsearchIndexManager;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        this.elasticsearchIndexQueueService = ElasticsearchIndexQueueServiceFactory.getInstance()
                .getElasticsearchIndexQueueService();
        this.elasticsearchIndexManager = new DSpace().getServiceManager().getServiceByName(
                       ElasticsearchIndexManager.class.getName(), ElasticsearchIndexManager.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }
        int eventType = event.getEventType();
        if (eventType == Event.CREATE || eventType == Event.MODIFY) {
            Item item = (Item) event.getSubject(context);
            if (itemsAlreadyProcessed.contains(item) || !elasticsearchIndexManager.isSupportedEntityType(item)) {
                return;
            }
            elasticsearchIndexQueueService.create(context, item.getID(), event.getEventType());
            itemsAlreadyProcessed.add(item);
            return;
        }
        ElasticsearchIndexQueue elasticIndex = elasticsearchIndexQueueService.find(context, event.getSubjectID());
        if (eventType == Event.MODIFY_METADATA) {
            DSpaceObject obj =  event.getSubject(context);
            if (Objects.isNull(obj)) {
                return;
            }
            Item item = (Item) obj;
            if (itemsAlreadyProcessed.contains(item) || !elasticsearchIndexManager.isSupportedEntityType(item)) {
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
            return;
        }
        if (eventType == Event.DELETE) {
            if (Objects.nonNull(elasticIndex)) {
                elasticIndex.setOperationType(eventType);
                elasticIndex.setInsertionDate(new Date());
                elasticsearchIndexQueueService.update(context, elasticIndex);
            } else {
                elasticsearchIndexQueueService.create(context, event.getSubjectID(), eventType);
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

}