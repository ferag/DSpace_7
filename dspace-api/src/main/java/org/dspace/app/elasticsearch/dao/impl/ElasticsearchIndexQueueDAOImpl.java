/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.dao.impl;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.dao.ElasticsearchIndexQueueDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link ElasticsearchIndexQueueDAO}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueDAOImpl
        extends AbstractHibernateDAO<ElasticsearchIndexQueue>
         implements ElasticsearchIndexQueueDAO {

    @Override
    @SuppressWarnings("unchecked")
    public List<ElasticsearchIndexQueue> findByItemId(Context context, UUID itemId, Integer limit, Integer offset)
            throws SQLException {
        Query query = createQuery(context, "FROM ElasticsearchIndexQueue WHERE item.id= : itemId");
        query.setParameter("itemId", itemId);
        if (limit != null && limit.intValue() > 0) {
            query.setMaxResults(limit);
        }
        query.setFirstResult(offset);
        return query.getResultList();
    }

}