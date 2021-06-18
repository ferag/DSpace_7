/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service.impl;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.dao.ElasticsearchIndexQueueDAO;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueServiceImpl implements ElasticsearchIndexQueueService {

    @Autowired(required = true)
    private AuthorizeService authorizeService;

    @Autowired(required = true)
    private ElasticsearchIndexQueueDAO elasticsearchIndexQueueDAO;

    @Override
    public ElasticsearchIndexQueue find(Context context, UUID uuid) throws SQLException {
        return elasticsearchIndexQueueDAO.findByID(context, ElasticsearchIndexQueue.class, uuid);
    }

    @Override
    public ElasticsearchIndexQueue create(Context context, UUID itemUuid, Integer operationType) throws SQLException {
        ElasticsearchIndexQueue elasticsearchIndexQueue = new ElasticsearchIndexQueue();
        elasticsearchIndexQueue.setId(itemUuid);
        elasticsearchIndexQueue.setOperationType(operationType);
        elasticsearchIndexQueue.setInsertionDate(new Date());
        return elasticsearchIndexQueueDAO.create(context, elasticsearchIndexQueue);
    }

    @Override
    public void delete(Context context, ElasticsearchIndexQueue elasticsearchIndexQueue)
            throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a ElasticsearchIndexQueue");
        }
        elasticsearchIndexQueueDAO.delete(context, elasticsearchIndexQueue);
    }

    @Override
    public void update(Context context, ElasticsearchIndexQueue elasticsearchIndexQueue)
            throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to update a ElasticsearchIndexQueue");
        }
        if (Objects.nonNull(elasticsearchIndexQueue)) {
            elasticsearchIndexQueueDAO.save(context, elasticsearchIndexQueue);
        }
    }

    @Override
    public ElasticsearchIndexQueue getFirstRecord(Context context) throws SQLException {
        return elasticsearchIndexQueueDAO.getFirstRecord(context);
    }

    @Override
    public void deleteAll(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a ElasticsearchIndexQueue");
        }
        elasticsearchIndexQueueDAO.deleteAll(context);
    }

}