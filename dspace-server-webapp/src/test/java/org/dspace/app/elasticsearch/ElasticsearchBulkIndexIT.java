/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
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

    @Autowired
    private ElasticsearchIndexManager elasticsearchIndexManager;

    @Test
    public void elasticsearchBulkIndexingWithIndexNotExistingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        Map<String, String> originIndexes = cleanUpIndexes();
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

        // the index does not exist
        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item1, jsonItem1)).thenReturn(true);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item2, jsonItem2)).thenReturn(true);

        context.restoreAuthSystemState();
        try {
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
            assertTrue(i2.getArgument(1).equals(item1) || i2.getArgument(1).equals(item2));
            assertTrue(i2.getArgument(2).equals(jsonItem1) || i2.getArgument(2).equals(jsonItem2));

            Invocation i3 = invocationIterator.next();
            assertTrue(i3.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i3.getArguments().length);
            assertEquals(i3.getArgument(0), testIndex);
            assertTrue(i3.getArgument(1).equals(item1) || i3.getArgument(1).equals(item2));
            assertTrue(i3.getArgument(2).equals(jsonItem1) || i3.getArgument(2).equals(jsonItem2));

        } finally {
            restoreIndexes(originIndexes);
        }
    }

    @Test
    public void elasticsearchBulkIndexingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        Map<String, String> originIndexes = cleanUpIndexes();
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


        String jsonItem1 = elasticsearchItemBuilder.convert(context, item1);

        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_OK);
        when(elasticsearchIndexProvider.deleteIndex(testIndex)).thenReturn(true);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item1, jsonItem1)).thenReturn(true);

        context.restoreAuthSystemState();
        try {
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
            assertTrue(i2.toString().startsWith("elasticsearchIndexProvider.deleteIndex"));
            assertEquals(1, i2.getArguments().length);
            assertEquals(i2.getArgument(0), testIndex);

            Invocation i3 = invocationIterator.next();
            assertTrue(i3.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i3.getArguments().length);
            assertEquals(i3.getArgument(0), testIndex);
            assertEquals(i3.getArgument(1), item1);
            assertEquals(i3.getArgument(2), jsonItem1);
        } finally {
            restoreIndexes(originIndexes);
        }
    }

    @Test
    public void elasticsearchBulkIndexingwithoutIndexParameterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        Map<String, String> originIndexes = cleanUpIndexes();
        String testIndex = "publication-" + String.valueOf(Year.now().getValue());

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

        ItemBuilder.createItem(context, col1)
                   .withTitle("Patent item Title")
                   .withIssueDate("2020-01-21")
                   .withAuthor("Bohach, Ivan")
                   .withEntityType("Patent").build();

        String jsonItem1 = elasticsearchItemBuilder.convert(context, item1);

        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_OK);
        when(elasticsearchIndexProvider.deleteIndex(testIndex)).thenReturn(true);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item1, jsonItem1)).thenReturn(true);

        context.restoreAuthSystemState();
        try {
            String[] args = new String[] { "elasticsearch-bulk-indexing", "-e", "Publication"};
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
            assertTrue(i2.toString().startsWith("elasticsearchIndexProvider.deleteIndex"));
            assertEquals(1, i2.getArguments().length);
            assertEquals(i2.getArgument(0), testIndex);

            Invocation i3 = invocationIterator.next();
            assertTrue(i3.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i3.getArguments().length);
            assertEquals(i3.getArgument(0), testIndex);
            assertEquals(i3.getArgument(1), item1);
            assertEquals(i3.getArgument(2), jsonItem1);
        } finally {
            restoreIndexes(originIndexes);
        }
    }

    @Test
    public void elasticsearchBulkIndexingCanNotDeleteIndexTest() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        Map<String, String> originIndexes = cleanUpIndexes();
        String testIndex = "testIndex";

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Publication item One")
                   .withIssueDate("2019-06-20")
                   .withAuthor("Smith, Maria")
                   .withEntityType("Publication").build();


        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_OK);
        when(elasticsearchIndexProvider.deleteIndex(testIndex)).thenReturn(false);

        context.restoreAuthSystemState();

        try {
            String[] args = new String[] { "elasticsearch-bulk-indexing", "-e", "Publication", "-i", testIndex};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
            bulkIndex.initialize(args, handler, admin);
            bulkIndex.setElasticsearchIndexProvider(elasticsearchIndexProvider);
            bulkIndex.run();

            assertNotNull(handler.getException());
            assertEquals("Can not delete Index with name: " + testIndex, handler.getException().getMessage());

            java.util.Collection<Invocation> invocations =
                      Mockito.mockingDetails(elasticsearchIndexProvider).getInvocations();
            Iterator<Invocation> invocationIterator = invocations.iterator();
            assertEquals(2, invocations.size());

            Invocation i = invocationIterator.next();
            assertTrue(i.toString().startsWith("elasticsearchIndexProvider.checkIngex"));
            assertEquals(1, i.getArguments().length);
            assertEquals(i.getArgument(0), testIndex);

            Invocation i2 = invocationIterator.next();
            assertTrue(i2.toString().startsWith("elasticsearchIndexProvider.deleteIndex"));
            assertEquals(1, i2.getArguments().length);
            assertEquals(i2.getArgument(0), testIndex);
        } finally {
            restoreIndexes(originIndexes);
        }
    }

    @Test
    public void elasticsearchBulkIndexingCanNotIndexingItem1Test() throws Exception {
        context.turnOffAuthorisationSystem();

        ElasticsearchBulkIndex bulkIndex = new ElasticsearchBulkIndex();
        ElasticsearchIndexProvider elasticsearchIndexProvider =  Mockito.mock(ElasticsearchIndexProvider.class);

        Map<String, String> originIndexes = cleanUpIndexes();
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

        when(elasticsearchIndexProvider.checkIngex(testIndex)).thenReturn(HttpStatus.SC_OK);
        when(elasticsearchIndexProvider.deleteIndex(testIndex)).thenReturn(true);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item1, jsonItem1)).thenReturn(false);
        when(elasticsearchIndexProvider.indexSingleItem(testIndex, item2, jsonItem2)).thenReturn(true);

        context.restoreAuthSystemState();
        try {
            String[] args = new String[] { "elasticsearch-bulk-indexing", "-e", "Publication", "-i", testIndex};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
            bulkIndex.initialize(args, handler, admin);
            bulkIndex.setElasticsearchIndexProvider(elasticsearchIndexProvider);
            bulkIndex.run();

            assertNull(handler.getException());

            java.util.Collection<Invocation> invocations =
                      Mockito.mockingDetails(elasticsearchIndexProvider).getInvocations();
            Iterator<Invocation> invocationIterator = invocations.iterator();
            assertEquals(6, invocations.size());

            Invocation i = invocationIterator.next();
            assertTrue(i.toString().startsWith("elasticsearchIndexProvider.checkIngex"));
            assertEquals(1, i.getArguments().length);
            assertEquals(i.getArgument(0), testIndex);

            Invocation i2 = invocationIterator.next();
            assertTrue(i2.toString().startsWith("elasticsearchIndexProvider.deleteIndex"));
            assertEquals(1, i2.getArguments().length);
            assertEquals(i2.getArgument(0), testIndex);

            Invocation i3 = invocationIterator.next();
            assertTrue(i3.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i3.getArguments().length);
            assertEquals(i3.getArgument(0), testIndex);
            assertTrue(i3.getArgument(1).equals(item1) || i3.getArgument(1).equals(item2));
            assertTrue(i3.getArgument(2).equals(jsonItem1) || i3.getArgument(2).equals(jsonItem2));

            Invocation i4 = invocationIterator.next();
            assertTrue(i4.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i4.getArguments().length);
            assertEquals(i4.getArgument(0), testIndex);
            assertTrue(i4.getArgument(1).equals(item1) || i4.getArgument(1).equals(item2));
            assertTrue(i4.getArgument(2).equals(jsonItem1) || i4.getArgument(2).equals(jsonItem2));

            Invocation i5 = invocationIterator.next();
            assertTrue(i5.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i5.getArguments().length);
            assertEquals(i5.getArgument(0), testIndex);
            assertTrue(i5.getArgument(1).equals(item1) || i5.getArgument(1).equals(item2));
            assertEquals(i5.getArgument(2), jsonItem1);

            Invocation i6 = invocationIterator.next();
            assertTrue(i6.toString().startsWith("elasticsearchIndexProvider.indexSingleItem"));
            assertEquals(3, i6.getArguments().length);
            assertEquals(i6.getArgument(0), testIndex);
            assertTrue(i6.getArgument(1).equals(item1) || i6.getArgument(1).equals(item2));
            assertTrue(i6.getArgument(2).equals(jsonItem1) || i6.getArgument(2).equals(jsonItem2));
        } finally {
            restoreIndexes(originIndexes);
        }
    }

    private Map<String, String> cleanUpIndexes() {
        Map<String, String> originIndexes = null;
        Map<String, String> testIndexes = new HashMap<String, String>();
        testIndexes.put("Publication", "test_pub");
        testIndexes.put("Person", "test_pers");
        configurationService.addPropertyValue("elasticsearch.entity", "Person");
        configurationService.addPropertyValue("elasticsearch.entity", "Publication");
        originIndexes = elasticsearchIndexManager.getEntityType2Index();
        elasticsearchIndexManager.setEntityType2Index(testIndexes);
        configurationService.addPropertyValue("elasticsearchbulk.maxattempt", 3);
        return originIndexes;
    }

    private void restoreIndexes(Map<String, String> originIndexes) {
        elasticsearchIndexManager.setEntityType2Index(originIndexes);
    }
}