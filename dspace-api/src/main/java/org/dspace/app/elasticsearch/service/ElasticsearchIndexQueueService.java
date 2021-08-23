/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * Interface of service to manage ElasticsearchIndexQueue
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchIndexQueueService  {

    /**
     * Get an ElasticsearchIndexQueue from the database.
     *
     * @param context  DSpace context object
     * @param id       ID of the ElasticsearchIndexQueue
     * @return         the ElasticsearchIndexQueue format, or null if the ID is invalid.
     * @throws         SQLException if database error
     */
    public ElasticsearchIndexQueue find(Context context, UUID uuid) throws SQLException;

    /**
     * Delete an ElasticsearchIndexQueue
     *
     * @param context                    context
     * @param ElasticsearchIndexQueue    elasticsearchIndexQueue
     * @throws SQLException              if database error
     * @throws AuthorizeException        if authorization error
     */
    public void delete(Context context, ElasticsearchIndexQueue elasticsearchIndexQueue)
           throws SQLException, AuthorizeException;

    /**
     * Create an ElasticsearchIndexQueue
     * 
     * @param context              DSpace context object
     * @param item
     * @param operationType
     * @return
     * @throws SQLException        If an SQL error occurs
     */
    public ElasticsearchIndexQueue create(Context context, UUID itemUuid, Integer operationType) throws SQLException;

    /**
     * Update the ElasticsearchIndexQueue
     *
     * @param context                      context
     * @param ElasticsearchIndexQueue      elasticsearchIndexQueue
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    public void update(Context context, ElasticsearchIndexQueue elasticsearchIndexQueue)
           throws SQLException, AuthorizeException;

    /**
     * Get the first ElasticsearchIndexQueue from the database, ordered by date ascendant.
     *
     * @param context  DSpace context object
     * @return         the ElasticsearchIndexQueue format, or null the database is empty.
     */
    public ElasticsearchIndexQueue getFirstRecord(Context context) throws SQLException;

    /**
     * WARNING: This method deletes all records in the table : {ElasticsearchIndexQueue}
     *          and it will not be possible to go back.
     * 
     * @param context                 DSpace context object
     * @throws SQLException           if database error
     * @throws AuthorizeException     if authorization error
     */
    public void deleteAll(Context context) throws SQLException, AuthorizeException;

}