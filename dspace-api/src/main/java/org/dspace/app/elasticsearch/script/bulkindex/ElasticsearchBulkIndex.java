/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.script.bulkindex;
import java.sql.SQLException;
import java.time.Year;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchIndexProvider;
import org.dspace.app.elasticsearch.service.ElasticsearchItemBuilder;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to Indexing Items in Elasticsearch.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchBulkIndex
        extends DSpaceRunnable<ElasticsearchBulkIndexScriptConfiguration<ElasticsearchBulkIndex>> {

    private static final Logger log = LogManager.getLogger(ElasticsearchBulkIndex.class);

    private ConfigurationService configurationService;

    private ElasticsearchIndexProvider elasticsearchIndexProvider;

    private ElasticsearchItemBuilder elasticsearchItemBuilder;

    private ElasticsearchIndexManager elasticsearchIndexManager;

    private Context context;

    private String entityType;

    private String index;

    private int maxAttempt;

    @Override
    public void setup() throws ParseException {
        this.elasticsearchIndexProvider = new DSpace().getServiceManager().getServiceByName(
                    ElasticsearchIndexProvider.class.getName(), ElasticsearchIndexProvider.class);
        this.elasticsearchItemBuilder = new DSpace().getServiceManager().getServiceByName(
                        ElasticsearchItemBuilder.class.getName(), ElasticsearchItemBuilder.class);
        this.elasticsearchIndexManager = new DSpace().getServiceManager().getServiceByName(
                      ElasticsearchIndexManager.class.getName(), ElasticsearchIndexManager.class);
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.entityType = commandLine.getOptionValue('e');
        this.index = commandLine.getOptionValue('i');
        this.maxAttempt = configurationService.getIntProperty("elasticsearchbulk.maxattempt", 3);
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();

        if (StringUtils.isBlank(entityType)) {
            throw new IllegalArgumentException("The EntityType must be provided");
        }
        if (!elasticsearchIndexManager.getEntityType2Index().containsKey(entityType)) {
            throw new IllegalArgumentException("Provided entity type does not supported!");
        }
        this.index = StringUtils.isNotBlank(this.index)
                     ? this.index : this.entityType.toLowerCase() + "-" + getCurrentYear();
        try {
            context.turnOffAuthorisationSystem();
            performBulkIndexing();
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performBulkIndexing() {
        deleteIndex();
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems();
            handler.logInfo("Update start");
            int countFoundItems = 0;
            int countUpdatedItems = 0;
            while (itemIterator.hasNext()) {
                int countAttempt = 0;
                Item item = context.reloadEntity(itemIterator.next());
                countFoundItems++;
                List<String> docs = elasticsearchItemBuilder.convert(context, item);
                for (String json : docs) {
                    boolean updated = false;
                    do {
                        countAttempt++;
                        updated = elasticsearchIndexProvider.indexSingleItem(this.index, item, json);
                        if (!updated && countAttempt == this.maxAttempt) {
                            handler.logInfo("It was not possible to indexing the item with uuid: " + item.getID());
                        }
                    } while (!updated && countAttempt < this.maxAttempt);
                    context.uncacheEntity(item);
                    if (updated) {
                        countUpdatedItems++;
                    }
                }
                count++;
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            handler.logInfo("Found " + countFoundItems + " items");
            handler.logInfo("Updated " + countUpdatedItems + " items");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void deleteIndex() {
        int status = elasticsearchIndexProvider.checkIndex(index);
        if (status == HttpStatus.SC_NOT_FOUND) {
            return;
        }
        if (status == HttpStatus.SC_OK) {
            if (!elasticsearchIndexProvider.deleteIndex(index)) {
                throw new RuntimeException("Can not delete Index with name: " + this.index);
            }
        }
    }

    private Iterator<Item> findItems() throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries("dspace.entity.type:" + this.entityType);
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private String getCurrentYear() {
        return String.valueOf(Year.now().getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ElasticsearchBulkIndexScriptConfiguration<ElasticsearchBulkIndex> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("elasticsearch-bulk-indexing",
                ElasticsearchBulkIndexScriptConfiguration.class);
    }

    public ElasticsearchIndexProvider getElasticsearchIndexProvider() {
        return elasticsearchIndexProvider;
    }

    public void setElasticsearchIndexProvider(ElasticsearchIndexProvider elasticsearchIndexProvider) {
        this.elasticsearchIndexProvider = elasticsearchIndexProvider;
    }

}