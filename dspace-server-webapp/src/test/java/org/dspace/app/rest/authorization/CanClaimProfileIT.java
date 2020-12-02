/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.ClaimItemFeature;
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
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of Profile Claim Authorization Feature implementation
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CanClaimProfileIT extends AbstractControllerIntegrationTest {

    private Collection claimableCollectionA;
    private Collection notClaimableCollection;
    private Item collectionAProfile;
    private Item collectionBProfile;
    private Item notClaimableCollectionProfile;
    private Item publication;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private Utils utils;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private AuthorizationFeature claimProfileFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).withName("Community").build();
        claimableCollectionA = CollectionBuilder.createCollection(context, community).withRelationshipType("Person")
                                                .withName("claimableA").build();
        final Collection claimableCollectionB =
            CollectionBuilder.createCollection(context, community).withRelationshipType("Person")
                             .withName("claimableB").build();
        notClaimableCollection = CollectionBuilder.createCollection(context, community).withRelationshipType("Person")
                                                  .withName("notClaimable").build();
        Collection publicationCollection =
            CollectionBuilder.createCollection(context, community).withRelationshipType("Publication")
                             .withName("notClaimable").build();

        collectionAProfile =
            ItemBuilder.createItem(context, claimableCollectionA).withRelationshipType("Person").build();
        collectionBProfile =
            ItemBuilder.createItem(context, claimableCollectionB).withRelationshipType("Person").build();

        publication =
            ItemBuilder.createItem(context, publicationCollection).withRelationshipType("Publication").build();

        notClaimableCollectionProfile =
            ItemBuilder.createItem(context, notClaimableCollection).withRelationshipType("Person").build();

        configurationService.addPropertyValue("claimable.entityType", "Person");
        configurationService.addPropertyValue("claimable.collection.uuid", claimableCollectionA.getID().toString());
        configurationService.addPropertyValue("claimable.collection.uuid", claimableCollectionB.getID().toString());

        context.restoreAuthSystemState();

        claimProfileFeature = authorizationFeatureService.find(ClaimItemFeature.NAME);

    }

    @Test
    public void canClaimAProfile() throws Exception {

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionAProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionBProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").exists())
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(publication))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").doesNotExist())
                        .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    @Test
    public void profileNotInClaimableCollection() throws Exception {

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(notClaimableCollectionProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").doesNotExist())
                        .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    @Test
    public void itemAlreadyInARelation() throws Exception {

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("claimable.relation.rightwardType", "isOwnedBy");
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType relationshipType =
            RelationshipTypeBuilder
                .createRelationshipTypeBuilder(context,
                                               person,
                                               person,
                                               "isPersonOwner",
                                               "isOwnedBy",
                                               0,
                                               1000,
                                               0,
                                               1000).build();
        Item ownedItem = ItemBuilder.createItem(context, notClaimableCollection)
            .withCrisOwner("owner", "ownerAuthority").build();

        RelationshipBuilder.createRelationshipBuilder(context, ownedItem, collectionAProfile,
                                                      relationshipType);
        context.restoreAuthSystemState();

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionAProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").doesNotExist())
                        .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    @Test
    public void userHasAlreadyAProfile() throws Exception {

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, claimableCollectionA)
                   .withCrisOwner(eperson.getFullName(), eperson.getID().toString())
                   .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", uri(collectionAProfile))
                                     .param("eperson", context.getCurrentUser().getID().toString())
                                     .param("feature", claimProfileFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded").doesNotExist())
                        .andExpect(jsonPath("$.page.totalElements", equalTo(0)));

    }

    private String uri(Item item) {
        ItemRest itemRest = itemConverter.convert(item, Projection.DEFAULT);
        String itemRestURI = utils.linkToSingleResource(itemRest, "self").getHref();
        return itemRestURI;
    }

}
