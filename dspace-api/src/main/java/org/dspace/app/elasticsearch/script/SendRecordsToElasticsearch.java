/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.script;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchProvider;
import org.dspace.app.elasticsearch.factory.ElasticsearchIndexQueueServiceFactory;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexConverter;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SendRecordsToElasticsearch
        extends DSpaceRunnable<SendRecordsToElasticsearchScriptConfiguration<SendRecordsToElasticsearch>> {

    private static final Logger log = LogManager.getLogger(SendRecordsToElasticsearch.class);

    private ElasticsearchIndexQueueService elasticsearchQueueService;

    private ElasticsearchIndexConverter elasticsearchIndexConverter;

    private ElasticsearchProvider elasticsearchProvider;

    private Context context;

    @Override
    public void setup() throws ParseException {
        this.elasticsearchQueueService = ElasticsearchIndexQueueServiceFactory.getInstance()
                                                        .getElasticsearchIndexQueueService();
        this.elasticsearchIndexConverter = new DSpace().getServiceManager().getServiceByName(
                                ElasticsearchIndexConverter.class.getName(), ElasticsearchIndexConverter.class);
        this.elasticsearchProvider = new DSpace().getServiceManager().getServiceByName(
                                            ElasticsearchProvider.class.getName(), ElasticsearchProvider.class);
    }

    @Override
    public void internalRun() throws Exception {
        boolean existRecord = true;
        context = new Context();
        assignCurrentUserInContext();
        try {
            context.turnOffAuthorisationSystem();
            while (existRecord) {
                ElasticsearchIndexQueue record = elasticsearchQueueService.getFirstRecord(context);
                if (Objects.nonNull(record)) {
                    String json = elasticsearchIndexConverter.convert(context, record);
                    System.out.println(json);
                    elasticsearchProvider.processRecord(context, record, json);
                    if (recorHasNotBeenModified(record)) {
                        elasticsearchQueueService.delete(context, record);
                    }
                } else {
                    existRecord = false;
                }

            }
            context.complete();
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