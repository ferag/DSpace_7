/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import com.google.gson.JsonObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.discovery.SearchService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * This IT's verify correct update of relationship references on solr
 * after positions are updated on database.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class RelationshipsPositionIndexingIT extends AbstractEntityIntegrationTest {
    private Item author1;
    private Item author2;
    private Item author3;
    private Item publication1;
    private Item publication2;
    private Item publication3;
    private Item publication4;

    private RelationshipType selectedResearchOutput;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private ConfigurationService configurationService;

    final SearchService searchService = new DSpace().getSingletonService(SearchService.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        final EntityType personEntity = getEntityType("Person");

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        final Community childCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                         .withName("Sub Community")
                                                         .build();
        final Collection personCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                             .withEntityType("Person")
                                                             .withName("Person").build();

        final Collection publicationCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                                  .withEntityType("Publication")
                                                                  .withName("Publications").build();

        final Collection patentCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                             .withEntityType("Patent")
                                                             .withName("Patent").build();

        author1 = ItemBuilder.createItem(context, personCollection)
                             .withTitle("Author1")
                             .withAuthor("Smith, Donald")
                             .withPersonIdentifierLastName("Smith")
                             .withPersonIdentifierFirstName("Donald")
                             .build();

        author2 = ItemBuilder.createItem(context, personCollection)
                             .withTitle("Author2")
                             .withAuthor("Smith, Maria")
                             .build();

        author3 = ItemBuilder.createItem(context, personCollection)
                             .withTitle("Author3")
                             .withAuthor("Doe, John")
                             .build();


        publication1 = ItemBuilder.createItem(context, publicationCollection)
                                  .withTitle("Publication1")
                                  .withAuthor("Testy, TEst")
                                  .withIssueDate("2015-01-01")
                                  .build();

        publication2 = ItemBuilder.createItem(context, patentCollection)
                             .withTitle("Publication 2")
                             .withAuthor("Testy, Foo")
                             .withIssueDate("2015-08-01")
                             .build();

        publication3 = ItemBuilder.createItem(context, patentCollection)
                                  .withTitle("Publication 3")
                                  .withAuthor("Testy, Foo")
                                  .withIssueDate("2015-08-01")
                                  .build();

        publication4 = ItemBuilder.createItem(context, patentCollection)
                                  .withTitle("Publication 4")
                                  .withAuthor("Testzz, Foo")
                                  .withIssueDate("2017-08-01")
                                  .build();

        selectedResearchOutput = RelationshipTypeBuilder
                                     .createRelationshipTypeBuilder(
                                         context,
                                         null,
                                         personEntity,
                                         "isResearchoutputsSelectedFor",
                                         "hasSelectedResearchoutputs",
                                         0, null,
                                         0, null).build();

        context.restoreAuthSystemState();
    }

    /**
     * This test reproduces the creation, position update and deletion of relationships between items,
     * and check correct update of SOLR documents of all items involved (directly or not) in the updated
     * relationship.
     *
     * @throws Exception
     */
    @Test
    public void relationPlacesIndexed() throws Exception {
        configurationService.setProperty("relationship.places.onlyright",
                                         "null::Person::isResearchoutputsSelectedFor::hasSelectedResearchoutputs");
        context.turnOffAuthorisationSystem();
        final Relationship author1ToPublication1 =
            RelationshipBuilder.createRelationshipBuilder(context, publication1,
                                                          author1, selectedResearchOutput, -1, -1)
                               .build();
        final Relationship author2ToPublication1 =
            RelationshipBuilder.createRelationshipBuilder(context, publication1, author2, selectedResearchOutput,
                                                          -1, -1)
                               .build();

        final Relationship author3ToPublication1 =
            RelationshipBuilder.createRelationshipBuilder(context, publication1, author3, selectedResearchOutput ,
                                                          -1, -1)
                               .build();

        final Relationship author1ToPublication2 =
            RelationshipBuilder.createRelationshipBuilder(context, publication2, author1, selectedResearchOutput,
                                                          -1, -1)
                               .build();
        final Relationship author1ToPublication3 =
            RelationshipBuilder.createRelationshipBuilder(context, publication3, author1, selectedResearchOutput,
                                                          -1, -1)
                               .build();

        final Relationship author2ToPublication4 =
            RelationshipBuilder.createRelationshipBuilder(context, publication4, author2, selectedResearchOutput,
                                                          -1, -1)
                               .build();
        context.restoreAuthSystemState();

        QueryResponse queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author2.getID()),
                       UUIDUtils.toString(author2.getID()),
                       UUIDUtils.toString(author3.getID())
                                              ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID())
                       ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID())));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication4.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author2.getID())));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID()),
                       UUIDUtils.toString(publication2.getID()),
                       UUIDUtils.toString(publication3.getID())
                       ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID()),
                       UUIDUtils.toString(publication4.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID())
                                     ));

        queryResponse = performQuery("*:*",
                                     "relation.isResearchoutputsSelectedFor:" + UUIDUtils.toString(author1.getID()));
        final SolrDocumentList results = queryResponse.getResults();
        assertThat(results.size(), is(3));
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("rightPlace", 0);

        final String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/relationships/" + author1ToPublication3.getID())
                                     .contentType("application/json")
                                     .content(contentObj.toString()))
                                                .andExpect(status().isOk());
        getClient(token).perform(put("/api/core/relationships/" + author2ToPublication4.getID())
                                     .contentType("application/json")
                                     .content(contentObj.toString()))
                        .andExpect(status().isOk());

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID())
                       ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author2.getID()),
                       UUIDUtils.toString(author3.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication4.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author2.getID()),
                       UUIDUtils.toString(author2.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID()),
                       UUIDUtils.toString(publication2.getID()),
                       UUIDUtils.toString(publication3.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID()),
                       UUIDUtils.toString(publication4.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID())
                                     ));

        getClient(token).perform(delete("/api/core/relationships/" + author2ToPublication4.getID()))
                        .andExpect(status().isNoContent());
        getClient(token).perform(delete("/api/core/relationships/" + author1ToPublication1.getID()))
                        .andExpect(status().isNoContent());

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author2.getID()),
                       UUIDUtils.toString(author3.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication4.getID()));
        assertNull(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication2.getID()),
                       UUIDUtils.toString(publication3.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication1.getID())
                                     ));

        context.turnOffAuthorisationSystem();

        final Relationship author1ToPublication4 =
            RelationshipBuilder.createRelationshipBuilder(context, publication4, author1, selectedResearchOutput,
                                                          -1, -1)
            .build();
        context.restoreAuthSystemState();

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(author1.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.hasSelectedResearchoutputs"),
                   containsInAnyOrder(
                       UUIDUtils.toString(publication2.getID()),
                       UUIDUtils.toString(publication3.getID()),
                       UUIDUtils.toString(publication4.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication3.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication2.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID()),
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication4.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID())
                                     ));

        queryResponse = performQuery("search.resourceid:" + UUIDUtils.toString(publication4.getID()));
        assertThat(queryResponse.getResults().get(0).getFieldValues("relation.isResearchoutputsSelectedFor"),
                   containsInAnyOrder(
                       UUIDUtils.toString(author1.getID())
                                     ));

        configurationService.setProperty("relationship.places.onlyright",
                                         "");
    }

    private QueryResponse performQuery(final String query, String... filterQuery)
        throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.addFilterQuery(filterQuery);
        return  searchService.getSolrSearchCore().getSolr().query(solrQuery);
    }

    private EntityType getEntityType(final String entityType) throws SQLException {
        final EntityType result = entityTypeService.findByEntityType(context, entityType);
        return Optional.ofNullable(result)
                       .orElseGet(() -> EntityTypeBuilder.createEntityTypeBuilder(context, entityType)
                                                         .build());
    }

}
