/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.dao.impl;
import java.sql.SQLException;
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
    public ElasticsearchIndexQueue getFirstRecord(Context context) throws SQLException {
        Query query = createQuery(context,
                      "FROM " + ElasticsearchIndexQueue.class.getSimpleName() + " ORDER BY insertionDate ASC");
        query.setMaxResults(1);
        return singleResult(query);
    }

}