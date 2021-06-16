/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.http.HttpStatus;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchIndexProvider;
import org.dspace.app.elasticsearch.script.bulkindex.ElasticsearchBulkIndex;
import org.dspace.app.elasticsearch.service.ElasticsearchItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite to verify the Elasticsearch bulk indexing.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchBulkIndexIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ElasticsearchItemBuilder elasticsearchItemBuilder;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        configurationService.addPropertyValue("elasticsearch.entity", "Person");
        configurationService.addPropertyValue("elasticsearch.entity", "Publication");
    }

    @Test
    public void elasticsearchBulkIndexingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        String testIndex = "testIndex";

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Publication item One")
                                .withIssueDate("2019-06-20")
                                .withAuthor("Smith, Maria")
                                .withEntityType("Publication").build();

        Item item2 = ItemBuilder.createItem(context, col1)
                                .withTitle("Publication item Two")
                                .withIssueDate("2018-02-12")
                                .withAuthor("Bandola, Stas")
                                .withEntityType("Publication").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Patent item Title")
                   .withIssueDate("2020-01-21")
                   .withAuthor("Bohach, Ivan")
                   .withEntityType("Patent").build();

        String jsonItem1 = elasticsearchItemBuilder.convert(context, item1);
        String jsonItem2 = elasticsearchItemBuilder.convert(context, item2);

        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item1, jsonItem1)).thenReturn(true);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item2, jsonItem2)).thenReturn(true);

        context.restoreAuthSystemState();

        String[] args = new String[] { "elasticsearch-bulk-indexing", "-e", "Publication", "-i", testIndex};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        bulkIndex.initialize(args, handler, admin);
        bulkIndex.setElasticsearchIndexProvider(elasticsearchIndexProvider);
        bulkIndex.run();
        assertNull(handler.getException());

        java.util.Collection<Invocation> invocations =
                  Mockito.mockingDetails(elasticsearchIndexProvider).getInvocations();
        Iterator<Invocation> invocationIterator = invocations.iterator();
        assertEquals(3, invocations.size());
        Invocation i = invocationIterator.next();

        assertTrue(i.toString().startsWith("elasticsearchIndexProvider.checkIngex"));
        assertEquals(1, i.getArguments().length);
        assertEquals(i.getArgument(0), testIndex);


        Invocation i2 = invocationIterator.next();
        assertTrue(i2.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
        assertEquals(3, i2.getArguments().length);
        assertEquals(i2.getArgument(0), testIndex);
        assertEquals(i2.getArgument(1), item1);
        assertEquals(i2.getArgument(2), jsonItem1);

        Invocation i3 = invocationIterator.next();
        assertTrue(i3.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
        assertEquals(3, i3.getArguments().length);
        assertEquals(i3.getArgument(0), testIndex);
        assertEquals(i3.getArgument(1), item2);
        assertEquals(i3.getArgument(2), jsonItem2);

    }

}