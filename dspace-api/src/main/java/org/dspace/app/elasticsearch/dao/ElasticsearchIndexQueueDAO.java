/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.dao;
import java.sql.SQLException;

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

    public ElasticsearchIndexQueue getFirstRecord(Context context) throws SQLException;

    /**
     * WARNING: This method deletes all records in the table : {ElasticsearchIndexQueue}
     *          and it will not be possible to go back.
     * 
     * @param context
     * @throws SQLException
     */
    public void deleteAll(Context context) throws SQLException;

}