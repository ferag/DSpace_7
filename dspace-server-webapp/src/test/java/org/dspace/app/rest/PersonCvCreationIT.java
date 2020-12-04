/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link ResearcherProfileRestRepository} specific import into PersonCv scenario
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class PersonCvCreationIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    private EntityType personEntityType;
    private EntityType personCvEntityType;

    private EPerson user;

    private Collection personCollection;
    private Collection cvCollection;

    /**
     * Tests setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        personCvEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "PersonCv").build();

        user = EPersonBuilder.createEPerson(context)
            .withEmail("user@example.com")
            .withPassword(password)
            .build();


        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae Community")
            .build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("People")
            .withRelationshipType("Person")
            .withSubmitterGroup(user)
            .build();

        cvCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Profiles")
            .withRelationshipType("PersonCv")
            .withSubmitterGroup(user)
            .build();


        RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, personCvEntityType, personEntityType, "isPersonOwner",
                "isOwnedByPersonCv", 0, null, 0,
                null).withCopyToLeft(false).withCopyToRight(false).build();

        configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
        configurationService.setProperty("researcher-profile.type", "PersonCv");

        context.setCurrentUser(user);

        context.restoreAuthSystemState();

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
        Item person = ItemBuilder.createItem(context, personCollection)
            .withFullName("Giuseppe Verdi")
            .withRelationshipType("Person")
            .withBirthDate("1813-10-10")
            .withDNI("123123").build();


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
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "PersonCv", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1813-10-10", 0)));

        String profileItemId = getItemIdByProfileId(authToken, user.getID().toString());

        getClient(authToken).perform(get("/api/core/items/" + profileItemId + "/relationships"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.relationships[0]._links.leftItem.href",
                is("http://localhost/api/core/items/" + profileItemId)))
            .andExpect(jsonPath("$._embedded.relationships[0]_links.rightItem.href",
                is("http://localhost/api/core/items/" + person.getID())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(user.getName())));
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
            .withRelationshipType("Person")
            .withBirthDate("1982-12-17")
            .withDNI("123555").build();


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
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "PersonCv", 0)))
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
