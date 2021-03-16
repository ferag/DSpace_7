/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.dspace.app.rest.matcher.AuthorizationMatcher.matchAuthorization;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.authorization.impl.ProfileRelatedEntityChangeFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ProfileRelatedEntityChangeFeature}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ProfileRelatedEntityChangeFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    private AuthorizationFeature canChangeProfileRelatedEntityFeature;

    private RelationshipType shadowCopy;

    private RelationshipType isCloneOf;

    private RelationshipType isOriginatedFrom;

    @Before
    public void before() throws Exception {
        canChangeProfileRelatedEntityFeature = authorizationFeatureService.find(ProfileRelatedEntityChangeFeature.NAME);
        assertThat(canChangeProfileRelatedEntityFeature, notNullValue());

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();

        EntityType publicationType = createEntityType("Publication");
        EntityType cvPublicationCloneType = createEntityType("CvPublicationClone");
        EntityType cvPublicationType = createEntityType("CvPublication");

        createEntityType("CvPerson");

        shadowCopy = createHasShadowCopyRelationship(cvPublicationCloneType, publicationType);
        isCloneOf = createCloneRelationship(cvPublicationCloneType, cvPublicationType);
        isOriginatedFrom = createIsOriginatedFromRelationship(publicationType, cvPublicationCloneType);

        Collection profileCollection = createCollection("CvPerson");
        configurationService.setProperty("researcher-profile.collection.uuid", profileCollection.getID().toString());
        configurationService.setProperty("claimable.entityType", "Person");

        context.restoreAuthSystemState();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFeatureWithCvPublicationNotAlreadyIncludedInProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);

        Collection collection = createCollection("Publication");

        Item item = createItem(collection);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        Authorization authorization = new Authorization(eperson, canChangeProfileRelatedEntityFeature, itemRest);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", hasItem(matchAuthorization(authorization))));

    }

    @Test
    public void testFeatureWithCvPublicationAlreadyIncludedInProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);

        Collection collection = createCollection("Publication");
        Collection cvCollection = createCollection("CvPublication");
        Collection cvCloneCollection = createCollection("CvPublicationClone");

        Item item = createItem(collection);
        Item cvItem = createItem(cvCollection, eperson);
        Item cvItemClone = createItem(cvCloneCollection);

        RelationshipBuilder.createRelationshipBuilder(context, cvItemClone, cvItem, isCloneOf);
        RelationshipBuilder.createRelationshipBuilder(context, item, cvItemClone, isOriginatedFrom);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    public void testFeatureWithCvPublicationAlreadyIncludedInProfileByShadowCopy() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);

        Collection collection = createCollection("Publication");
        Collection cvCollection = createCollection("CvPublication");
        Collection cvCloneCollection = createCollection("CvPublicationClone");

        Item item = createItem(collection);
        Item cvItem = createItem(cvCollection, eperson);
        Item cvItemClone = createItem(cvCloneCollection);

        RelationshipBuilder.createRelationshipBuilder(context, cvItemClone, cvItem, isCloneOf);
        RelationshipBuilder.createRelationshipBuilder(context, cvItemClone, item, shadowCopy);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFeatureWithCvPublicationAlreadyIncludedInAnotherProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);
        createProfileForUser(admin);

        Collection collection = createCollection("Publication");
        Collection cvCollection = createCollection("CvPublication");
        Collection cvCloneCollection = createCollection("CvPublicationClone");

        Item item = createItem(collection);
        Item cvItem = createItem(cvCollection, admin);
        Item cvItemClone = createItem(cvCloneCollection);

        RelationshipBuilder.createRelationshipBuilder(context, cvItemClone, cvItem, isCloneOf);
        RelationshipBuilder.createRelationshipBuilder(context, item, cvItemClone, isOriginatedFrom);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        Authorization authorization = new Authorization(eperson, canChangeProfileRelatedEntityFeature, itemRest);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", hasItem(matchAuthorization(authorization))));

    }

    @Test
    public void testFeatureWithItemEntityTypeNotIncludableInProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);

        Collection collection = createCollection("Equipment");

        Item item = createItem(collection);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    public void testFeatureWithCvPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        createProfileForUser(eperson);

        Collection collection = createCollection("Person");

        Item item = createItem(collection);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    @Test
    public void testFeatureWithUserWithoutProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = createCollection("Publication");

        Item item = createItem(collection);

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
            .param("uri", getItemUri(itemRest))
            .param("eperson", String.valueOf(eperson.getID()))
            .param("feature", ProfileRelatedEntityChangeFeature.NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

    }

    private Item createItem(Collection collection) {
        return ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();
    }

    private Item createItem(Collection collection, EPerson owner) {
        return ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner(owner.getName(), owner.getID().toString())
            .build();
    }

    private Collection createCollection(String relationshipType) {
        return CollectionBuilder.createCollection(context, parentCommunity)
            .withRelationshipType(relationshipType)
            .build();
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsOriginatedFromRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType,
            ORIGINATED.getLeftType(), ORIGINATED.getRightType(), 0, null, 0, 1).build();
    }

    private void createProfileForUser(EPerson ePerson) throws Exception {
        researcherProfileService.createAndReturn(context, ePerson);
    }

    public String getItemUri(ItemRest itemRest) {
        return utils.linkToSingleResource(itemRest, "self").getHref();
    }
}
