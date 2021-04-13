/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.RelationshipBuilder.createRelationshipBuilder;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@link CvEntityRestRepository}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private SearchService searchService;

    private RelationshipType institutionShadowCopy;

    private RelationshipType cvShadowCopy;

    private RelationshipType isCloneOf;

    private RelationshipType isOriginatedFrom;

    private RelationshipType isMergedIn;

    private Collection collection;

    private Collection institutionCollection;

    private Collection cvCloneCollection;

    private Collection cvCollection;

    private ResearcherProfile researcherProfile;

    @Before
    public void before() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        EntityType publicationType = createEntityType("Publication");
        EntityType institutionPublicationType = createEntityType("InstitutionPublication");
        EntityType cvPublicationCloneType = createEntityType("CvPublicationClone");
        EntityType cvPublicationType = createEntityType("CvPublication");

        institutionShadowCopy = createHasShadowCopyRelationship(institutionPublicationType, publicationType);
        cvShadowCopy = createHasShadowCopyRelationship(cvPublicationCloneType, publicationType);
        isCloneOf = createCloneRelationship(cvPublicationCloneType, cvPublicationType);
        isOriginatedFrom = createIsOriginatedFromRelationship(publicationType, cvPublicationCloneType);
        isMergedIn = createIsMergedInRelationship(publicationType);

        collection = createCollection("Publication");

        cvCloneCollection = createCollection("CvPublicationClone");
        configurationService.setProperty("cti-vitae.clone.publication-collection-id",
            cvCloneCollection.getID().toString());

        cvCollection = createCollection("CvPublication");
        configurationService.setProperty("researcher-profile.publication.collection.uuid",
            cvCollection.getID().toString());

        institutionCollection = createCollection("InstitutionPublication");

        Collection profileCollection = createCollection("CvPerson");
        configurationService.setProperty("researcher-profile.collection.uuid", profileCollection.getID().toString());

        researcherProfile = createProfileForUser(eperson);

        context.restoreAuthSystemState();
    }

    @Test
    public void testCreateAndReturn() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2021-03-17")
            .build();

        String publicationId = publication.getID().toString();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/cventities")
            .param("item", publicationId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.type", is("cventity")));

        List<Relationship> cvShadowCopyRelations = findRelations(publication, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));

        Item cvPublicationClone = cvShadowCopyRelations.get(0).getLeftItem();
        assertThat(cvPublicationClone.getOwningCollection(), is(equalTo(cvCloneCollection)));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.title", "Test publication")));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-03-17")));

        List<Relationship> cloneRelations = findRelations(cvPublicationClone, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        Item cvPublication = cloneRelations.get(0).getRightItem();
        assertThat(cvPublication.getOwningCollection(), is(equalTo(cvCollection)));
        assertThat(cvPublication.getMetadata(), hasItem(with("dc.title", "Test publication")));
        assertThat(cvPublication.getMetadata(), hasItem(with("dc.date.issued", "2021-03-17")));
        assertThat(cvPublication.getMetadata(), hasItem(with("cris.owner", eperson.getName(), null,
            eperson.getID().toString(), 0, 600)));
        assertThat(cvPublication.getMetadata(), hasItem(with("perucris.ctivitae.owner",
            researcherProfile.getFullName(), null, researcherProfile.getItemId().toString(), 0, 600)));


        Map<String, List<String>> cvPublicationFields =
            solrSearchFields(cvPublication.getID(), "perucris.ctivitae.owner", "perucris.ctivitae.owner_authority");

        assertThat(cvPublicationFields
            .get("perucris.ctivitae.owner").get(0), is(researcherProfile.getFullName()));
        assertThat(cvPublicationFields
            .get("perucris.ctivitae.owner_authority").get(0), is(researcherProfile.getItemId().toString()));

        Map<String, List<String>> publicationFields =
            solrSearchFields(publication.getID(), "search.resourceid");

        assertThat(publicationFields.get("search.resourceid").get(0), is(publicationId));

        try {
            solrSearchFields(publication.getID(), "perucris.ctivitae.owner", "perucris.ctivitae.owner_authority");
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(SearchServiceException.class.getName()));
        }

    }

    @Test
    public void testCreateAndReturnWithInstitutionItem() throws Exception {

        context.turnOffAuthorisationSystem();

        Item institutionPublication = ItemBuilder.createItem(context, institutionCollection)
            .withTitle("Test publication")
            .withIssueDate("2021-03-17")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2021-03-17")
            .build();

        createRelationshipBuilder(context, institutionPublication, publication, institutionShadowCopy);

        String publicationId = publication.getID().toString();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/cventities")
            .param("item", publicationId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.type", is("cventity")));

        assertThat(findRelations(publication, cvShadowCopy), empty());
        assertThat(findRelations(publication, institutionShadowCopy), hasSize(1));

        List<Relationship> mergedInRelations = findRelations(publication, isMergedIn);
        assertThat(mergedInRelations, hasSize(1));

        Item mergedInPublication = mergedInRelations.get(0).getLeftItem();
        assertThat(mergedInPublication.isArchived(), is(equalTo(false)));
        assertThat(mergedInPublication.isWithdrawn(), is(equalTo(true)));
        assertThat(mergedInPublication.getOwningCollection(), is(equalTo(collection)));
        assertThat(mergedInPublication.getMetadata(), hasItem(with("dc.title", "Test publication")));
        assertThat(mergedInPublication.getMetadata(), hasItem(with("dc.date.issued", "2021-03-17")));

        List<Relationship> cvShadowCopyRelations = findRelations(mergedInPublication, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));

        Item cvPublicationClone = cvShadowCopyRelations.get(0).getLeftItem();
        assertThat(cvPublicationClone.getOwningCollection(), is(equalTo(cvCloneCollection)));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.title", "Test publication")));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-03-17")));

        List<Relationship> cloneRelations = findRelations(cvPublicationClone, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        Item cvPublication = cloneRelations.get(0).getRightItem();
        assertThat(cvPublication.getOwningCollection(), is(equalTo(cvCollection)));
        assertThat(cvPublication.getMetadata(), hasItem(with("dc.title", "Test publication")));
        assertThat(cvPublication.getMetadata(), hasItem(with("dc.date.issued", "2021-03-17")));
        assertThat(cvPublication.getMetadata(), hasItem(with("cris.owner", eperson.getName(), null,
            eperson.getID().toString(), 0, 600)));
        assertThat(cvPublication.getMetadata(), hasItem(with("perucris.ctivitae.owner",
            researcherProfile.getFullName(), null, researcherProfile.getItemId().toString(), 0, 600)));

        List<Relationship> isOriginatedFromRelations = findRelations(cvPublicationClone, isOriginatedFrom);
        assertThat(isOriginatedFromRelations, hasSize(1));
        assertThat(isOriginatedFromRelations.get(0).getLeftItem(), is(equalTo(publication)));

        Map<String, List<String>> cvPublicationFields =
            solrSearchFields(cvPublication.getID(), "perucris.ctivitae.owner", "perucris.ctivitae.owner_authority");

        assertThat(cvPublicationFields
            .get("perucris.ctivitae.owner").get(0), is(researcherProfile.getFullName()));
        assertThat(cvPublicationFields
            .get("perucris.ctivitae.owner_authority").get(0), is(researcherProfile.getItemId().toString()));

        Map<String, List<String>> publicationFields =
            solrSearchFields(publication.getID(), "search.resourceid");

        assertThat(publicationFields.get("search.resourceid").get(0), is(publicationId));

        try {
            solrSearchFields(publication.getID(), "perucris.ctivitae.owner", "perucris.ctivitae.owner_authority");
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(SearchServiceException.class.getName()));
        }

        Map<String, List<String>> institutionPublicationFields =
            solrSearchFields(institutionPublication.getID(), "search.resourceid");

        assertThat(institutionPublicationFields.get("search.resourceid").get(0),
            is(institutionPublication.getID().toString()));

        try {
            solrSearchFields(institutionPublication.getID(),
                "perucris.ctivitae.owner", "perucris.ctivitae.owner_authority");
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(SearchServiceException.class.getName()));
        }

    }

    @Test
    public void testCreateAndReturnWithoutLoggedInUser() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2021-03-17")
            .build();

        String publicationId = publication.getID().toString();

        context.restoreAuthSystemState();

        getClient().perform(post("/api/cris/cventities")
            .param("item", publicationId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testCreateAndReturnWithoutProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2021-03-17")
            .build();

        String publicationId = publication.getID().toString();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/cventities")
            .param("item", publicationId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

    }

    @Test
    public void testCreateAndReturnWithoutItemParameter() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/cventities")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testCreateAndReturnWithUnknownItemIdParameter() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/cventities")
            .param("item", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());

    }

    private Collection createCollection(String relationshipType) throws SQLException, AuthorizeException {
        return CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType(relationshipType)
            .withSubmitterGroup(eperson)
            .build();
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsMergedInRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, MERGED.getLeftType(),
            MERGED.getRightType(), 0, 1, 0, null).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsOriginatedFromRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType,
            ORIGINATED.getLeftType(), ORIGINATED.getRightType(), 0, null, 0, 1).build();
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private ResearcherProfile createProfileForUser(EPerson ePerson) throws Exception {
        return researcherProfileService.createAndReturn(context, ePerson);
    }

    private Map<String, List<String>> solrSearchFields(UUID itemId, String... fields) throws SearchServiceException {
        DiscoverResult idLookup = discoveryLookup(itemId, fields);
        IndexableObject idxObj = idLookup.getIndexableObjects().get(0);
        List<DiscoverResult.SearchDocument> searchDocument = idLookup.getSearchDocument(idxObj);
        Map<String, List<String>> searchFields = searchDocument.get(0).getSearchFields();
        return searchFields;
    }

    private DiscoverResult discoveryLookup(UUID resourceId, String... searchFields) throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourceid:" + UUIDUtils.toString(resourceId));
        Arrays.stream(searchFields).forEach(discoverQuery::addSearchField);
        return searchService.search(context, discoverQuery);
    }
}
