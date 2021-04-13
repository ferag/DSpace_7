/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.matcher.RelationshipMatcher.with;
import static org.dspace.app.matcher.RelationshipMatcher.withLeftItem;
import static org.dspace.app.matcher.RelationshipMatcher.withRightItem;
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.builder.EntityTypeBuilder.createEntityTypeBuilder;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link ResearcherProfileRestRepository} specific import into CvPerson scenario
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CvPersonCreationIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ItemService itemService;

    private EntityType personEntityType;

    private EntityType cvPersonEntityType;

    private EntityType cvPersonCloneEntityType;

    private EPerson user;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection personCollection;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    /**
     * Tests setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        personEntityType = createEntityTypeBuilder(context, "Person").build();
        cvPersonEntityType = createEntityTypeBuilder(context, "CvPerson").build();
        cvPersonCloneEntityType = createEntityTypeBuilder(context, "CvPersonClone").build();

        user = EPersonBuilder.createEPerson(context)
            .withEmail("user@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        ctiVitaeCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae Community")
            .build();

        ctiVitaeCloneCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae clone Community")
            .build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("People")
            .withEntityType("Person")
            .withSubmitterGroup(user)
            .build();

        cvCollection = CollectionBuilder.createCollection(context, ctiVitaeCommunity)
            .withName("Profiles")
            .withEntityType("CvPerson")
            .withSubmitterGroup(user)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Profiles")
            .withEntityType("CvPersonClone")
            .build();

        createRelationshipTypeBuilder(context, cvPersonEntityType, personEntityType, "isPersonOwner",
            "isOwnedByCvPerson", 0, null, 0, null).withCopyToLeft(false).withCopyToRight(false).build();

        configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
        configurationService.setProperty("cti-vitae.clone.person-collection-id", cvCloneCollection.getID().toString());
        configurationService.setProperty("researcher-profile.type", "CvPerson");

        context.setCurrentUser(user);

        context.restoreAuthSystemState();

    }

    private RelationshipType createWorkflowRelationshipTypes() throws SQLException {

        EntityType institutionPersonEntityType = createEntityTypeBuilder(context, "InstitutionPerson").build();

        createRelationshipTypeBuilder(context, cvPersonCloneEntityType, personEntityType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).withCopyToLeft(false).withCopyToRight(false).build();

        createRelationshipTypeBuilder(context, cvPersonCloneEntityType, cvPersonEntityType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).withCopyToLeft(false).withCopyToRight(false).build();

        createRelationshipTypeBuilder(context, personEntityType, personEntityType, MERGED.getLeftType(),
            MERGED.getRightType(), 0, 1, 0, 1).withCopyToLeft(false).withCopyToRight(false).build();

        createRelationshipTypeBuilder(context, personEntityType, cvPersonCloneEntityType, ORIGINATED.getLeftType(),
            ORIGINATED.getRightType(), 0, null, 0, 1).withCopyToLeft(false).withCopyToRight(false).build();

        return createRelationshipTypeBuilder(context, institutionPersonEntityType, personEntityType,
            SHADOW_COPY.getLeftType(), SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    /**
     * Given a request containing a DSpace Object URI, verifies that a researcherProfile is created with
     * data cloned from source object's public data.
     *
     * @throws Exception
     */
    @Test
    public void testCloneFromDSpaceSource() throws Exception {

        context.turnOffAuthorisationSystem();

        createWorkflowRelationshipTypes();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withFullName("Giuseppe Verdi")
            .withBirthDate("1813-10-10")
            .withDNI("123123")
            .build();


        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox =
            CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
                .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();


        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("crisrp", "name", Optional.empty()),
            1, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("person", "birthDate", Optional.empty()),
            2, 1)
            .withBox(publicBox).build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("perucris", "identifier", Optional.of("dni")),
            1, 1)
            .withBox(ownerAndAdministratorBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST).content(
                "http://localhost:8080/server/api/core/items/" + person.getID()))
            .andExpect(jsonPath("$.id", is(user.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(
                jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", user.getID()))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.name", "Giuseppe Verdi", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "CvPerson", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1813-10-10", 0)));

        String profileItemId = getItemIdByProfileId(authToken, user.getID().toString());
        assertThat(profileItemId, notNullValue());

        Item profileItem = itemService.find(context, UUID.fromString(profileItemId));
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.isArchived(), is(true));
        assertThat(profileItem.getOwningCollection(), is(cvCollection));

        List<Relationship> profileItemRelations = relationshipService.findByItem(context, profileItem);
        assertThat(profileItemRelations, hasSize(2));
        assertThat(profileItemRelations, hasItem(withRightItem(profileItem, CLONE)));
        assertThat(profileItemRelations, hasItem(with(profileItem, person, "isPersonOwner", "isOwnedByCvPerson")));

        Relationship profileItemCloneRelation = findRelationship(profileItemRelations, CLONE);

        Item profileItemClone = profileItemCloneRelation.getLeftItem();
        assertThat(profileItemClone.isArchived(), is(true));
        assertThat(profileItemClone.getOwningCollection(), is(cvCloneCollection));

        List<Relationship> profileItemCloneRelations = relationshipService.findByItem(context, profileItemClone);
        assertThat(profileItemCloneRelations, hasSize(2));
        assertThat(profileItemCloneRelations, hasItem(with(profileItemClone, profileItem, CLONE)));
        assertThat(profileItemCloneRelations, hasItem(with(profileItemClone, person, SHADOW_COPY)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(user.getName())));
    }

    /**
     * Given a request containing a DSpace Object URI, verifies that a researcherProfile is created with
     * data cloned from source object's public data.
     *
     * @throws Exception
     */
    @Test
    public void testRelationDeletedOnClonedProfile() throws Exception {

        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, personCollection)
            .withFullName("Giuseppe Rossi")
            .withBirthDate("2000-12-10")
            .withDNI("123123")
            .build();


        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox =
            CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
                .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();


        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("crisrp", "name", Optional.empty()),
            1, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("person", "birthDate", Optional.empty()),
            2, 1)
            .withBox(publicBox).build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("perucris", "identifier", Optional.of("dni")),
            1, 1)
            .withBox(ownerAndAdministratorBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST).content(
                "http://localhost:8080/server/api/core/items/" + person.getID()))
            .andExpect(jsonPath("$.id", is(user.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(
                jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        String profileItemId = getItemIdByProfileId(authToken, user.getID().toString());

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", user.getID()))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", user.getID()))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/" + profileItemId + "/relationships"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalPages", is(0)));

    }

    /**
     * Given a request containing an eperson Id, a DSpace Object URI, performed by an admin,
     * verifies that a researcherProfile is created with
     * data cloned from source object's public data.
     *
     * @throws Exception
     */
    @Test
    public void testCloneFromDSpaceSourceByAdmin() throws Exception {

        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, personCollection)
            .withFullName("Mario Rossi")
            .withBirthDate("1982-12-17")
            .withDNI("123555")
            .build();


        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox =
            CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
                .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();


        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("crisrp", "name", Optional.empty()),
            1, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("person", "birthDate", Optional.empty()),
            2, 1)
            .withBox(publicBox).build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("perucris", "identifier", Optional.of("dni")),
            1, 1)
            .withBox(ownerAndAdministratorBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        UUID userID = user.getID();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", userID.toString())
            .contentType(TEXT_URI_LIST).content(
                "http://localhost:8080/server/api/core/items/" + person.getID()))
            .andExpect(jsonPath("$.id", is(userID.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(
                jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + userID, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", userID))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", userID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), userID.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.name", "Mario Rossi", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "CvPerson", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1982-12-17", 0)));

        String profileItemId = getItemIdByProfileId(authToken, userID.toString());

        getClient(authToken).perform(get("/api/core/items/" + profileItemId + "/relationships"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.relationships[0]._links.leftItem.href",
                is("http://localhost/api/core/items/" + profileItemId)))
            .andExpect(jsonPath("$._embedded.relationships[0]_links.rightItem.href",
                is("http://localhost/api/core/items/" + person.getID())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", userID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(user.getName())));
    }

    /**
     * Given a request containing a DSpace Object URI, verifies that a
     * researcherProfile is created with data cloned from source object's public
     * data.
     *
     * @throws Exception
     */
    @Test
    public void testCloneFromDSpaceSourceWithInstitutionPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        RelationshipType shadowCopyRelationshipType = createWorkflowRelationshipTypes();

        Collection institutionPersonCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("People")
            .withEntityType("InstitutionPerson")
            .withSubmitterGroup(user)
            .build();

        Item institutionPerson = ItemBuilder.createItem(context, institutionPersonCollection)
            .withFullName("Giuseppe Verdi")
            .withBirthDate("1813-10-10")
            .withDNI("123123")
            .build();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withFullName("Giuseppe Verdi")
            .withBirthDate("1813-10-10")
            .withDNI("123123")
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, institutionPerson, person, shadowCopyRelationshipType);

        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, personEntityType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox = CrisLayoutBoxBuilder
            .createBuilder(context, personEntityType, false, false)
            .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();

        CrisLayoutFieldBuilder.createMetadataField(context, metadataField("crisrp", "name", Optional.empty()), 1, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder
            .createMetadataField(context, metadataField("person", "birthDate", Optional.empty()), 2, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder
            .createMetadataField(context, metadataField("perucris", "identifier", Optional.of("dni")), 1, 1)
            .withBox(ownerAndAdministratorBox)
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST).content("http://localhost:8080/server/api/core/items/" + person.getID()))
            .andExpect(jsonPath("$.id", is(user.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(
                jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", user.getID()))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.name", "Giuseppe Verdi", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "CvPerson", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1813-10-10", 0)));

        String profileItemId = getItemIdByProfileId(authToken, user.getID().toString());
        assertThat(profileItemId, notNullValue());

        Item profileItem = itemService.find(context, UUID.fromString(profileItemId));
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.isArchived(), is(true));
        assertThat(profileItem.getOwningCollection(), is(cvCollection));

        List<Relationship> profileItemRelations = relationshipService.findByItem(context, profileItem);
        assertThat(profileItemRelations, hasSize(2));
        assertThat(profileItemRelations, hasItem(withRightItem(profileItem, CLONE)));
        assertThat(profileItemRelations, hasItem(with(profileItem, person, "isPersonOwner", "isOwnedByCvPerson")));

        Relationship profileItemCloneRelation = findRelationship(profileItemRelations, CLONE);

        Item profileItemClone = profileItemCloneRelation.getLeftItem();
        assertThat(profileItemClone.isArchived(), is(true));
        assertThat(profileItemClone.getOwningCollection(), is(cvCloneCollection));

        List<Relationship> profileItemCloneRelations = relationshipService.findByItem(context, profileItemClone);
        assertThat(profileItemCloneRelations, hasSize(3));
        assertThat(profileItemCloneRelations, hasItem(with(profileItemClone, profileItem, CLONE)));
        assertThat(profileItemCloneRelations, hasItem(with(person, profileItemClone, ORIGINATED)));
        assertThat(profileItemCloneRelations, hasItem(withLeftItem(profileItemClone, SHADOW_COPY)));

        Relationship profileItemCloneCopyRelation = findRelationship(profileItemCloneRelations, SHADOW_COPY);

        Item profileCloneCopy = profileItemCloneCopyRelation.getRightItem();
        assertThat(profileCloneCopy.isArchived(), is(false));
        assertThat(profileCloneCopy.isWithdrawn(), is(true));
        assertThat(profileCloneCopy.getOwningCollection(), is(personCollection));

        List<Relationship> profileCloneCopyRelations = relationshipService.findByItem(context, profileCloneCopy);
        assertThat(profileCloneCopyRelations, hasSize(2));
        assertThat(profileCloneCopyRelations, hasItem(with(profileItemClone, profileCloneCopy, SHADOW_COPY)));
        assertThat(profileCloneCopyRelations, hasItem(with(profileCloneCopy, person, MERGED)));

        List<Relationship> personRelations = relationshipService.findByItem(context, person);
        assertThat(personRelations, hasSize(4));
        assertThat(personRelations, hasItem(with(profileItem, person, "isPersonOwner", "isOwnedByCvPerson")));
        assertThat(personRelations, hasItem(with(profileCloneCopy, person, MERGED)));
        assertThat(personRelations, hasItem(with(person, profileItemClone, ORIGINATED)));
        assertThat(personRelations, hasItem(with(institutionPerson, person, SHADOW_COPY)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(user.getName())));
    }

    private Relationship findRelationship(List<Relationship> profileItemRelations, ConcytecWorkflowRelation relation) {
        Relationship relationship = profileItemRelations.stream()
            .filter(r -> r.getRelationshipType().getLeftwardType().equals(relation.getLeftType()))
            .findFirst().orElse(null);
        assertThat(relationship, notNullValue());
        return relationship;
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }

    private MetadataField metadataField(String schema, String element, Optional<String> qualifier)
        throws SQLException {

        MetadataSchema metadataSchema = metadataSchemaService.find(context, schema);

        return metadataFieldService.findByElement(context,
            metadataSchema,
            element,
            qualifier.orElse(null));
    }
}
