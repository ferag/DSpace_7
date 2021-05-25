/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.dao;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ElasticsearchIndexQueue object.
 * The implementation of this class is responsible for all database calls for the
 * ElasticsearchIndexQueue object and is autowired by spring. This class should only be
 * accessed from a single service and should never be exposed outside of the API
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchIndexQueueDAO extends GenericDAO<ElasticsearchIndexQueue> {

    /**
     * Get the ElasticsearchIndex queue records by the item id.
     *
     * @param context           DSpace context object
     * @param itemId            The item uuid
     * @param limit             Limit
     * @param offset            Offset
     * @return                  The ElasticsearchIndex queue records
     * @throws SQLException     If an SQL error occurs
     */
    public List<ElasticsearchIndexQueue> findByItemId(Context context, UUID itemId, Integer limit, Integer offset)
        throws SQLException;

}