/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.content.Item.ANY;
import static org.dspace.xmlworkflow.ConcytecFeedback.APPROVE;
import static org.dspace.xmlworkflow.ConcytecFeedback.REJECT;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.HAS_SHADOW_COPY_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_SHADOW_COPY_RELATIONSHIP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import java.util.concurrent.atomic.AtomicInteger;

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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
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
    private XmlWorkflowService workflowService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    private ItemService itemService;

    private Collection collection;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EPerson submitter;

    private EPerson firstDirectorioUser;

    private EPerson secondDirectorioUser;

    private EPerson institutionUser;

    private Community directorioCommunity;

    private Collection directorioPublications;

    private Group directorioEditorGroup;

    private Group directorioReviewGroup;

    @Before
    public void before() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            HAS_SHADOW_COPY_RELATIONSHIP, IS_SHADOW_COPY_RELATIONSHIP, 0, 1, 0, 1);
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1);

        submitter = createEPerson("submitter@example.com");
        firstDirectorioUser = createEPerson("firstDirectorioUser@example.com");
        secondDirectorioUser = createEPerson("secondDirectorioUser@example.com");
        institutionUser = createEPerson("user@example.com");

        directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        Community directorioSubCommunity = CommunityBuilder.createSubCommunity(context, directorioCommunity)
            .withName("Directorio de Produccion Cientifica")
            .build();

        directorioReviewGroup = GroupBuilder.createGroup(context)
            .withName("review group")
            .addMember(firstDirectorioUser)
            .addMember(secondDirectorioUser)
            .build();

        directorioEditorGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(firstDirectorioUser)
            .build();

        directorioPublications = CollectionBuilder
            .createCollection(context, directorioSubCommunity, "123456789/directorio-workflow-test")
            .withName("Publications")
            .withRelationshipType("Publication")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", directorioReviewGroup)
            .withRoleGroup("editor", directorioEditorGroup)
            .withRoleGroup("finaleditor", directorioEditorGroup)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Group reviewGroup = GroupBuilder.createGroup(context)
            .withName("Reviewer group")
            .addMember(institutionUser)
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
        collectionRoleService.deleteByCollection(context, collection);
        collectionRoleService.deleteByCollection(context, directorioPublications);
        workflowItemService.deleteByCollection(context, collection);
        workflowItemService.deleteByCollection(context, directorioPublications);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testItemSubmissionWithInstitutionReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem workflowItem = getWorkflowItem(item);
        assertThat(workflowItem, notNullValue());

        List<ClaimedTask> tasks = claimedTaskService.findByWorkflowItem(context, workflowItem);
        assertThat(tasks, hasSize(1));

        ClaimedTask task = tasks.get(0);
        assertThat(task.getOwner(), equalTo(institutionUser));

        rejectClaimedTaskViaRest(institutionUser, task, "wrong title");

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(getWorkspaceItem(item), notNullValue());
        assertThat(reloadItem(shadowItemCopy), nullValue());

    }

    @Test
    public void testItemSubmissionWithInstitutionRejectAfterDirectorioReviewerApprovement() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();
        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(shadowWorkflowItemCopy, notNullValue());

        XmlWorkflowItem workflowItem = getWorkflowItem(item);
        assertThat(workflowItem, notNullValue());

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioReviewGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        List<ClaimedTask> tasks = claimedTaskService.findByWorkflowItem(context, workflowItem);
        assertThat(tasks, hasSize(1));

        ClaimedTask task = tasks.get(0);
        assertThat(task.getOwner(), equalTo(institutionUser));

        rejectClaimedTaskViaRest(institutionUser, task, "wrong title");

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(getWorkspaceItem(item), notNullValue());
        assertThat(reloadItem(shadowItemCopy), nullValue());

    }

    @Test
    public void testItemSubmissionWithConcytecApprove() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioReviewGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(APPROVE.name()));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(true));

    }

    @Test
    public void testItemSubmissionWithConcytecReviewerReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        claimTaskAndReject(shadowWorkflowItemCopy, firstDirectorioUser, directorioReviewGroup, "wrong publication");

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(REJECT.name()));
        assertThat(getConcytecCommentMetadataValue(item), equalTo("wrong publication"));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(reloadItem(shadowItemCopy), notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(shadowItemCopy), equalTo(REJECT.name()));
        assertThat(getConcytecCommentMetadataValue(shadowItemCopy), equalTo("wrong publication"));

    }

    @Test
    public void testItemSubmissionWithConcytecEditorReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioReviewGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy), notNullValue());

        claimTaskAndReject(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup, "wrong publication");

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(REJECT.name()));
        assertThat(getConcytecCommentMetadataValue(item), equalTo("wrong publication"));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(reloadItem(shadowItemCopy), notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(shadowItemCopy), equalTo(REJECT.name()));
        assertThat(getConcytecCommentMetadataValue(shadowItemCopy), equalTo("wrong publication"));

    }

    @Test
    public void testItemSubmissionWithConcytecEdit() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThatIsShadowRelationship(relationship, item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(item.getMetadata(), hasSize(shadowItemCopy.getMetadata().size() - 1));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioReviewGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        String directorioTitle = "Submission Item Edited";
        itemService.replaceMetadata(context, shadowItemCopy, "dc", "title", null, null, directorioTitle, null, -1, 0);

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(APPROVE.name()));
        assertThat(getTitle(item), equalTo("Submission Item"));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(getTitle(shadowItemCopy), equalTo(directorioTitle));

    }

    @Test
    public void testItemCorrectionApproved() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));

        Item shadowItemCopy = relationships.get(0).getRightItem();
        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioReviewGroup);
        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);
        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Item submission new title");
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

    }

    private void assertThatIsShadowRelationship(Relationship relationship, Item leftItem) {
        assertThat(relationship.getLeftItem(), equalTo(leftItem));
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo(HAS_SHADOW_COPY_RELATIONSHIP));
        assertThat(relationship.getRelationshipType().getRightwardType(), equalTo(IS_SHADOW_COPY_RELATIONSHIP));
    }

    private void claimTaskAndApprove(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup) throws Exception {
        List<PoolTask> poolTasks = poolTaskService.find(context, workflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(expectedGroup));

        performActionOnPoolTaskViaRest(user, poolTask);

        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);
        assertThat(claimedTask, notNullValue());

        approveClaimedTaskViaRest(user, claimedTask);
    }

    private void claimTaskAndReject(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup, String reason)
        throws Exception {
        List<PoolTask> poolTasks = poolTaskService.find(context, workflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(expectedGroup));

        performActionOnPoolTaskViaRest(user, poolTask);

        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);
        assertThat(claimedTask, notNullValue());

        rejectClaimedTaskViaRest(user, claimedTask, reason);
    }

    private WorkspaceItem createWorkspaceItem() throws IOException {
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
        return workspaceItem;
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

    private WorkspaceItem requestForItemCorrection(EPerson user, Item item) throws Exception {
        AtomicInteger idRef = new AtomicInteger();

        getClient(getAuthToken(user.getEmail(), password))
            .perform(post("/api/submission/workspaceitems")
            .param("owningCollection", collection.getID().toString())
            .param("relationship", "isCorrectionOfItem")
            .param("item", item.getID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        return workspaceItemService.find(context, idRef.get());
    }

    private String getConcytecCommentMetadataValue(Item item) {
        return itemService.getMetadataFirstValue(item, "perucris", "concytec", "comment", ANY);
    }

    private String getConcytecFeedbackMetadataValue(Item item) {
        return itemService.getMetadataFirstValue(item, "perucris", "concytec", "feedback", ANY);
    }

    private String getTitle(Item item) {
        return itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
    }

    private void replaceTitle(Context context, Item item, String newTitle) throws SQLException, AuthorizeException {
        itemService.replaceMetadata(context, item, "dc", "title", null, null, newTitle, null, -1, 0);
        itemService.update(context, item);
    }

    private void performActionOnClaimedTaskViaRest(EPerson user, ClaimedTask task, MultiValueMap<String, String> params)
        throws Exception {

        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", task.getID()).params(params)
            .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());

    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withPassword(password)
            .build();
    }

    private Item reloadItem(Item item) throws SQLException {
        return context.reloadEntity(item);
    }
}
