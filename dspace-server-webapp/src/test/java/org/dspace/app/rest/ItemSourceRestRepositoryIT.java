/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.ItemSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
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
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Tests against the /api/core/itemsources endpoint
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science)
 */
public class ItemSourceRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private EntityTypeService entityTypeService;

    private EntityType publicationType;
    private EntityType institutionPublicationType;
    private EntityType institutionProjectType;
    private EntityType projectType;
    @SuppressWarnings("unused")
    private Relationship relationship1;
    @SuppressWarnings("unused")
    private Relationship relationship2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        publicationType = entityTypeService.findByEntityType(context, "Publication");
        if (publicationType == null) {
            publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        }
        institutionPublicationType = entityTypeService.findByEntityType(context, "InstitutionPublication");
        if (institutionPublicationType == null) {
            institutionPublicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "InstitutionPublication")
                                                          .build();
        }
        institutionProjectType = entityTypeService.findByEntityType(context, "InstitutionProject");
        if (institutionProjectType == null) {
            institutionProjectType = EntityTypeBuilder.createEntityTypeBuilder(context, "InstitutionProject")
                                                      .build();
        }
        projectType = entityTypeService.findByEntityType(context, "Project");
        if (projectType == null) {
            projectType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        }

        context.restoreAuthSystemState();
    }

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources/" + UUID.randomUUID().toString()))
                            .andExpect(status().isNotFound());
    }

    @Test
    public void findOnIsOriginatedFromTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community directorio = CommunityBuilder.createCommunity(context)
                                               .withName("Directorio").build();

        Collection col1 = CollectionBuilder.createCollection(context, directorio)
                                           .withName("Collection 1").build();

        Item publication1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test Publication Title")
                .withAuthor("Roman, Bandola")
                .withIssueDate("2019-01-01")
                .withEntityType("Publication")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2").build();

        Item publication2 = ItemBuilder.createItem(context, col2)
                .withTitle("Publication 2")
                .withAuthor("Anton, Bandola")
                .withIssueDate("2019-01-01")
                .withEntityType("InstitutionPublication").build();

        Item publication3 = ItemBuilder.createItem(context, col2)
                .withTitle("Test Publication Title")
                .withAuthor("Roman, Bandola")
                .withIssueDate("2020-03-08")
                .withEntityType("InstitutionPublication").build();

        RelationshipType relationshipType = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                                            context, publicationType, institutionPublicationType,
                                            ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                            ConcytecWorkflowRelation.ORIGINATED.getRightType(),
                                            0, null, 0, 1).build();

        relationship1 = RelationshipBuilder.createRelationshipBuilder(context,
                                     publication1, publication2, relationshipType).build();

        relationship2 = RelationshipBuilder.createRelationshipBuilder(context,
                                     publication1, publication3, relationshipType).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources/" + publication1.getID().toString()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.id", is(publication1.getID().toString())))
                            .andExpect(jsonPath("$.type", is("itemsource")))
                            .andExpect(jsonPath("$.sources", Matchers.containsInAnyOrder(
                                       ItemSourceMatcher.matchSource(publication2.getID().toString(),
                                                   ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                                      parentCommunity.getName(), "dc.date.issued"),
                                       ItemSourceMatcher.matchSource(publication3.getID().toString(),
                                                   ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                                  parentCommunity.getName(), "dc.contributor.author", "dc.title")
                                       )));
    }


    @Test
    public void findOnIsShadowCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();
        Community directorio = CommunityBuilder.createCommunity(context)
                                               .withName("Directorio").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item publication1 = ItemBuilder.createItem(context, col1)
                .withTitle("Project Title")
                .withIssueDate("2020-03-17")
                .withEntityType("InstitutionProject")
                .build();

        Collection col2 = CollectionBuilder.createCollection(context, directorio)
                                           .withName("Collection 2").build();

        Item publication2 = ItemBuilder.createItem(context, col2)
                .withTitle("Project Title")
                .withAuthor("Anton, Bandola")
                .withIssueDate("2020-05-01")
                .withEntityType("Project").build();

        RelationshipType relationshipType = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                                            context, institutionProjectType, projectType,
                                            ConcytecWorkflowRelation.SHADOW_COPY.getLeftType(),
                                            ConcytecWorkflowRelation.SHADOW_COPY.getRightType(),
                                            0, 1, 0, 1).build();

        relationship1 = RelationshipBuilder.createRelationshipBuilder(context,
                                     publication1, publication2, relationshipType).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources/" + publication2.getID().toString()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.id", is(publication2.getID().toString())))
                            .andExpect(jsonPath("$.type", is("itemsource")))
                            .andExpect(jsonPath("$.sources", Matchers.contains(
                                       ItemSourceMatcher.matchSource(publication1.getID().toString(),
                                                  ConcytecWorkflowRelation.SHADOW_COPY.getRightType(),
                                                  parentCommunity.getName(), "dc.title")
                                       )));
    }

    @Test
    public void findOnWithOutSourcesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community directorio = CommunityBuilder.createCommunity(context)
                                               .withName("Directorio").build();

        Collection collection = CollectionBuilder.createCollection(context, directorio)
                                           .withName("Collection 1").build();

        Item publication = ItemBuilder.createItem(context, collection)
                                       .withTitle("Test Publication Title")
                                       .withAuthor("Roman, Bandola")
                                       .withIssueDate("2019-01-01")
                                       .withEntityType("Publication")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources/" + publication.getID().toString()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.id", is(publication.getID().toString())))
                            .andExpect(jsonPath("$.type", is("itemsource")))
                            .andExpect(jsonPath("$.sources").value(Matchers.hasSize(0)));
    }

    @Test
    public void findOnIsOriginatedFromAnonymousUserTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community directorio = CommunityBuilder.createCommunity(context)
                                               .withName("Directorio").build();

        Collection col1 = CollectionBuilder.createCollection(context, directorio)
                                           .withName("Collection 1").build();

        Item publication1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test Publication Title")
                .withAuthor("Roman, Bandola")
                .withIssueDate("2019-01-01")
                .withEntityType("Publication")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2").build();

        Item publication2 = ItemBuilder.createItem(context, col2)
                .withTitle("Publication 2")
                .withAuthor("Anton, Bandola")
                .withIssueDate("2019-01-01")
                .withEntityType("InstitutionPublication").build();

        Item publication3 = ItemBuilder.createItem(context, col2)
                .withTitle("Test Publication Title")
                .withAuthor("Roman, Bandola")
                .withIssueDate("2020-03-08")
                .withEntityType("InstitutionPublication").build();

        RelationshipType relationshipType = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                                            context, publicationType, institutionPublicationType,
                                            ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                            ConcytecWorkflowRelation.ORIGINATED.getRightType(),
                                            0, null, 0, 1).build();

        relationship1 = RelationshipBuilder.createRelationshipBuilder(context,
                                     publication1, publication2, relationshipType).build();

        relationship2 = RelationshipBuilder.createRelationshipBuilder(context,
                publication1, publication3, relationshipType).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/itemsources/" + publication1.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", is(publication1.getID().toString())))
                   .andExpect(jsonPath("$.type", is("itemsource")))
                   .andExpect(jsonPath("$.sources", Matchers.containsInAnyOrder(
                              ItemSourceMatcher.matchSource(publication2.getID().toString(),
                                          ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                               parentCommunity.getName(), "dc.date.issued"),
                              ItemSourceMatcher.matchSource(publication3.getID().toString(),
                                          ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                         parentCommunity.getName(), "dc.contributor.author", "dc.title")
                              )));
    }

    @Test
    public void findOnIsOriginatedFromMultiValueMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community directorio = CommunityBuilder.createCommunity(context)
                                               .withName("Directorio").build();

        Collection col1 = CollectionBuilder.createCollection(context, directorio)
                                           .withName("Collection 1").build();

        Item publication1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test Publication Title")
                .withAuthor("Roman, Bandola")
                .withAuthor("Anton, Mostoviy")
                .withIssueDate("2019-01-01")
                .withEntityType("Publication")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2").build();

        Item publication2 = ItemBuilder.createItem(context, col2)
                .withTitle("Publication 2")
                .withAuthor("Roman, Bandola")
                .withIssueDate("2019-01-01")
                .withEntityType("InstitutionPublication").build();

        Item publication3 = ItemBuilder.createItem(context, col2)
                .withTitle("Test Publication Title")
                .withAuthor("Anton, Mostoviy")
                .withIssueDate("2020-03-08")
                .withEntityType("InstitutionPublication").build();

        RelationshipType relationshipType = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                                            context, publicationType, institutionPublicationType,
                                               ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                               ConcytecWorkflowRelation.ORIGINATED.getRightType(),
                                            0, null, 0, 1).build();

        relationship1 = RelationshipBuilder.createRelationshipBuilder(context,
                                     publication1, publication2, relationshipType).build();

        relationship2 = RelationshipBuilder.createRelationshipBuilder(context,
                publication1, publication3, relationshipType).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/core/itemsources/" + publication1.getID().toString()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.id", is(publication1.getID().toString())))
                            .andExpect(jsonPath("$.type", is("itemsource")))
                            .andExpect(jsonPath("$.sources", Matchers.containsInAnyOrder(
                                       ItemSourceMatcher.matchSource(publication2.getID().toString(),
                                                   ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                           parentCommunity.getName(), "dc.contributor.author/0", "dc.date.issued"),
                                       ItemSourceMatcher.matchSource(publication3.getID().toString(),
                                                   ConcytecWorkflowRelation.ORIGINATED.getLeftType(),
                                                      parentCommunity.getName(), "dc.contributor.author/1", "dc.title")
                                       )));
    }

}