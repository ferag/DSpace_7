/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.HAS_SHADOW_COPY_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_SHADOW_COPY_RELATIONSHIP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

/**
 * Integration test for the CONCYTEC workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ConcytecWorkflowIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RelationshipService relationshipService;

    private Collection collection;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EPerson submitter;

    private Community directorioCommunity;

    private Collection directorioPublications;

    @Before
    public void before() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            HAS_SHADOW_COPY_RELATIONSHIP, IS_SHADOW_COPY_RELATIONSHIP, 0, 1, 0, 1);

        submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        Community directorioSubCommunity = CommunityBuilder.createSubCommunity(context, directorioCommunity)
            .withName("Directorio de Produccion Cientifica")
            .build();

        directorioPublications = CollectionBuilder.createCollection(context, directorioSubCommunity)
            .withName("Publications")
            .withRelationshipType("Publication")
            .withSubmitterGroup(submitter)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity, "123456789/concytec-workflow-test")
            .withName("Institution collection")
            .withRelationshipType("Publication")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer")
            .build();

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());

    }

    @Test
    public void testItemSubmission() throws Exception {

        InputStream pdf = simpleArticle.getInputStream();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Submission Item")
            .withIssueDate("2017-10-17")
            .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
            .withAuthor("Mario Rossi")
            .withAuthorAffilitation("4Science")
            .withEditor("Mario Rossi")
            .grantLicense()
            .build();

        Item item = workspaceItem.getItem();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, workspaceItem.getID());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getLeftItem(), equalTo(item));
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo(HAS_SHADOW_COPY_RELATIONSHIP));
        assertThat(relationship.getRelationshipType().getRightwardType(), equalTo(IS_SHADOW_COPY_RELATIONSHIP));

        Item shadowItemCopy = relationship.getRightItem();
        assertThat(shadowItemCopy.getOwningCollection(), equalTo(directorioPublications));
        assertThat(shadowItemCopy, not(equalTo(item)));

    }

    private void submitItemViaRest(String authToken, Integer wsId) throws Exception, SQLException {
        getClient(authToken).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + wsId).contentType(textUriContentType))
            .andExpect(status().isCreated());
    }
}
