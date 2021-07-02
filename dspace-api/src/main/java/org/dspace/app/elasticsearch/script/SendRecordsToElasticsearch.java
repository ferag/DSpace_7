/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.script;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchProvider;
import org.dspace.app.elasticsearch.factory.ElasticsearchIndexQueueServiceFactory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.app.elasticsearch.service.ElasticsearchItemBuilder;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.event.Event;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to process ElasticsearchIndex queue
 * and send documents in JSON format to Elasticsearch.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SendRecordsToElasticsearch
        extends DSpaceRunnable<SendRecordsToElasticsearchScriptConfiguration<SendRecordsToElasticsearch>> {

    private static final Logger log = LogManager.getLogger(SendRecordsToElasticsearch.class);

    private ElasticsearchIndexQueueService elasticsearchQueueService;

    private ElasticsearchItemBuilder elasticsearchItemBuilder;

    private ElasticsearchProvider elasticsearchProvider;

    private Context context;

    private Integer limit;

    @Override
    public void setup() throws ParseException {
        this.elasticsearchQueueService = ElasticsearchIndexQueueServiceFactory.getInstance()
                                                        .getElasticsearchIndexQueueService();
        this.elasticsearchItemBuilder = new DSpace().getServiceManager().getServiceByName(
                                ElasticsearchItemBuilder.class.getName(), ElasticsearchItemBuilder.class);
        this.elasticsearchProvider = new DSpace().getServiceManager().getServiceByName(
                                            ElasticsearchProvider.class.getName(), ElasticsearchProvider.class);
        String strLimit = commandLine.getOptionValue('l');
        this.limit = StringUtils.isNotBlank(strLimit) ?  Integer.valueOf(strLimit) : 0;
    }

    @Override
    public void internalRun() throws Exception {
        boolean existRecord = true;
        boolean checkLimit = false;
        handler.logInfo("Start to send records to Elasticsearch!");
        int countProcessedItems = 0;
        context = new Context();
        assignCurrentUserInContext();
        try {
            context.turnOffAuthorisationSystem();
            List<String> docs = Collections.emptyList();
            while (existRecord && !checkLimit) {
                ElasticsearchIndexQueue record = elasticsearchQueueService.getFirstRecord(context);
                if (Objects.nonNull(record)) {
                    if (record.getOperationType() == Event.DELETE) {
                        elasticsearchProvider.processRecord(context, record, StringUtils.EMPTY);
                    } else {
                        docs = elasticsearchItemBuilder.convert(context, record);
                        if (docs.isEmpty()) {
                            handler.logError("It was not possible to convert the queueRecord with uuid: "
                                             + record.getID() + " and OperationType: " + record.getOperationType());
                        } else {
                            for (String json : docs) {
                                elasticsearchProvider.processRecord(context, record, json);
                            }
                        }
                    }
                    if (recorHasNotBeenModified(record)) {
                        elasticsearchQueueService.delete(context, record);
                    }
                    countProcessedItems ++;
                    if (this.limit == countProcessedItems) {
                        checkLimit = true;
                    }
                } else {
                    existRecord = false;
                }
            }
            context.complete();
            handler.logInfo(countProcessedItems + " ElasticsearchIndexQueue have been processed!");
            handler.logInfo("Process end");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private boolean recorHasNotBeenModified(ElasticsearchIndexQueue record) throws SQLException {
        ElasticsearchIndexQueue recordToCheck = elasticsearchQueueService.find(context, record.getId());
        return record.getInsertionDate().equals(recordToCheck.getInsertionDate());
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public SendRecordsToElasticsearchScriptConfiguration<SendRecordsToElasticsearch> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-elasticsearch",
                SendRecordsToElasticsearchScriptConfiguration.class);
    }

}