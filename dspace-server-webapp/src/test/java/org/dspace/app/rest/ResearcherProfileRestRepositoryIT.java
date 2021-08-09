/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.profile.OrcidEntitySyncPreference.ALL;
import static org.dspace.app.profile.OrcidEntitySyncPreference.MINE;
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.dspace.builder.RelationshipBuilder.createRelationshipBuilder;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.jayway.jsonpath.JsonPath;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.orcid.webhook.OrcidWebhookServiceImpl;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.ResearcherProfileRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.importer.external.ctidb.CtiDatabaseDao;
import org.dspace.importer.external.ctidb.CtiDatabaseImportFacadeImpl;
import org.dspace.importer.external.ctidb.model.CtiDatosConfidenciales;
import org.dspace.importer.external.ctidb.model.CtiDatosLaborales;
import org.dspace.importer.external.ctidb.model.CtiDerechosPi;
import org.dspace.importer.external.ctidb.model.CtiFormacionAcademica;
import org.dspace.importer.external.ctidb.model.CtiInvestigador;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;
import org.dspace.importer.external.ctidb.model.CtiProyecto;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;



/**
 * Integration tests for {@link ResearcherProfileRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ResearcherProfileRestRepositoryIT extends AbstractControllerIntegrationTest {

    public static final String DNI_TEST = "01234567";
    public static final Integer INVESTIGADOR_ID_TEST = 1;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Mock
    private CtiDatabaseDao ctiDatabaseDaoMock;

    @Autowired
    private CtiDatabaseImportFacadeImpl ctiDatabaseImportFacade;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private OrcidWebhookServiceImpl orcidWebhookService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    private EPerson user;

    private EPerson anotherUser;

    private Collection cvPersonCollection;

    private Collection personCollection;

    private Group administrators;

    /**
     * Tests setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        user = EPersonBuilder.createEPerson(context).withEmail("user@example.com").withPassword(password).build();

        anotherUser = EPersonBuilder.createEPerson(context).withEmail("anotherUser@example.com").withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        cvPersonCollection = CollectionBuilder.createCollection(context, parentCommunity).withName("Profile Collection")
                .withEntityType("CvPerson")
                .withSubmitterGroup(user)
                .withTemplateItem().build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Profile Collection")
            .withEntityType("Person")
            .withSubmitterGroup(user)
            .build();

        configurationService.setProperty("researcher-profile.collection.uuid", cvPersonCollection.getID().toString());

        administrators = groupService.findByName(context, Group.ADMIN);

        itemService.addMetadata(context, cvPersonCollection.getTemplateItem(), "cris", "policy",
                                "group", null, administrators.getName());

        configurationService.setProperty("researcher-profile.collection.uuid", cvPersonCollection.getID().toString());
        configurationService.setProperty("claimable.entityType", "Person");

        context.setCurrentUser(user);

        context.restoreAuthSystemState();

    }

    @After
    public void after() throws SQLException, AuthorizeException {
        List<OrcidQueue> records = orcidQueueService.findAll(context);
        for (OrcidQueue record : records) {
            orcidQueueService.delete(context, record);
        }
    }

    /**
     * Verify that the findById endpoint returns the own profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindById() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(user.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection).withCrisOwner(name, id.toString()).build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString()))).andExpect(jsonPath("$.visible", is(true)))
                .andExpect(jsonPath("$.type", is("profile"))).andExpect(jsonPath("$.orcid").doesNotExist())
                .andExpect(jsonPath("$.orcidSynchronization").doesNotExist())
                .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that the an admin user can call the findById endpoint to get a
     * profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithAdmin() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection).withCrisOwner(name, id.toString()).build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString()))).andExpect(jsonPath("$.visible", is(true)))
                .andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that a standard user can't access the profile of another user.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithoutOwnerUser() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection).withCrisOwner(name, id.toString()).build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id)).andExpect(status().isForbidden());

    }

    /**
     * Verify that the createAndReturn endpoint create a new researcher profile.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturn() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
                                            .andExpect(jsonPath("$.metadata", matchMetadata("cris.policy.group", administrators.getName(),
                                                                            UUIDUtils.toString(administrators.getID()), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(name)));
    }

    /**
     * Verify that an admin can call the createAndReturn endpoint to store a new
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithAdmin() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        configurationService.setProperty("researcher-profile.collection.uuid", null);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("cris.policy.group", administrators.getName(),
                                                                            UUIDUtils.toString(administrators.getID()), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(name)));

        authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString()))).andExpect(jsonPath("$.visible", is(false)))
                .andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));
    }

    /**
     * Verify that a standard user can't call the createAndReturn endpoint to store
     * a new researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithoutOwnUser() throws Exception {

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").param("eperson", user.getID().toString())
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isForbidden());

    }

    /**
     * Verify that a conflict occurs if an user that have already a profile call the
     * createAndReturn endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

    }

    /**
     * Verify that an unprocessable entity status is back when the createAndReturn
     * is called to create a profile for an unknown user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithUnknownEPerson() throws Exception {

        String unknownId = UUID.randomUUID().toString();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                post("/api/cris/profiles/").param("eperson", unknownId).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Verify that a user can delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
                .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get())).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataDoesNotExist("cris.owner"))));

    }

    /**
     * Verify that a user can hard delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testHardDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", true);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
                .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get())).andExpect(status().isNotFound());

    }

    /**
     * Verify that an admin can delete a profile of another user using the delete
     * endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());
    }

    /**
     * Verify that an user can delete his profile using the delete endpoint even if
     * was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(userToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isNoContent());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

    }

    /**
     * Verify that a standard user can't call the delete endpoint to delete a
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        getClient(anotherUserToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

    }

    /**
     * Verify that an user can change the profile visibility using the patch
     * endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttribute() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));

        // change the visibility to false
        operations = asList(new ReplaceOperation("/visible", false));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

    }

    /**
     * Verify that an user can not change the profile visibility of another user
     * using the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.visible", is(false)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        // try to change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(anotherUserToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));
    }

    /**
     * Verify that an admin can change the profile visibility of another user using
     * the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(adminToken)
                .perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that an user can change the visibility of his profile using the patch
     * endpoint even if was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibilityOfProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(userToken)
                .perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that after an user login an automatic claim between the logged eperson
     * and possible profiles without eperson is done.
     *
     * @throws Exception
     */
    @Test
    public void testAutomaticProfileClaimByEmail() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // create and delete a profile
        getClient(adminToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        String firstItemId = getItemIdByProfileId(adminToken, id);

        MetadataValueRest valueToAdd = new MetadataValueRest(user.getEmail());
        List<Operation> operations = asList(new AddOperation("/metadata/person.email", valueToAdd));

        getClient(adminToken)
                .perform(patch(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstItemId)
                        .contentType(MediaType.APPLICATION_JSON).content(getPatchContent(operations)))
                .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id)).andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isNotFound());

        // the automatic claim is done after the user login
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id)).andExpect(status().isOk());

        // the profile item should be the same
        String secondItemId = getItemIdByProfileId(adminToken, id);
        assertEquals("The item should be the same", firstItemId, secondItemId);

    }

    /**
     * Given a request containing an external reference URI, verifies that a
     * researcherProfile is created with data cloned from source object.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testCloneFromExternalSource() throws Exception {
        // FIXME: unIgnore once orcid integration ready and merged

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, cvPersonCollection).withFullName("Giuseppe Garibaldi")
                .withBirthDate("1807-07-04").withOrcidIdentifier("0000-1111-2222-3333").build();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();

        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();

        CrisLayoutFieldBuilder.createMetadataField(context, metadataField("crisrp", "name", Optional.empty()), 1, 1)
                .withBox(publicBox).build();

        CrisLayoutFieldBuilder
                .createMetadataField(context, metadataField("person", "birthDate", Optional.empty()), 2, 1)
                .withBox(publicBox).build();

        CrisLayoutFieldBuilder
                .createMetadataField(context, metadataField("perucris", "identifier", Optional.of("dni")), 1, 1)
                .withBox(ownerAndAdministratorBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST).content(
                "http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/0000-1111-2222-3333"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(user.getID().toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$",
                        matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", user.getID())).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", user.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(
                        jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.name", "Giuseppe Garibaldi", 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)))
                .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1807-07-04", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", user.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(user.getName())));
    }

//    @Test
//    public void testCloneFromExternalSourceRecordNotFound() throws Exception {
//
//        String authToken = getAuthToken(user.getEmail(), password);
//
//        getClient(authToken)
//                .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
//                        .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/FAKE"))
//                .andExpect(status().isBadRequest());
//    }

//    @Test
//    public void testCloneFromExternalSourceMultipleUri() throws Exception {
//
//        String authToken = getAuthToken(user.getEmail(), password);
//
//        getClient(authToken)
//                .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
//                        .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n "
//                                + "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
//                .andExpect(status().isBadRequest());
//
//    }

//    @Test
//    public void testCloneFromExternalProfileAlreadyAssociated() throws Exception {
//
//        String id = user.getID().toString();
//        String authToken = getAuthToken(user.getEmail(), password);
//
//        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
//                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
//                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));
//
//        getClient(authToken)
//                .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
//                        .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id"))
//                .andExpect(status().isConflict());
//    }

//    @Test
//    public void testCloneFromExternalCollectionNotSet() throws Exception {
//
//        configurationService.setProperty("researcher-profile.collection.uuid", "not-existing");
//        String id = user.getID().toString();
//        String authToken = getAuthToken(user.getEmail(), password);
//
//        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
//                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
//                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));
//
//        getClient(authToken)
//                .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
//                        .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n "
//                                + "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
//                .andExpect(status().isBadRequest());
//    }

    @Test
    public void testClaimExistentDirectorioPersonAsProfileAndMergeItWithCtiDatabase() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson userWithDni = EPersonBuilder.createEPerson(context).withEmail("userWithDni@example.com")
                .withPassword(password).withDni(DNI_TEST).build();

        context.setCurrentUser(userWithDni);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Cv Person Clone Collection").withEntityType("CvPersonClone").build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person Collection").withEntityType("Person").build();

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
                cvPersonCloneCollection.getID().toString());

        Item person = ItemBuilder.createItem(context, personCollection).withFullName("Giuseppe Garibaldi")
                .withBirthDate("1807-07-04").build();

        EntityType personType = createEntityType("Person");
        EntityType institutionPersonType = createEntityType("InstitutionPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        createHasShadowCopyRelationship(institutionPersonType, personType);
        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);
        createIsOriginatedFromRelationship(personType, cvPersonCloneType);
        createIsMergedInRelationship(personType);
        createIsPersonOwnerRelationship(cvPersonType, personType);

        mockCtiDatabaseDao(ctiDatabaseDaoMock);

        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, personType, false, false)
                .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutFieldBuilder.createMetadataField(context, metadataField("crisrp", "name", Optional.empty()), 1, 1)
                .withBox(publicBox).build();

        CrisLayoutFieldBuilder
                .createMetadataField(context, metadataField("person", "birthDate", Optional.empty()), 2, 1)
                .withBox(publicBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(userWithDni.getEmail(), password);

        String claimedURI = "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/"
                + person.getID().toString();

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST).content(claimedURI))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(userWithDni.getID().toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$",
                        matchLinks("http://localhost/api/cris/profiles/" + userWithDni.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", userWithDni.getID())).andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", userWithDni.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("item")))
                .andExpect(mergeDirectorioCtiResultMatcher(userWithDni));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", userWithDni.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("eperson"))).andExpect(jsonPath("$.name", is(userWithDni.getName())));
    }

    @Test
    public void testPatchToClaimPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        RelationshipType cvShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        RelationshipType isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        RelationshipType isPersonOwner = createIsPersonOwnerRelationship(cvPersonType, personType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Cv Person Clone Collection").withEntityType("CvPersonClone").build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person Collection").withEntityType("Person").build();

        Item person = ItemBuilder.createItem(context, personCollection).withTitle("User").build();

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
                cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        List<Relationship> cvShadowCopyRelations = findRelations(person, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));

        Item cvPersonCloneItem = cvShadowCopyRelations.get(0).getLeftItem();
        assertThat(cvPersonCloneItem.getOwningCollection(), equalTo(cvPersonCloneCollection));
        assertThat(cvPersonCloneItem.getMetadata(), hasItem(with("dc.title", "user@example.com")));

        List<Relationship> cloneRelations = findRelations(cvPersonCloneItem, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        UUID profileId = cloneRelations.get(0).getRightItem().getID();

        getClient(userToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(profileId.toString())));

        List<Relationship> isPersonOwnerRelations = findRelations(person, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getLeftItem().getID(), equalTo(profileId));

    }

    @Test
    public void testPatchToClaimWithAlreadyClonedPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Cv Person Clone Collection").withEntityType("CvPersonClone").build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person Collection").withEntityType("Person").build();

        Item person = ItemBuilder.createItem(context, personCollection).withTitle("User").build();

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
                cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isConflict());

    }

    @Test
    public void testPatchToClaimEntityWithWrongType() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Publication Collection").withEntityType("Publication").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection).withTitle("Publication").build();

        configurationService.setProperty("claimable.entityType", "Person");

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", publication.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());

    }

    @Test
    public void testPatchToClaimPersonWithInvalidItemId() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", "wrong-id"));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToClaimPersonWithUnkownItemId() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", "873510e9-6d3f-4b1d-add0-bb1d7d53f07f"));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToClaimPersonRelatedToInstitutionPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType institutionPersonType = createEntityType("InstitutionPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        RelationshipType institutionShadowCopy = createHasShadowCopyRelationship(institutionPersonType, personType);
        RelationshipType cvShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        RelationshipType isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        RelationshipType isOriginatedFrom = createIsOriginatedFromRelationship(personType, cvPersonCloneType);
        RelationshipType isMergedIn = createIsMergedInRelationship(personType);
        RelationshipType isPersonOwner = createIsPersonOwnerRelationship(cvPersonType, personType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Cv Person Clone Collection").withEntityType("CvPersonClone").build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person Collection").withEntityType("Person").build();

        Collection institutionPersonCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Institution Person Collection").withEntityType("InstitutionPerson").build();

        Item person = ItemBuilder.createItem(context, personCollection).withTitle("User").build();

        Item institutionPerson = ItemBuilder.createItem(context, institutionPersonCollection).withTitle("User").build();

        createRelationshipBuilder(context, institutionPerson, person, institutionShadowCopy);

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
                cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken)
                .perform(post("/api/cris/profiles/").param("eperson", id).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        assertThat(findRelations(person, cvShadowCopy), empty());

        List<Relationship> isMergedInRelations = findRelations(person, isMergedIn);
        assertThat(isMergedInRelations, hasSize(1));
        Item mergedInItem = isMergedInRelations.get(0).getLeftItem();
        assertThat(mergedInItem.isArchived(), is(false));
        assertThat(mergedInItem.isWithdrawn(), is(true));

        List<Relationship> isOriginatedFromRelations = findRelations(person, isOriginatedFrom);
        assertThat(isOriginatedFromRelations, hasSize(1));

        Item cvPersonCloneItem = isOriginatedFromRelations.get(0).getRightItem();
        assertThat(cvPersonCloneItem.getOwningCollection(), equalTo(cvPersonCloneCollection));
        assertThat(cvPersonCloneItem.getMetadata(), hasItem(with("dc.title", "user@example.com")));

        List<Relationship> cvShadowCopyRelations = findRelations(mergedInItem, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));
        assertThat(cvShadowCopyRelations.get(0).getLeftItem(), equalTo(cvPersonCloneItem));

        List<Relationship> cloneRelations = findRelations(cvPersonCloneItem, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        UUID profileId = cloneRelations.get(0).getRightItem().getID();

        getClient(userToken).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(profileId.toString())));

        List<Relationship> isPersonOwnerRelations = findRelations(person, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getLeftItem().getID(), equalTo(profileId));

    }

    @Test
    public void researcherProfileSecurityAnonymousTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));

        getClient().perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient().perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient().perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

        // hide the profile and linked CVs
        List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations2))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient().perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isUnauthorized());

    }

    @Test
    public void researcherProfileSecuritySimpleLoggedUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));

        getClient(epersonToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient(epersonToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient(epersonToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

        // hide the profile and linked CVs
        List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations2))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(epersonToken).perform(get("/api/core/items/" + CvPublication.getID()))
                .andExpect(status().isForbidden());

        getClient(epersonToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isForbidden());

        getClient(epersonToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isForbidden());
    }

    @Test
    public void researcherProfileSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

        // hide the profile and linked CVs
        List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations2))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

    }

    @Test
    public void researcherProfileSecurityAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(true)));

        getClient(adminToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient(adminToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient(adminToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

        // hide the profile and linked CVs
        List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations2))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(adminToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

        getClient(adminToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

        getClient(adminToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

    }

    @Test
    public void cvOwnerAuthorizedToSeeNotVisibleDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPublication,
                        "CvPublication Title", "2021-01-01")));

        getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        ItemMatcher.matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

        getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        ItemMatcher.matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String researcherToken = getAuthToken(researcher.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPublication.getID()))
                .andExpect(status().isForbidden());

        getClient(tokenEperson).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isForbidden());

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isForbidden());
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient().perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isUnauthorized());

        getClient().perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isUnauthorized());
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));

        getClient(tokenAdmin).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPublication,
                        "CvPublication Title", "2021-01-01")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk()).andExpect(
                jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk()).andExpect(
                jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileVisibleLoggedUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String researcherToken = getAuthToken(researcher.getEmail(), password);

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPublication,
                        "CvPublication Title", "2021-01-01")));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        ItemMatcher.matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        ItemMatcher.matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileVisibleAnonymousUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        EPerson researcher = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").withPassword(password).build();

        ResearcherProfile researcherProfile = createProfileForUser(researcher);

        Item CvPublication = ItemBuilder.createItem(context, col1).withTitle("CvPublication Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
                .withEntityType("CvPublication").withIssueDate("2021-01-01").build();

        Item CvProject = ItemBuilder.createItem(context, col1).withTitle("CvProject Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withEntityType("CvProject")
                .withIssueDate("2021-02-07").build();

        Item CvPatent = ItemBuilder.createItem(context, col1).withTitle("CvPatent Title")
                .withCrisOwner(researcher.getName(), researcherProfile.getId().toString()).withIssueDate("2020-10-11")
                .withEntityType("CvPatent").build();

        context.restoreAuthSystemState();

        String researcherToken = getAuthToken(researcher.getEmail(), password);

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(researcherToken)
                .perform(patch("/api/cris/profiles/" + researcher.getID()).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visible", is(true)));

        getClient().perform(get("/api/core/items/" + CvPublication.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPublication,
                        "CvPublication Title", "2021-01-01")));

        getClient().perform(get("/api/core/items/" + CvProject.getID())).andExpect(status().isOk()).andExpect(
                jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

        getClient().perform(get("/api/core/items/" + CvPatent.getID())).andExpect(status().isOk()).andExpect(
                jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    /**
     * Verify that after an user login an automatic claim between the logged eperson
     * and possible profiles without eperson is done.
     *
     * @throws Exception
     */
    @Test
    public void testAutomaticProfileClaimByOrcid() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withNameInMetadata("Test", "User")
                .withPassword(password).withEmail("test@email.it").withOrcid("0000-1111-2222-3333").build();

        Item item = ItemBuilder.createItem(context, cvPersonCollection).withTitle("Test User")
                .withOrcidIdentifier("0000-1111-2222-3333").build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId)).andExpect(status().isOk());

        String profileItemId = getItemIdByProfileId(token, epersonId);
        assertEquals("The item should be the same", item.getID().toString(), profileItemId);

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfManyClaimableItemsAreFound() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withNameInMetadata("Test", "User")
                .withPassword(password).withEmail("test@email.it").withOrcid("0000-1111-2222-3333").build();

        ItemBuilder.createItem(context, cvPersonCollection).withTitle("Test User")
                .withOrcidIdentifier("0000-1111-2222-3333").build();

        ItemBuilder.createItem(context, cvPersonCollection).withTitle("Test User 2")
                .withOrcidIdentifier("0000-1111-2222-3333").build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        getClient(getAuthToken(ePerson.getEmail(), password)).perform(get("/api/cris/profiles/{id}", epersonId))
                .andExpect(status().isNotFound());

    }

    @Test
    @Ignore
    public void testNoAutomaticProfileClaimOccursIfTheUserHasAlreadyAProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withNameInMetadata("Test", "User")
                .withPassword(password).withEmail("test@email.it").withOrcid("0000-1111-2222-3333").build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId)).andExpect(status().isOk());

        String profileItemId = getItemIdByProfileId(token, epersonId);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection).withTitle("Test User")
                .withOrcidIdentifier("0000-1111-2222-3333").build();

        context.restoreAuthSystemState();

        token = getAuthToken(ePerson.getEmail(), password);

        String newProfileItemId = getItemIdByProfileId(token, epersonId);
        assertEquals("The item should be the same", newProfileItemId, profileItemId);

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfTheFoundProfileIsAlreadyClaimed() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withNameInMetadata("Test", "User")
                .withPassword(password).withEmail("test@email.it").build();

        ItemBuilder.createItem(context, cvPersonCollection).withTitle("Admin User").withPersonEmail("test@email.it")
                .withCrisOwner("Admin User", admin.getID().toString()).build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId)).andExpect(status().isNotFound());

    }

    @Test
    public void testOrcidMetadataOfEpersonAreCopiedOnProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(ePersonId.toString())))
                .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcid", is("0000-1111-2222-3333")))
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")))
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is("DISABLED")))
            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is("DISABLED")))
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences", empty()));

        String itemId = getItemIdByProfileId(authToken, ePersonId);

        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));
        assertThat(profileItem, notNullValue());

        List<MetadataValue> metadata = profileItem.getMetadata();
        assertThat(metadata, hasItem(with("person.identifier.orcid", "0000-1111-2222-3333")));
        assertThat(metadata, hasItem(with("cris.orcid.access-token", "af097328-ac1c-4a3e-9eb4-069897874910")));
        assertThat(metadata, hasItem(with("cris.orcid.refresh-token", "32aadae0-829e-49c5-824f-ccaf4d1913e4")));
        assertThat(metadata, hasItem(with("cris.orcid.scope", "/first-scope", 0)));
        assertThat(metadata, hasItem(with("cris.orcid.scope", "/second-scope", 1)));

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForPublications() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/publications", ALL.name()));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/publications", MINE.name()));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(MINE.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(MINE.name())));

        operations = asList(new ReplaceOperation("/orcid/publications", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());

    }

    @Test
    @Ignore("functionality temporarily disabled")
    public void testPatchToSetOrcidSynchronizationPreferenceForFundings() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/fundings", ALL.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/fundings", MINE.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is(MINE.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is(MINE.name())));

        operations = asList(new ReplaceOperation("/orcid/fundings", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/profile", "AFFILIATION, EDUCATION"));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                        containsInAnyOrder("AFFILIATION", "EDUCATION")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk()).andExpect(
                jsonPath("$.orcidSynchronization.profilePreferences", containsInAnyOrder("AFFILIATION", "EDUCATION")));

        operations = asList(new ReplaceOperation("/orcid/profile", "IDENTIFIERS"));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.profilePreferences", containsInAnyOrder("IDENTIFIERS")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.profilePreferences", containsInAnyOrder("IDENTIFIERS")));

        operations = asList(new ReplaceOperation("/orcid/profiles", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationMode() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        operations = asList(new ReplaceOperation("/orcid/mode", "MANUAL"));

        getClient(authToken)
                .perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        operations = asList(new ReplaceOperation("/orcid/mode", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithWrongPath() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User")
                .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
                .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4").withOrcidScope("/first-scope")
                .withOrcidScope("/second-scope").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/wrong-path", "BATCH"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithProfileNotLinkedToOrcid() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withCanLogin(true).withOrcid("0000-1111-2222-3333")
                .withEmail("test@email.it").withPassword(password).withNameInMetadata("Test", "User").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId).content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    public void updateCvLinkedEntitiesSolrDocumentsAfterClaimTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context).withNameInMetadata("Viktor", "Bruni")
                .withEmail("viktor.bruni@test.com").withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection cvCollection = CollectionBuilder.createCollection(context, parentCommunity).withName("Profiles")
                .withEntityType("CvPerson").build();

        Collection cvCloneCollection = CollectionBuilder.createCollection(context, parentCommunity).withName("Profiles")
                .withEntityType("CvPersonClone").withWorkflow("institutionWorkflow").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Person")
                .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        Item personItem = ItemBuilder.createItem(context, col1).withTitle("Person Item Title")
                .withPersonEducation("High school").withPersonEducationStartDate("1968-09-01")
                .withPersonEducationEndDate("1973-06-10").withEntityType("Person").build();

        Item publicationItem = ItemBuilder.createItem(context, col2)
                .withAuthor(personItem.getName(), personItem.getID().toString()).withTitle("Publication Item Title")
                .withEntityType("Publication").build();

        Item projectItem = ItemBuilder.createItem(context, col2)
                .withProjectInvestigator(personItem.getName(), personItem.getID().toString())
                .withTitle("Project Item Title").withEntityType("Project").build();

        Item patentItem = ItemBuilder.createItem(context, col2)
                .withAuthor(personItem.getName(), personItem.getID().toString()).withTitle("Patent Item Title")
                .withEntityType("Patent").build();

        context.commit();
        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {

            configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
            configurationService.setProperty("cti-vitae.clone.person-collection-id",
                    cvCloneCollection.getID().toString());
            context.restoreAuthSystemState();

            String tokenUser = getAuthToken(user.getEmail(), password);

            getClient(tokenUser)
                    .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                            .content("http://localhost:8080/server/api/core/items/" + personItem.getID()))
                    .andExpect(status().isCreated()).andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            getClient(tokenUser).perform(get("/api/cris/profiles/{id}/item", idRef.get())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata['dc.title'][0].value", is(user.getFullName())))
                    .andExpect(jsonPath("$.metadata['cris.owner'][0].value", is(user.getEmail())))
                    .andExpect(jsonPath("$.metadata['cris.owner'][0].authority", is(user.getID().toString())))
                    .andExpect(jsonPath("$.metadata['dspace.entity.type'][0].value", is("CvPerson")))
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            assertSearchQuery(publicationItem, user.getFullName(), idRef.get().toString());
            assertSearchQuery(projectItem, user.getFullName(), idRef.get().toString());
            assertSearchQuery(patentItem, user.getFullName(), idRef.get().toString());
        } finally {
            ItemBuilder.deleteItem(idRef.get());
        }
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "disabled");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", null);

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        EPerson anotherUser = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withEmail("user@email.it")
            .withPassword(password)
            .withNameInMetadata("Another", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        assertThat(context.reloadEntity(firstQueueRecord), nullValue());
        assertThat(context.reloadEntity(secondQueueRecord), nullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), empty());
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        assertThat(context.reloadEntity(firstQueueRecord), nullValue());
        assertThat(context.reloadEntity(secondQueueRecord), nullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), empty());
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        assertThat(context.reloadEntity(firstQueueRecord), nullValue());
        assertThat(context.reloadEntity(secondQueueRecord), nullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), empty());
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        assertThat(context.reloadEntity(firstQueueRecord), nullValue());
        assertThat(context.reloadEntity(secondQueueRecord), nullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), empty());
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        assertThat(context.reloadEntity(firstQueueRecord), notNullValue());
        assertThat(context.reloadEntity(secondQueueRecord), notNullValue());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.access-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "cris.orcid.authenticated"), not(empty()));
    }

    @Test
    public void testProfileDisconnectionFromOrcidCauseOrcidWebhookUnregistration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_owner");

        context.turnOffAuthorisationSystem();

        String orcid = "0000-1111-2222-3333";

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid(orcid)
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        Item profile = createProfile(ePerson);

        addMetadata(profile, "cris", "orcid", "webhook", "2020-02-02");

        OrcidQueue firstQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();
        OrcidQueue secondQueueRecord = OrcidQueueBuilder.createOrcidQueue(context, profile, profile).build();

        context.restoreAuthSystemState();

        OrcidClient orcidClient = orcidWebhookService.getOrcidClient();
        OrcidClient orcidClientMock = mock(OrcidClient.class);

        String webhookAccessToken = "603315a5-cf2e-40ad-934a-24357a890bf9";
        when(orcidClientMock.getWebhookAccessToken()).thenReturn(buildTokenResponse(webhookAccessToken));

        try {

            orcidWebhookService.setOrcidClient(orcidClientMock);

            getClient(getAuthToken(ePerson.getEmail(), password))
                .perform(patch("/api/cris/profiles/{id}", ePerson.getID().toString())
                    .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
                .andExpect(jsonPath("$.visible", is(false)))
                .andExpect(jsonPath("$.type", is("profile")))
                .andExpect(jsonPath("$.orcid").doesNotExist())
                .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

            assertThat(context.reloadEntity(firstQueueRecord), nullValue());
            assertThat(context.reloadEntity(secondQueueRecord), nullValue());

            profile = context.reloadEntity(profile);

            assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
            assertThat(getMetadataValues(profile, "cris.orcid.access-token"), empty());
            assertThat(getMetadataValues(profile, "cris.orcid.refresh-token"), empty());
            assertThat(getMetadataValues(profile, "cris.orcid.scope"), empty());
            assertThat(getMetadataValues(profile, "cris.orcid.webhook"), empty());

            verify(orcidClientMock).getWebhookAccessToken();
            verify(orcidClientMock).unregisterWebhook(eq(webhookAccessToken), eq(orcid), any());
            verifyNoMoreInteractions(orcidClientMock);

        } finally {
            orcidWebhookService.setOrcidClient(orcidClient);
        }

    }

    @Test
    @Ignore("OrcidQueueServiceImpl.findAllEntitiesLinkableWith not able to find publications/fundings of CvPerson")
    public void testOrcidSynchronizationPreferenceUpdateForceOrcidQueueRecalculation() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidAccessToken("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4")
            .withOrcidRefreshToken("6b29a03d-f494-4690-889f-2c0ddf26b82d")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        UUID ePersonId = ePerson.getID();

        Item profile = createProfile(ePerson);

        UUID profileItemId = profile.getID();

        Collection publications = createCollection("Publications", "Publication");

        Item publication = createPublication(publications, "Test publication", profile);

        Collection fundings = createCollection("Fundings", "Funding");

        Item firstFunding = createFundingWithInvestigator(fundings, "First funding", profile);
        Item secondFunding = createFundingWithCoInvestigator(fundings, "Second funding", profile);

        context.restoreAuthSystemState();

        // no preferences configured, so no orcid queue records created
        assertThat(orcidQueueService.findByOwnerId(context, profileItemId), empty());

        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/publications", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        List<OrcidQueue> queueRecords = orcidQueueService.findByOwnerId(context, profileItemId);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(publication)));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        queueRecords = orcidQueueService.findByOwnerId(context, profileItemId);
        assertThat(queueRecords, hasSize(3));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(publication)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(firstFunding)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(secondFunding)));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/publications", "DISABLED"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        queueRecords = orcidQueueService.findByOwnerId(context, profileItemId);
        assertThat(queueRecords, hasSize(2));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(firstFunding)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(secondFunding)));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "DISABLED"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        assertThat(orcidQueueService.findByOwnerId(context, profileItemId), empty());

        configurationService.setProperty("orcid.linkable-metadata-fields.ignore", "crisfund.coinvestigators");

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        queueRecords = orcidQueueService.findByOwnerId(context, profileItemId);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(firstFunding)));

        // verify that no ORCID queue recalculation is done if the preference does not change
        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        List<OrcidQueue> newRecords = orcidQueueService.findByOwnerId(context, profileItemId);
        assertThat(newRecords, hasSize(1));
        assertThat(queueRecords.get(0).getID(), is(newRecords.get(0).getID()));

    }

    @Test
    public void researcherProfileClaim() throws Exception {
        String id = user.getID().toString();
        String name = user.getName();

        context.turnOffAuthorisationSystem();

        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType cvPersonType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, cvPersonType,
            personType, "isPersonOwner", "isOwnedByCvPerson", 0, 1000, 0, 1000).build();

        Item person = ItemBuilder.createItem(context, personCollection)
                                      .withFullName("Doe, John")
                                      .build();

        Item otherPerson = ItemBuilder.createItem(context, personCollection)
                                       .withFullName("Smith, Jane")
                                       .build();

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Cv Person Clone Collection").withEntityType("CvPersonClone").build();

        configurationService.setProperty("cti-vitae.clone.person-collection-id",
            cvPersonCloneCollection.getID().toString());
        configurationService.setProperty("claimable.relation.rightwardType", "isOwnedByCvPerson");

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$.id", is(id)))
                            .andExpect(jsonPath("$.type", is("profile")))
                            .andExpect(jsonPath("$",
                                                matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
                            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.type", is("item")))
                            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id, 0)))
                            .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
                            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.type", is("eperson")))
                            .andExpect(jsonPath("$.name", is(name)));

        // trying to claim another profile
        getClient(authToken).perform(post("/api/cris/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + otherPerson.getID().toString()))
                            .andExpect(status().isConflict());

        // other person trying to claim same profile
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withEmail("foo@bar.baz")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        context.restoreAuthSystemState();

        final String ePersonToken = getAuthToken(ePerson.getEmail(), password);

        getClient(ePersonToken).perform(post("/api/cris/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isBadRequest());

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id))
                            .andExpect(status().isNoContent());
    }

    @Test
    public void claimForNotAllowedEntityType() throws Exception {
        String id = user.getID().toString();
        String name = user.getName();

        context.turnOffAuthorisationSystem();

        final Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                        .withEntityType("Publication")
                                                        .build();

        final Item publication = ItemBuilder.createItem(context, publications)
                                       .withTitle("title")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + publication.getID().toString()))
                            .andExpect(status().isBadRequest());
    }

    @Test
    public void testCloneFromExternalSourceRecordNotFound() throws Exception {

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken)
            .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                                                .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/FAKE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testCloneFromExternalSourceMultipleUri() throws Exception {

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken)
            .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                                                .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n "
                                                             + "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testCloneFromExternalProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
                            .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken)
            .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                                                .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id"))
            .andExpect(status().isConflict());
    }

    @Test
    public void testCloneFromExternalCollectionNotSet() throws Exception {

        configurationService.setProperty("researcher-profile.collection.uuid", "not-existing");
        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id.toString())))
                            .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken)
            .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                                                .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n "
                                                             + "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
            .andExpect(status().isBadRequest());
    }

    private Item createProfile(EPerson ePerson) throws Exception {

        String authToken = getAuthToken(ePerson.getEmail(), password);

        AtomicReference<UUID> ePersonIdRef = new AtomicReference<UUID>();
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andDo(result -> ePersonIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", ePersonIdRef.get())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        return itemService.find(context, itemIdRef.get());
    }

//    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
//        MvcResult result = getClient(token).perform(get("/api/cris/profiles/{id}/item", id))
//                                           .andExpect(status().isOk())
//                                           .andReturn();
//    }

    private void assertSearchQuery(Item item, String owner, String authority)
            throws SearchServiceException, SolrServerException, IOException {
        SolrClient solrClient = searchService.getSolrSearchCore().getSolr();
        SolrDocument doc = solrClient.getById("Item-" + item.getID().toString());
        assertEquals(owner, (String) doc.getFirstValue("perucris.ctivitae.owner"));
        assertEquals(authority, (String) doc.getFirstValue("perucris.ctivitae.owner_authority"));
    }

    @Test
    public void createAndReturnCheckNestedMetadataFieldsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        EPerson user = EPersonBuilder.createEPerson(context).withNameInMetadata("Viktok", "Bruni")
                .withEmail("viktor.bruni@test.com").withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection cvCollection = CollectionBuilder.createCollection(context, parentCommunity).withName("Profiles")
                .withEntityType("CvPerson").build();

        Collection cvCloneCollection = CollectionBuilder.createCollection(context, parentCommunity).withName("Profiles")
                .withEntityType("CvPersonClone").withWorkflow("institutionWorkflow").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withEntityType("Person")
                .withName("Collection 1").build();

        Item personItem = ItemBuilder.createItem(context, col1).withTitle("Person Item Title")
                .withPersonEducation("High school").withPersonEducationStartDate("1968-09-01")
                .withPersonEducationEndDate("1973-06-10").withEntityType("Person").build();

        AtomicReference<UUID> idRef = new AtomicReference<UUID>();
        try {
            MetadataField title = metadataFieldService.findByElement(context, "dc", "title", null);
            MetadataField education = metadataFieldService.findByElement(context, "crisrp", "education", null);

            MetadataField educationStart = metadataFieldService.findByElement(context, "crisrp", "education", "start");
            MetadataField educationEnd = metadataFieldService.findByElement(context, "crisrp", "education", "end");

            List<MetadataField> nestedFields = new ArrayList<MetadataField>();
            nestedFields.add(educationStart);
            nestedFields.add(educationEnd);

            CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                    .withShortname("box-shortname-one").withSecurity(LayoutSecurity.PUBLIC).build();

            CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0).withLabel("LABEL TITLE")
                    .withRendering("RENDERIGN TITLE").withStyle("STYLE").withBox(box1).build();

            CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                    .withShortname("box-shortname-two").withSecurity(LayoutSecurity.PUBLIC).build();

            CrisLayoutFieldBuilder.createMetadataField(context, education, 0, 0).withLabel("LABEL EDUCATION")
                    .withRendering("RENDERIGN EDUCATION").withStyle("STYLE").withBox(box2).withNestedField(nestedFields)
                    .build();

            configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
            configurationService.setProperty("cti-vitae.clone.person-collection-id",
                    cvCloneCollection.getID().toString());
            context.restoreAuthSystemState();

            String tokenUser = getAuthToken(user.getEmail(), password);

            getClient(tokenUser)
                    .perform(post("/api/cris/profiles/").contentType(TEXT_URI_LIST)
                            .content("http://localhost:8080/server/api/core/items/" + personItem.getID()))
                    .andExpect(status().isCreated()).andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            getClient(tokenUser).perform(get("/api/cris/profiles/{id}/item", idRef.get())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata",
                            matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)))
                    .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.education", "High school", 0)))
                    .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.education.start", "1968-09-01", 0)))
                    .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.education.end", "1973-06-10", 0)))
                    .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)))
                    .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Person Item Title", 0)));
        } finally {
            ItemBuilder.deleteItem(idRef.get());
        }
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/cris/profiles/{id}/item", id)).andExpect(status().isOk())
                .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
                SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(), CLONE.getRightType(), 0,
                1, 0, 1).build();
    }

    private RelationshipType createIsOriginatedFromRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType, ORIGINATED.getLeftType(),
                ORIGINATED.getRightType(), 0, null, 0, 1).build();
    }

    private RelationshipType createIsMergedInRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, MERGED.getLeftType(),
                MERGED.getRightType(), 0, 1, 0, null).build();
    }

    private RelationshipType createIsPersonOwnerRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType, "isPersonOwner", "isOwnedByCvPerson", 0,
                null, 0, null).build();
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private MetadataField metadataField(String schema, String element, Optional<String> qualifier) throws SQLException {

        MetadataSchema metadataSchema = metadataSchemaService.find(context, schema);

        return metadataFieldService.findByElement(context, metadataSchema, element, qualifier.orElse(null));
    }

    private ResearcherProfile createProfileForUser(EPerson ePerson) throws Exception {
        return researcherProfileService.createAndReturn(context, ePerson);
    }

    private void addMetadata(Item item, String schema, String element, String qualifier,
        String value) throws Exception {
        context.turnOffAuthorisationSystem();
        item = context.reloadEntity(item);
        itemService.addMetadata(context, item, schema, element, qualifier, null, value, null, -1);
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    private Collection createCollection(String name, String entityType) throws SQLException {
        return CollectionBuilder.createCollection(context, context.reloadEntity(parentCommunity))
            .withName(name)
            .withEntityType(entityType)
            .build();
    }

    private Item createPublication(Collection collection, String title, Item author) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withAuthor(author.getName(), author.getID().toString())
            .build();
    }

    private Item createFundingWithInvestigator(Collection collection, String title, Item investigator) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withFundingInvestigator(investigator.getName(), investigator.getID().toString())
            .build();
    }

    private Item createFundingWithCoInvestigator(Collection collection, String title, Item investigator) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withFundingCoInvestigator(investigator.getName(), investigator.getID().toString())
            .build();
    }

    private Predicate<OrcidQueue> orcidQueueRecordWithEntity(Item entity) {
        return orcidQueue -> entity.equals(orcidQueue.getEntity());
    }

    private OrcidTokenResponseDTO buildTokenResponse(String accessToken) {
        OrcidTokenResponseDTO response = new OrcidTokenResponseDTO();
        response.setAccessToken(accessToken);
        return response;
    }

    private static class CtiDatabaseMockBuilder {

        protected static final Integer CTI_SEXO_MASCULINO = 2;
        protected static final Integer PAIS_NACIMIENTO_ID = 2;
        protected static final Integer PAIS_RESIDENCIA_ID = 3;
        protected static final Integer DEPARTAMENTO_ID = 4;
        protected static final Integer PROVINCIA_ID = 5;
        protected static final Integer DISTRITO_ID = 6;

        protected static final Integer DATO_CONFIDENTIALES_ID = 7;
        protected static final Integer INSTITUTION_LABORAL_ID = 8;
        protected static final Integer DATOS_LABORALES_ID = 9;
        protected static final Integer FORMACION_ACADEMICA_ID = 10;
        protected static final Integer GRADO_ACADEMICO_ID = 11;
        protected static final Integer CENTRO_ESTUDIOS_ID = 12;
        protected static final Integer CENTRO_ESTUDIOS_PAIS_ID = 13;

        protected static final Integer PRODUCCION_BIBLIOGRAFICA_ID = 14;
        protected static final Integer PROYECTO_ID = 15;
        protected static final Integer PROPRIEDAD_INTELECTUAL_ID = 16;

        protected CtiInvestigador mockInvestigador() {
            CtiInvestigador investigador = new CtiInvestigador();
            investigador.setCtiId(INVESTIGADOR_ID_TEST);

            investigador.setApellidoMaterno("ApellidoMaterno");
            investigador.setApellidoPaterno("ApellidoPaterno");
            investigador.setNombres("Nombres");
            investigador.setDescPersonal("DescPersonal");
            investigador.setDireccionWeb("DireccionWeb");
            investigador.setEmail("investigador@cti.com");

            investigador.setIdOrcid("orcid");
            investigador.setIdPerfilScopus("idPerfilScopus");

            investigador.setInstitucionLaboralId(INSTITUTION_LABORAL_ID);

            investigador.setPaisNacimientoId(PAIS_NACIMIENTO_ID);
            investigador.setPaisNacimientoNombre("PaisNacimientoNombre");
            investigador.setPaisResidenciaId(PAIS_RESIDENCIA_ID);
            investigador.setPaisResidenciaNombre("PaisResidenciaNombre");

            // Geo
            investigador.setDistritoDesc("DistritoDesc");
            investigador.setDistritoId(DISTRITO_ID);
            investigador.setDepartamentoDescr("DepartamentoDescr");
            investigador.setDepartamentoId(DEPARTAMENTO_ID);
            investigador.setProvinciaDesc("ProvinciaDesc");
            investigador.setProvinciaId(PROVINCIA_ID);
            investigador.setSexo(CTI_SEXO_MASCULINO);

            return investigador;
        }

        protected List<CtiDatosConfidenciales> mockDatosConfidenciales() {
            CtiDatosConfidenciales datosConfidenciales = new CtiDatosConfidenciales();
            datosConfidenciales.setCtiId(DATO_CONFIDENTIALES_ID);
            datosConfidenciales.setFechaNacimiento(formatDate("1980-01-01"));
            datosConfidenciales.setNroDocumento("NroDocumento");
            datosConfidenciales.setTelefonoCelular("TelefonoCelular");
            datosConfidenciales.setTelefonoFijo("TelefonoFijo");
            datosConfidenciales.setTipoDocumentoId(1);
            List<CtiDatosConfidenciales> datosConfidentialesList = new ArrayList<>();
            datosConfidentialesList.add(datosConfidenciales);
            return datosConfidentialesList;
        }

        protected List<CtiDatosLaborales> mockDatosLaboralesList() {
            CtiDatosLaborales datosLaborales = new CtiDatosLaborales();
            datosLaborales.setCtiId(DATOS_LABORALES_ID);
            datosLaborales.setCargo("Cargo");
            datosLaborales.setFechaInicio(formatDate("2020-01-02"));
            datosLaborales.setFechaFin(formatDate("2020-01-03"));
            datosLaborales.setInstitucionId(INSTITUTION_LABORAL_ID);
            datosLaborales.setInstitucionRazonSocial("InstitucionRazonSocial");
            List<CtiDatosLaborales> datosLaboralesList = new ArrayList<>();
            datosLaboralesList.add(datosLaborales);
            return datosLaboralesList;
        }

        protected List<CtiFormacionAcademica> mockFormacionAcademica() {
            CtiFormacionAcademica formacionAcademica = new CtiFormacionAcademica();
            formacionAcademica.setCtiId(FORMACION_ACADEMICA_ID);
            formacionAcademica.setTitulo("Titulo");
            formacionAcademica.setFechaInicio(formatDate("2020-01-10"));
            formacionAcademica.setFechaFin(formatDate("2020-01-11"));
            formacionAcademica.setGradoAcademicoDescripcion("GradoAcademicoDescripcion");
            formacionAcademica.setGradoAcademicoId(GRADO_ACADEMICO_ID);
            formacionAcademica.setCentroEstudiosNombre("CentroEstudiosNombre");
            formacionAcademica.setCentroEstudiosId(CENTRO_ESTUDIOS_ID);
            formacionAcademica.setCentroEstudiosPaisId(CENTRO_ESTUDIOS_PAIS_ID);
            formacionAcademica.setCentroEstudiosPaisNombre("CentroEstudiosPaisNombre");
            List<CtiFormacionAcademica> formacionAcademicaList = new ArrayList<>();
            formacionAcademicaList.add(formacionAcademica);
            return formacionAcademicaList;
        }

        protected List<CtiProduccionBibliografica> mockProduccionBibliografica() {
            CtiProduccionBibliografica produccionBibliografica = new CtiProduccionBibliografica();
            produccionBibliografica.setCtiId(PRODUCCION_BIBLIOGRAFICA_ID);
            produccionBibliografica.setTitulo("Suggestion Titulo");
            List<CtiProduccionBibliografica> produccionBibliogaficaList = new ArrayList<>();
            produccionBibliogaficaList.add(produccionBibliografica);
            return produccionBibliogaficaList;
        }

        protected List<CtiProyecto> mockProyecto() {
            CtiProyecto proyecto = new CtiProyecto();
            proyecto.setCtiId(PROYECTO_ID);
            proyecto.setTitulo("Suggestion Titulo");
            List<CtiProyecto> proyectoList = new ArrayList<>();
            proyectoList.add(proyecto);
            return proyectoList;
        }

        protected List<CtiDerechosPi> mockDerechosPi() {
            CtiDerechosPi propriedad = new CtiDerechosPi();
            propriedad.setCtiId(PROPRIEDAD_INTELECTUAL_ID);
            propriedad.setTituloPi("Suggestion TituloPI");
            List<CtiDerechosPi> propriedadList = new ArrayList<>();
            propriedadList.add(propriedad);
            return propriedadList;
        }

        private Date formatDate(String date) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(date);
            } catch (ParseException e) {
                //
            }
            return null;
        }
    }

    private void mockCtiDatabaseDao(CtiDatabaseDao ctiDatabaseDaoMock) throws ParseException {

        CtiDatabaseMockBuilder ctiBuilder = new CtiDatabaseMockBuilder();

        // lookup
        when(ctiDatabaseDaoMock.getInvestigadorIdFromDni(DNI_TEST)).thenReturn(INVESTIGADOR_ID_TEST);

        // profile
        when(ctiDatabaseDaoMock.getInvestigadorBaseInfo(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockInvestigador());
        when(ctiDatabaseDaoMock.getDatosConfidenciales(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockDatosConfidenciales());
        when(ctiDatabaseDaoMock.getDatosLaborales(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockDatosLaboralesList());
        when(ctiDatabaseDaoMock.getFormacionAcademica(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockFormacionAcademica());

        // suggestions
        when(ctiDatabaseDaoMock.getAllProduccionesBibliograficas(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockProduccionBibliografica());
        when(ctiDatabaseDaoMock.getAllProyectos(INVESTIGADOR_ID_TEST)).thenReturn(ctiBuilder.mockProyecto());
        when(ctiDatabaseDaoMock.getAllPropriedadIntelectual(INVESTIGADOR_ID_TEST))
        .thenReturn(ctiBuilder.mockDerechosPi());

        this.ctiDatabaseImportFacade.setCtiDatabaseDao(ctiDatabaseDaoMock);

    }

    private ResultMatcher mergeDirectorioCtiResultMatcher(EPerson user) {
        return ResultMatcher.matchAll(
                //
                jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)),
                jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)),

                // From Directorio (claimed profile)
                jsonPath("$.metadata", matchMetadata("crisrp.name", "Giuseppe Garibaldi", 0)),
                jsonPath("$.metadata", matchMetadata("person.birthDate", "1807-07-04", 0)),

                // From cti database
                jsonPath("$.metadata", matchMetadata("dc.title", "ApellidoPaterno ApellidoMaterno Nombres", 0)),
                jsonPath("$.metadata", matchMetadata("person.email", "investigador@cti.com", 0)),
                jsonPath("$.metadata", matchMetadata("person.familyName", "ApellidoPaterno ApellidoMaterno", 0)),
                jsonPath("$.metadata", matchMetadata("person.givenName", "Nombres", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.apellidoMaterno", "ApellidoMaterno", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.apellidoPaterno", "ApellidoPaterno", 0)),
                jsonPath("$.metadata", matchMetadata("oairecerif.person.gender", "m", 0)),
                jsonPath("$.metadata", matchMetadata("oairecerif.identifier.url", "DireccionWeb", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.mobilePhone", "TelefonoCelular", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.phone", "TelefonoFijo", 0)),
                jsonPath("$.metadata", matchMetadata("person.identifier.scopus-author-id", "idPerfilScopus", 0)),

                jsonPath("$.metadata", matchMetadata("crisrp.education", "Titulo", 0)),
                jsonPath("$.metadata", matchMetadata("crisrp.education.role", "GradoAcademicoDescripcion", 0)),
                jsonPath("$.metadata", matchMetadata("crisrp.education.start", "2020-01-10", 0)),
                jsonPath("$.metadata", matchMetadata("crisrp.education.end", "2020-01-11", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.education.country",
                        "#PLACEHOLDER_PARENT_METADATA_VALUE#", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.education.grantor", "CentroEstudiosNombre", 0)),

                jsonPath("$.metadata", matchMetadata("oairecerif.person.affiliation", "InstitucionRazonSocial", 0)),
                jsonPath("$.metadata", matchMetadata("oairecerif.affiliation.startDate", "2020-01-02", 0)),
                jsonPath("$.metadata", matchMetadata("oairecerif.affiliation.endDate", "2020-01-03", 0)),
                jsonPath("$.metadata", matchMetadata("oairecerif.affiliation.role", "Cargo", 0)),

                jsonPath("$.metadata", matchMetadata("person.jobTitle", "Cargo", 0)),
                jsonPath("$.metadata", matchMetadata("person.affiliation.name", "InstitucionRazonSocial", 0)),

                jsonPath("$.metadata", matchMetadata("perucris.identifier.cti", "1", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.identifier.dina", "1", 0)),
                jsonPath("$.metadata", matchMetadata("perucris.identifier.dni", "01234567", 0))

                );
    }

}
