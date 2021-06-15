/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchConnectorImpl;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchProvider;
import org.dspace.app.elasticsearch.service.ElasticsearchIndexQueueService;
import org.dspace.app.elasticsearch.service.ElasticsearchItemBuilder;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.rest.matcher.HttpEntityRequestMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ElasticsearchIndexQueueBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite to verify the ElasticsearchQueueConsumer.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexQueueIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ElasticsearchIndexQueueService elasticsearchService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ElasticsearchProvider elasticsearchProvider;

    @Autowired
    private ElasticsearchConnectorImpl elasticsearchConnector;

    @Autowired
    private ElasticsearchItemBuilder elasticsearchIndexConverter;

    private Map<String, String> originIndexes;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Map<String, String> testIndexes = new HashMap<String, String>();
        testIndexes.put("Publication", "test_pub");
        testIndexes.put("Person", "test_pers");
        configurationService.addPropertyValue("elasticsearch.entity", "Person");
        configurationService.addPropertyValue("elasticsearch.entity", "Publication");
        this.originIndexes = elasticsearchProvider.getIndexes();
        elasticsearchProvider.setIndexes(testIndexes);
    }

    @After
    @Override
    public void destroy() throws Exception {
        elasticsearchProvider.setIndexes(originIndexes);
    }

    @Test
    public void elasticsearchIndexQueueWithCreatedItemsTest() throws Exception {
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
        ElasticsearchIndexQueue record1 = null;
        ElasticsearchIndexQueue record2 = null;
        try {
            record1 = elasticsearchService.find(context, personItem.getID());
            record2 = elasticsearchService.find(context, publicationItem.getID());
            assertNotNull(record1);
            assertEquals(personItem.getID().toString(), record1.getID().toString());
            assertEquals(Event.CREATE, record1.getOperationType().intValue());

            assertNotNull(record2);
            assertEquals(publicationItem.getID().toString(), record2.getID().toString());
            assertEquals(Event.CREATE, record2.getOperationType().intValue());
        } finally {
            context.setCurrentUser(admin);
            elasticsearchService.delete(context, record1);
            elasticsearchService.delete(context, record2);
        }
    }

    @Test
    public void elasticsearchIndexQueueWithDeletedItemTest() throws Exception {
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

        context.restoreAuthSystemState();

        ElasticsearchIndexQueue record1 = elasticsearchService.find(context, personItem.getID());
        assertNotNull(record1);
        assertEquals(personItem.getID().toString(), record1.getID().toString());
        assertEquals(Event.CREATE, record1.getOperationType().intValue());

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().is(204));

        // after deletion we have the same item but with operation type DELETE
        record1 = elasticsearchService.find(context, personItem.getID());
        assertNotNull(record1);
        assertEquals(personItem.getID().toString(), record1.getID().toString());
        assertEquals(Event.DELETE, record1.getOperationType().intValue());
    }

    @Test
    public void elasticsearchIndexQueueWithUnsupportedItemsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item patentItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Patent item Title")
                                     .withIssueDate("2021-03-17")
                                     .withAuthor("Doe, John")
                                     .withEntityType("Patent").build();

        context.restoreAuthSystemState();
        assertNull(elasticsearchService.find(context, patentItem.getID()));
    }

    @Test
    public void elasticsearchIndexQueueWithWithdrawnItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item publicationItem = ItemBuilder.createItem(context, col1)
                                     .withTitle("Publication item Title")
                                     .withIssueDate("2021-03-17")
                                     .withAuthor("Doe, John")
                                     .withEntityType("Publication").build();

        context.restoreAuthSystemState();

        ElasticsearchIndexQueue record1 = null;
        try {
            record1 = elasticsearchService.find(context, publicationItem.getID());
            assertNotNull(record1);
            assertEquals(publicationItem.getID().toString(), record1.getID().toString());
            assertEquals(Event.CREATE, record1.getOperationType().intValue());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);

            List<Operation> ops = new ArrayList<Operation>();
            ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
            ops.add(replaceOperation);
            String patchBody = getPatchContent(ops);

            // withdraw item
            getClient(tokenAdmin).perform(patch("/api/core/items/" + publicationItem.getID())
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(publicationItem.getID().toString())))
                                 .andExpect(jsonPath("$.withdrawn", Matchers.is(true)))
                                 .andExpect(jsonPath("$.inArchive", Matchers.is(false)));

            record1 = elasticsearchService.find(context, publicationItem.getID());
            assertNotNull(record1);
            assertEquals(publicationItem.getID().toString(), record1.getID().toString());
            assertEquals(Event.DELETE, record1.getOperationType().intValue());
        } finally {
            context.setCurrentUser(admin);
            elasticsearchService.delete(context, record1);
        }
    }

    @Test
    public void elasticsearchIndexQueuePatchItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title item A").build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        ElasticsearchIndexQueue record = null;
        try {

            record = elasticsearchService.find(context, itemA.getID());
            assertNotNull(record);
            assertEquals(itemA.getID().toString(), record.getID().toString());
            assertEquals(Event.CREATE, record.getOperationType().intValue());

            List<Operation> ops = new ArrayList<>();
            List<Map<String, String>> values = new ArrayList<>();
            Map<String, String> value = new HashMap<>();
            value.put("value", "New Title");
            values.add(value);
            ops.add(new ReplaceOperation("/metadata/dc.title", values));

            getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                                 .content(getPatchContent(ops))
                                 .contentType(contentType))
                                 .andExpect(status().isOk());

            record = elasticsearchService.find(context, itemA.getID());
            assertNotNull(record);
            assertEquals(itemA.getID().toString(), record.getID().toString());
            assertEquals(Event.MODIFY_METADATA, record.getOperationType().intValue());
        } finally {
            context.setCurrentUser(admin);
            elasticsearchService.delete(context, record);
        }
    }

    @Test
    public void sendElasticsearchIndexQueueWithCreateOperationTypeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originHttpClient = elasticsearchConnector.getHttpClient();
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        try {
            elasticsearchConnector.setHttpClient(mockHttpClient);

            HttpResponse response = mock(HttpResponse.class);
            when(response.getStatusLine()).thenReturn(statusLine(new ProtocolVersion("http", 1, 1),
                                                                     HttpStatus.SC_CREATED, "OK"));

            when(mockHttpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1").build();

            Item publicationItem = ItemBuilder.createItem(context, col1)
                                              .withTitle("Publication item Title")
                                              .withIssueDate("2020-06-25")
                                              .withAuthor("Smith, Maria")
                                              .withEntityType("Publication").build();

            ElasticsearchIndexQueue record = elasticsearchService.find(context, publicationItem.getID());
            assertNotNull(record);
            assertEquals(publicationItem.getID().toString(), record.getID().toString());
            assertEquals(Event.CREATE, record.getOperationType().intValue());
            String json = elasticsearchIndexConverter.convert(context, record);

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-elasticsearch" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            verify(mockHttpClient).execute(ArgumentMatchers.argThat(new HttpEntityRequestMatcher(json, "POST" )));

            record = elasticsearchService.find(context, publicationItem.getID());
            assertNull(record);
        } finally {
            elasticsearchConnector.setHttpClient(originHttpClient);
        }
    }

    @Test
    public void sendElasticsearchIndexQueueWithModifyMetadataOperationTypeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originHttpClient = elasticsearchConnector.getHttpClient();
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        try {
            elasticsearchConnector.setHttpClient(mockHttpClient);

            HttpResponse response = mock(HttpResponse.class);
            when(response.getStatusLine()).thenReturn(statusLine(new ProtocolVersion("http", 1, 1),
                                                                     HttpStatus.SC_OK, "OK"));

            when(mockHttpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1").build();

            Item publicationItem = ItemBuilder.createItem(context, col1)
                                              .withTitle("Publication item Title")
                                              .withIssueDate("2020-09-20")
                                              .withAuthor("Smith, Maria")
                                              .withEntityType("Publication").build();

            List<Operation> ops = new ArrayList<>();
            List<Map<String, String>> values = new ArrayList<>();
            Map<String, String> value = new HashMap<>();
            value.put("value", "New Title");
            values.add(value);
            ops.add(new ReplaceOperation("/metadata/dc.title", values));

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(patch("/api/core/items/" + publicationItem.getID())
                                 .content(getPatchContent(ops))
                                 .contentType(contentType))
                                 .andExpect(status().isOk());

            ElasticsearchIndexQueue record = elasticsearchService.find(context, publicationItem.getID());
            assertNotNull(record);
            assertEquals(publicationItem.getID().toString(), record.getID().toString());
            assertEquals(Event.MODIFY_METADATA, record.getOperationType().intValue());
            String json = elasticsearchIndexConverter.convert(context, record);

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-elasticsearch" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            verify(mockHttpClient).execute(ArgumentMatchers.argThat(new HttpEntityRequestMatcher(json, "POST" )));

            record = elasticsearchService.find(context, publicationItem.getID());
            assertNull(record);

        } finally {
            elasticsearchConnector.setHttpClient(originHttpClient);
        }
    }

    @Test
    public void sendElasticsearchIndexQueueWithDeleteOperationTypeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originHttpClient = elasticsearchConnector.getHttpClient();
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        try {
            elasticsearchConnector.setHttpClient(mockHttpClient);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");

            when(mockHttpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            UUID uuid = UUID.randomUUID();
            ElasticsearchIndexQueueBuilder.createElasticsearchIndexQueue(context, uuid, Event.DELETE).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-elasticsearch" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            java.util.Collection<Invocation> invocations = Mockito.mockingDetails(mockHttpClient).getInvocations();
            assertEquals(2, invocations.size());
            Iterator<Invocation> invocationIterator = invocations.iterator();
            assertTrue(invocationIterator.next().getArgument(0).toString().startsWith("GET"));
            assertTrue(invocationIterator.next().getArgument(0).toString().startsWith("DELETE"));

            assertNull(elasticsearchService.find(context, uuid));
        } finally {
            elasticsearchConnector.setHttpClient(originHttpClient);
        }
    }

    @Test
    public void sendElasticsearchIndexQueueIndexNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        UUID uuid = UUID.randomUUID();
        ElasticsearchIndexQueueBuilder.createElasticsearchIndexQueue(context, uuid, 100).build();

        context.restoreAuthSystemState();

        String[] args = new String[] { "update-elasticsearch" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));
        Exception exception = handler.getException();
        assertNotNull(exception);
        assertEquals("Not found index for ElasticsearchIndexQueue with uuid: " + uuid.toString(),
                     exception.getMessage());
    }

    private StatusLine statusLine(final ProtocolVersion protocolVersion, int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return protocolVersion;
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}