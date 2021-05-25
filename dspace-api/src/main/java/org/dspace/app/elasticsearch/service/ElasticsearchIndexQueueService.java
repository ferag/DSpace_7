/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
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
    public ElasticsearchIndexQueue find(Context context, int id) throws SQLException;

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
     * Get the ElasticsearchIndex queue records by the item id.
     *
     * @param  context        DSpace context object
     * @param  itemId         The owner item id
     * @return                The elasticsearchIndex queue records
     * @throws SQLException   If an SQL error occurs
     */
    public List<ElasticsearchIndexQueue> findByItemId(Context context, UUID itemId) throws SQLException;

    /**
     * Create an ElasticsearchIndexQueue
     * 
     * @param context              DSpace context object
     * @param item
     * @param operationType
     * @return
     * @throws SQLException        If an SQL error occurs
     */
    public ElasticsearchIndexQueue create(Context context, Item item, Integer operationType) throws SQLException;

}