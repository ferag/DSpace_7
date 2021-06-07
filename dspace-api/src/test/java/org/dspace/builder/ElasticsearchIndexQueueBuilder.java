package org.dspace.builder;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * Builder to construct ElasticsearchIndexQueue objects
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueBuilder
        extends  AbstractBuilder<ElasticsearchIndexQueue, ElasticsearchIndexQueueService> {

    private static final Logger log = Logger.getLogger(ElasticsearchIndexQueueBuilder.class);

    private ElasticsearchIndexQueue elasticsearchIndexQueue;

    protected ElasticsearchIndexQueueBuilder(Context context) {
        super(context);
    }

    public static ElasticsearchIndexQueueBuilder createElasticsearchIndexQueue(Context context,
            UUID itemUuid, int operationType) throws SQLException, AuthorizeException {
        ElasticsearchIndexQueueBuilder builder = new ElasticsearchIndexQueueBuilder(context);
        return builder.create(context, itemUuid, operationType);
    }

    private ElasticsearchIndexQueueBuilder create(Context context, UUID itemUuid, int operationType)
            throws SQLException, AuthorizeException {
        try {
            this.context = context;
            this.elasticsearchIndexQueue = getService().create(context, itemUuid, operationType);
        } catch (Exception e) {
            log.error("Error in ElasticsearchIndexQueueBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public ElasticsearchIndexQueue build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, elasticsearchIndexQueue);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in ElasticsearchIndexQueueBuilder.build(), error: ", e);
        }
        return elasticsearchIndexQueue;
    }

    @Override
    public void delete(Context c, ElasticsearchIndexQueue elasticsearchIndexQueue) throws Exception {
        if (Objects.nonNull(elasticsearchIndexQueue)) {
            getService().delete(c, elasticsearchIndexQueue);
        }
    }

    public void delete(ElasticsearchIndexQueue elasticsearchIndexQueue) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ElasticsearchIndexQueue attachedTab = c.reloadEntity(elasticsearchIndexQueue);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }
        indexingService.commit();
    }

    @Override
    public void cleanup() throws Exception {
        delete(elasticsearchIndexQueue);
    }

    @Override
    protected ElasticsearchIndexQueueService getService() {
        return elasticsearchIndexQueueService;
    }

}