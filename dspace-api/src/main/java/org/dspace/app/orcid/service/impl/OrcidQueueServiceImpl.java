/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidQueueService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueServiceImpl implements OrcidQueueService {

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Autowired
    private ItemService itemService;

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId) throws SQLException {
        return orcidQueueDAO.findByOwnerId(context, ownerId, -1, 0);
    }

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        return orcidQueueDAO.findByOwnerId(context, ownerId, limit, offset);
    }

    @Override
    public List<OrcidQueue> findByOwnerAndEntityId(Context context, UUID ownerId, UUID entityId) throws SQLException {
        return orcidQueueDAO.findByOwnerAndEntityId(context, ownerId, entityId);
    }

    @Override
    public List<OrcidQueue> findByOwnerOrEntity(Context context, Item item) throws SQLException {
        return orcidQueueDAO.findByOwnerOrEntity(context, item);
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        return orcidQueueDAO.countByOwnerId(context, ownerId);
    }

    @Override
    public List<OrcidQueue> findAll(Context context) throws SQLException {
        return orcidQueueDAO.findAll(context, OrcidQueue.class);
    }

    @Override
    public OrcidQueue create(Context context, Item owner, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setEntityType(itemService.getEntityType(entity));
        orcidQueue.setOwner(owner);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue create(Context context, Item owner, Item entity, String putCode) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setOwner(owner);
        orcidQueue.setEntity(entity);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setEntityType(itemService.getEntityType(entity));
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue create(Context context, Item owner, String entityType, String putCode) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntityType(entityType);
        orcidQueue.setOwner(owner);
        orcidQueue.setPutCode(putCode);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = orcidQueueDAO.findByID(context, OrcidQueue.class, id);
        if (orcidQueue != null) {
            delete(context, orcidQueue);
        }
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.delete(context, orcidQueue);
    }

    @Override
    public OrcidQueue find(Context context, int id) throws SQLException {
        return orcidQueueDAO.findByID(context, OrcidQueue.class, id);
    }

    @Override
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.save(context, orcidQueue);
    }
}
