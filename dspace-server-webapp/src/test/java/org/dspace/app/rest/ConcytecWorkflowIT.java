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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private ItemService itemService;


    private Collection collection;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EPerson submitter;

    private Community directorioCommunity;

    private Collection directorioPublications;

    private Group directorioEditorGroup;

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

        directorioEditorGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(admin)
            .build();

        directorioPublications = CollectionBuilder
            .createCollection(context, directorioSubCommunity, "123456789/directorio-workflow-test")
            .withName("Publications")
            .withRelationshipType("Publication")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Group reviewGroup = GroupBuilder.createGroup(context)
            .withName("Reviewer group")
            .addMember(submitter)
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity, "123456789/institution-workflow-test")
            .withName("Institution collection")
            .withRelationshipType("Publication")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        workflowItemService.deleteByCollection(context, directorioPublications);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testItemSubmissionWithInstitutionReject() throws Exception {

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

        submitItemViaRest(submitter, workspaceItem.getID());

        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getLeftItem(), equalTo(item));
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo(HAS_SHADOW_COPY_RELATIONSHIP));
        assertThat(relationship.getRelationshipType().getRightwardType(), equalTo(IS_SHADOW_COPY_RELATIONSHIP));

        item = context.reloadEntity(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();
        UUID shadowItemCopyId = shadowItemCopy.getID();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem workflowItem = getWorkflowItem(item);
        assertThat(workflowItem, notNullValue());

        List<ClaimedTask> tasks = claimedTaskService.findByWorkflowItem(context, workflowItem);
        assertThat(tasks, hasSize(1));

        ClaimedTask task = tasks.get(0);
        assertThat(task.getOwner(), equalTo(submitter));

        rejectClaimedTaskViaRest(submitter, task, "wrong title");

        assertThat(getWorkspaceItem(item), notNullValue());
        assertThat(itemService.find(context, shadowItemCopyId), nullValue());

    }

    @Test
    public void testItemSubmissionWithConcytecApprove() throws Exception {

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

        submitItemViaRest(submitter, workspaceItem.getID());

        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getLeftItem(), equalTo(item));
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo(HAS_SHADOW_COPY_RELATIONSHIP));
        assertThat(relationship.getRelationshipType().getRightwardType(), equalTo(IS_SHADOW_COPY_RELATIONSHIP));

        item = context.reloadEntity(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        List<PoolTask> poolTasks = poolTaskService.find(context, shadowWorkflowItemCopy);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup(), equalTo(directorioEditorGroup));

        performActionOnPoolTaskViaRest(admin, poolTask);

        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, shadowWorkflowItemCopy, admin);
        assertThat(claimedTask, notNullValue());
        approveClaimedTaskViaRest(admin, claimedTask);

        item = context.reloadEntity(item);
        assertThat(item.isArchived(), is(true));

        shadowItemCopy = context.reloadEntity(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(true));

    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }

    private XmlWorkflowItem getWorkflowItem(Item item) throws SQLException {
        return workflowItemService.findByItem(context, item);
    }

    private WorkspaceItem getWorkspaceItem(Item item) throws SQLException {
        return workspaceItemService.findByItem(context, item);
    }

    private void submitItemViaRest(EPerson user, Integer wsId) throws Exception, SQLException {
        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + wsId).contentType(textUriContentType))
            .andExpect(status().isCreated());
    }

    private void rejectClaimedTaskViaRest(EPerson user, ClaimedTask task, String reason) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_reject", "submit_reject");
        params.add("reason", reason);
        performActionOnClaimedTaskViaRest(user, task, params);
    }

    private void approveClaimedTaskViaRest(EPerson user, ClaimedTask task) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_approve", "submit_approve");
        performActionOnClaimedTaskViaRest(user, task, params);
    }

    private void performActionOnPoolTaskViaRest(EPerson user, PoolTask task) throws Exception {
        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/pooltasks/{id}", task.getID())
            .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());
    }

    private void performActionOnClaimedTaskViaRest(EPerson user, ClaimedTask task, MultiValueMap<String, String> params)
        throws Exception {

        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", task.getID()).params(params)
            .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());

    }
}
