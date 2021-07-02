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
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.time.Year;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
import org.dspace.app.elasticsearch.externalservice.ElasticsearchIndexProvider;
import org.dspace.app.elasticsearch.script.bulkindex.ElasticsearchBulkIndex;
import org.dspace.app.elasticsearch.service.ElasticsearchItemBuilder;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
import org.dspace.metrics.wos.UpdateWOSMetrics;
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

        List<String> docsItem1 = elasticsearchItemBuilder.convert(context, item1);
        List<String> docsItem2 = elasticsearchItemBuilder.convert(context, item2);

        assertEquals(1, docsItem1.size());
        assertEquals(1, docsItem2.size());

        String jsonItem1 = docsItem1.get(0);
        String jsonItem2 = docsItem2.get(0);

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


        List<String> docsItem1 = elasticsearchItemBuilder.convert(context, item1);
        assertEquals(1, docsItem1.size());

        String jsonItem1 = docsItem1.get(0);

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

        List<String> docsItem1 = elasticsearchItemBuilder.convert(context, item1);
        assertEquals(1, docsItem1.size());

        String jsonItem1 = docsItem1.get(0);

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

        List<String> docsItem1 = elasticsearchItemBuilder.convert(context, item1);
        List<String> docsItem2 = elasticsearchItemBuilder.convert(context, item2);

        assertEquals(1, docsItem1.size());
        assertEquals(1, docsItem2.size());

        String jsonItem1 = docsItem1.get(0);
        String jsonItem2 = docsItem2.get(0);

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

    @Test
    public void crosswalkForPublicationJsonToBeSentToElasticsearchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        try (FileInputStream file = new FileInputStream(testProps.get("test.publicationCrosswalk").toString())) {

            String json = IOUtils.toString(file, Charset.defaultCharset());

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1").build();

            Item item = createFullPublicationItemForElasticsearch(col1);

            CrisMetricsBuilder.createCrisMetrics(context, item)
                              .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                              .withMetricCount(1007)
                              .isLast(true).build();

            CrisMetricsBuilder.createCrisMetrics(context, item)
                              .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                              .withMetricCount(5)
                              .isLast(true).build();

            context.restoreAuthSystemState();
            String[] args = new String[] {"update-metrics-in-solr"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            List<String> generatedDocs = elasticsearchItemBuilder.convert(context, item);
            assertEquals(1, generatedDocs.size());
            assertTrue(generatedDocs.get(0).contains(json));
        }
    }

    private Item createFullPublicationItemForElasticsearch(Collection collection) {
        return ItemBuilder.createItem(context, collection)
                          .withEntityType("Publication")
                          .withIsPartOf("Journal")
                          .withRelationFunding("Test funding")
                          .withAuthor("John Smith").withAuthor("Artur Noris")
                          .withEmbargoEnd("2022-01-01")
                          .withAuthorAffiliation("Affiliation 4Science").withAuthorAffiliation("Test Affiliation")
                          .withRenatiDiscipline("discipline")
                          .withSubject("Subject test").withSubject("Subject export")
                       // .withDegreeGrantor("Test grantor")
                          .withEditorOrcid("0000-0002-9079-593X").withEditorOrcid("0000-0000-0000-1234")
                          .withVolume("V01")
                          .withDoiIdentifier("10.1000/182")
                          .withPerucrisSubjectOCDE("oecd::Ingeniería, Tecnología::Ingeniería mecánica")
                          .withPerucrisSubjectOCDE("oecd::Ingeniería, Tecnología::Biotecnología ambiental"
                                           + "::Biorremediación, Biotecnologías de diagnóstico en la gestión ambiental")
                          .withRelationIssn("0002").withRelationIssn("0023")
                          .withIssueDate("2021-06-28")
                       // .withDegreeName("Test degree name")
                          .withAccess("embargoed access")
                          .withCitationEndPage("20")
                          .withRenatiType("Test renati Type")
                          .withSubjectLoc("Test loc")
                          .withType("Test Type")
                          .withType("Test Type 2")
                          .withRelationIsbn("ISBN-01")
                          .withRelationDataset("DataSet")
                          .withIsbnIdentifier("11-22-33")
                          .withEditor("Editor 1")
                          .withEditor("Editor 2")
                          .withLanguage("en")
                          .withLanguage("it")
                          .withCitationIdentifier("CIT-01")
                          .withTitle("Publication Title")
                          .withAdvisorDni("123456")
                          .withAdvisorOrcid("0000-0002-0000-9999")
                          .withAuthorOrcid("0000-0000-0000-7777").withAuthorOrcid("0000-0000-0000-8888")
                          .withVersion("V01")
                          .withCoverageIsbn("Test Coverage Isbn")
                          .withIssue("03")
                          .withDescription("Publication Description")
                          .withPmidIdentifier("1234567890")
                          .withRelationDoi("doi:10.3972/test")
                          .withEditorDni("editor-dni-0001")
                          .withEditorDni("editor-dni-0002")
                          .withRelationGrantno("relation-grantno-01")
                          .withIsiIdentifier("111-222-333")
                          .withCoveragePublication("Coverage publication")
                          .withAdvisor("First advisor")
                          .withDDC("test-ddc")
                          .withRightsUri("rights-uri")
                          .withRelationIspartofseries("ispartofseries")
                          .withIdentifierUrl("identifier-url")
                       // .withDegreeDiscipline("degree-discipline")
                          .withDescriptionAbstract("This is a publication")
                          .withDescriptionSponsorship("description- sponsorship")
                          .withRenatiLevel("Renati-Level-22")
                          .withInternalNote("Note")
                          .withRelationPublication("Published in publication")
                          .withRelationConference("Relation-Conference")
                          .withEditorAffiliation("Editor Affiliation-1").withEditorAffiliation("Editor Affiliation-2")
                          .withAlternativeTitle("Alternative publication title")
                          .withAuthorDniIdentifier("Author-Dni-1").withAuthorDniIdentifier("Author-Dni-2")
                          .withScopusIdentifier("scopus-id-1")
                          .withCoverageDoi("coverage-doi-1")
                          .withCitationStartPage("1")
                          .withRelationProject("First project")
                          .withSubjectMesh("Subject-mesh-1").withSubjectMesh("Subject-mesh-2")
                          .build();
    }

    @Test
    public void crosswalkForOrgUnitJsonToBeSentToElasticsearchTest() throws Exception {
        context.turnOffAuthorisationSystem();
        try (FileInputStream file = new FileInputStream(testProps.get("test.orgunitCrosswalk").toString())) {

            String json = IOUtils.toString(file, Charset.defaultCharset());
            String replacedJson = json.replace("{", "").trim();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1").build();

            Item item = createFullOrgunitItemForElasticsearch(col1);

            List<String> generatedDocs = elasticsearchItemBuilder.convert(context, item);
            assertEquals(1, generatedDocs.size());
            assertTrue(generatedDocs.get(0).contains(replacedJson));

        }
    }

    private Item createFullOrgunitItemForElasticsearch(Collection collection) {
        return ItemBuilder.createItem(context, collection)
                          .withEntityType("OrgUnit")
                          .withSubject("Science").withSubject("Test Subject")
                          .withRegistrationNumber("RenRegNum20201008")
                          .withUbigeo("UbiGeo::La Libertad::Trujillo")
                          .withParentOrganization("Parent OrgUnit")
                          .withRegistration("UNT")
                          .withOrgUnitAddressCountry("PE")
                          .withDateOfQualification("2008-01-01")
                          .withOrgUnitAddressLocality("trujillo")
                          .withOrgUnitScopusAffiliationIdentifier("SCOPUS-01")
                          .withOrganizationRuc("RUC-UNTR")
                          .withStiActions("yes")
                          .withRenacytOcde("oecd::Ciencias agrícolas::Ciencia veterinaria::Ciencia veterinaria")
                          .withOrgUnitLegalName("Universidad de Trujillo (UNT)")
                          .withOrgUnitRinIdentifier("RIN-01")
                          .withRenacytStrength("Gran capacidad de obtención de finanaciamiento")
                          .withFormalUnit("yes")
                          .withSectorInstitucional("Higher education")
                          .withOrgUnitRorIdentifier("ROR-01")
                          .withDescriptionAbstract("This is an OrgUnit")
                          .withEducacionSuperior("Institución universitaria e institución con rango universitario")
                          .withSubjectOCDE("oecd::Ingeniería, Tecnología::Ingeniería médica::Ingeniería médica")
                          .withSubjectOCDE("oecd::Ciencias médicas, Ciencias de la salud"
                                             + "::Ciencias de la salud::Epidemiología")
                          .withInternalNote("Nota interna dentro de una organizacion")
                          .withType("type-1")
                          .withOrgUnitIdentifier("ID-01")
                          .withOrgUnitType("Research group")
                          .withIndustrialClassification("IND-CLASS")
                          .withTypeNaturaleza("Public")
                          .withValidityOfRegistration("2020-01-01")
                          .withTitle("Universidad Nacional de Trujillo")
                          .withOrgUnitCrossRefFunderIdentifier("CRF-01")
                          .withOrgUnitIsniIdentifier("ISNI-01")
                          .withPersonalSiteUrl("www.test.com")
                          .withPersonalSiteUrl("www.test2.com")
                          .withRenacytClassification("Level 1")
                          .withAcronym("UNITRU")
                          .build();
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