/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.matcher.CrisMetricsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.metrics.scopus.ScopusPersonProvider.ScopusPersonMetric;
import org.dspace.metrics.scopus.ScopusPersonRestConnector;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
*/
public class UpdateScopusPersonMetricsIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CrisMetricsService crisMetriscService;

    @Autowired
    private ScopusPersonRestConnector hindexConnector;

    @Test
    public void updateHindexCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.H_INDEX.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newHIndexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertNotEquals(newHIndexMetric.getID(), metric.getID());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + newHIndexMetric.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newHIndexMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + newHIndexMetric.getID())));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateCitedCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.CITED.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.CITED.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCitedMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.CITED.metricType(), itemA.getID());

            assertNotEquals(newCitedMetric.getID(), metric1.getID());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + newCitedMetric.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCitedMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + newCitedMetric.getID())));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateDocumentCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.DOCUMENT.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.DOCUMENT.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newDocumentMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.DOCUMENT.metricType(), itemA.getID());

            assertNotEquals(newDocumentMetric.getID(), metric1.getID());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + newDocumentMetric.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newDocumentMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + newDocumentMetric.getID())));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateCitationCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.CITATION.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                                            ScopusPersonMetric.CITATION.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCitationMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.CITATION.metricType(), itemA.getID());

            assertNotEquals(newCitationMetric.getID(), metric1.getID());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + newCitationMetric.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCitationMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + newCitationMetric.getID())));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateCoauthorCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "OK");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.COAUTHOR.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.COAUTHOR.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCoauthorMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.COAUTHOR.metricType(), itemA.getID());

            assertNotEquals(newCoauthorMetric.getID(), metric1.getID());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + newCoauthorMetric.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCoauthorMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + newCoauthorMetric.getID())));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateHIndexCrisMetricNotFoundResourceMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexResouceNotFound").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(
                                                  new ProtocolVersion("http", 1, 1), 404, "Not Found");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.H_INDEX.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics hindexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertEquals(hindexMetric.getID(), metric1.getID());
            assertEquals(hindexMetric.getMetricCount(), metric1.getMetricCount(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateHIndexCrisMetricApiKeyInvalidMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexApiKeyInvalid").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");
            hindexConnector.setEnhanced(true);

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(
                                                  new ProtocolVersion("http", 1, 1), 401, "Unauthorized");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.H_INDEX.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics hindexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertEquals(hindexMetric.getID(), metric1.getID());
            assertEquals(hindexMetric.getMetricCount(), metric1.getMetricCount(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

    @Test
    public void updateHIndexCrisMetricMissinToSetEnhancedMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        HttpClient originalHttpClient = hindexConnector.getHttpClient();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexApiKeyInvalid").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            hindexConnector.setHttpClient(httpClient);
            hindexConnector.setUrl("www.testurl.org/");

            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(
                                                  new ProtocolVersion("http", 1, 1), 401, "Unauthorized");
            basicHttpResponse.setEntity(new BasicHttpEntity());
            InputStream inputStream = new StringInputStream(jsonExample);
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpResponse.setEntity(basicHttpEntity);
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(inputStream);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(basicHttpResponse);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withRelationshipType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person", "-p",
                    ScopusPersonMetric.H_INDEX.name() };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics hindexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertEquals(hindexMetric.getID(), metric1.getID());
            assertEquals(hindexMetric.getMetricCount(), metric1.getMetricCount(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            hindexConnector.setHttpClient(originalHttpClient);
            hindexConnector.setEnhanced(null);
            hindexConnector.setUrl("");
        }
    }

}