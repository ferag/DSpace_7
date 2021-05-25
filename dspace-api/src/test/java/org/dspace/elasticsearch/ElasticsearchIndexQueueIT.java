/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.elasticsearch;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class will aim to test ElasticsearchIndexQueue related use cases
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueIT extends AbstractIntegrationTestWithDatabase {

    @Autowired
    private ElasticsearchIndexQueueService elasticsearchService;

    @Test
    public void test99() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item personItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Person item Title")
                                     .withIssueDate("2021-03-17")
                                     .withAuthor("Doe, John")
                                     .withEntityType("Person").build();

        Item publicationItem = ItemBuilder.createItem(context, col1)
                                          .withTitle("Publication item Title")
                                          .withIssueDate("2020-06-25")
                                          .withAuthor("Smith, Maria")
                                          .withEntityType("Publication").build();

        context.restoreAuthSystemState();

        List<ElasticsearchIndexQueue> records = elasticsearchService.findByItemId(context, personItem.getID());
        List<ElasticsearchIndexQueue> records2 = elasticsearchService.findByItemId(context, publicationItem.getID());
        assertEquals(1, records.size());
        assertEquals(1, records2.size());
    }

}