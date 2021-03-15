/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.REINSTATE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Test suite for testing Deduplication operations
 * 
 * @author luca giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SubmissionDeduplicationRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private XmlWorkflowService xmlWorkflowService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private ConfigurationService configurationService;

    private Collection collection;

    private Collection institutionCollection;

    private EPerson submitter;

    private EPerson editor;

    private EntityType publicationType;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Root community")
            .build();

        submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();

        editor = EPersonBuilder.createEPerson(context)
            .withEmail("editor@example.com")
            .withPassword(password)
            .build();

        institutionCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Institution collection")
            .withRelationshipType("InstitutionPublication")
            .withSubmissionDefinition("institution-publication")
            .withSubmitterGroup(submitter)
            .withWorkflowGroup(2, editor)
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withRelationshipType("Publication")
            .withSubmissionDefinition("publication")
            .withSubmitterGroup(submitter)
            .withWorkflowGroup(2, editor)
            .build();

        publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void testWorkspaceSubmissionDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", collection);

        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testInstitutionWorkspaceSubmissionDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", institutionCollection);

        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", institutionCollection);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']").doesNotExist());

    }

    @Test
    public void testInstitutionWorkflowDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", institutionCollection);

        WorkflowItem workflowItem = createWorkflowItem("Test publication", institutionCollection);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']").doesNotExist());

    }

    @Test
    public void testWorkflowDuplicationRejection() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", collection);
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", collection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision").doesNotExist());

        claimTask(workflowItem, editor);

        String patchBody = patchDetectDuplication(itemId, "reject", null);
        String editorToken = getAuthToken(editor.getEmail(), password);

        getClient(editorToken).perform(patch("/api/workflow/workflowitems/" + workflowItem.getID())
            .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision", is("reject")));

        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision", is("reject")));

    }

    @Test
    public void testWorkflowDuplicationVerification() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", collection);
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", collection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision").doesNotExist())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowNote").doesNotExist());

        claimTask(workflowItem, editor);

        String patchBody = patchDetectDuplication(itemId, "verify", "note");
        String editorToken = getAuthToken(editor.getEmail(), password);

        getClient(editorToken).perform(patch("/api/workflow/workflowitems/" + workflowItem.getID())
            .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision", is("verify")))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowNote", is("note")));

        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision", is("verify")))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowNote", is("note")));

    }

    @Test
    public void testWorkflowDuplicationOnlyWithNotInstitutionItemArchived() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", collection);
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", collection);

        createItem("Test publication", institutionCollection);

        createWorkflowItem("Test publication", collection);

        createWorkspaceItem("Test publication", collection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches", aMapWithSize(1)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "'].matchObject.id", is(itemId)))
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowDecision").doesNotExist())
            .andExpect(jsonPath("$.sections['detect-duplicate'].matches['" + itemId + "']"
                + ".workflowNote").doesNotExist());
    }

    @Test
    public void testNoDuplicationOccursWithCorrection() throws Exception {

        context.turnOffAuthorisationSystem();

        configurationService.setProperty("item-correction.permit-all", true);
        createIsCorrectionOfRelationship(publicationType);

        Item item = createItem("Test publication", collection);

        WorkspaceItem workspaceItemCorrection = requestForItemCorrection(submitter, item);

        XmlWorkflowItem workflowItemCorrection = xmlWorkflowService.start(context, workspaceItemCorrection);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItemCorrection.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testNoDuplicationOccursWithWithdraw() throws Exception {

        context.turnOffAuthorisationSystem();

        configurationService.setProperty("item-withdrawn.permit-all", true);
        createIsWithdrawOfRelationship(publicationType);

        Item item = createItem("Test publication", collection);

        WorkspaceItem workspaceItemWithdraw = requestForItemWithdraw(submitter, item);

        XmlWorkflowItem workflowItemWithdraw = xmlWorkflowService.start(context, workspaceItemWithdraw);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItemWithdraw.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testNoDuplicationOccursWithReinstate() throws Exception {

        context.turnOffAuthorisationSystem();

        configurationService.setProperty("item-reinstate.permit-all", true);
        createIsReinstatementOfRelationship(publicationType);

        Item item = createItem("Test publication", collection);

        WorkspaceItem workspaceItemReinstate = requestForItemReinstate(submitter, item);

        XmlWorkflowItem workflowItemReinstate = xmlWorkflowService.start(context, workspaceItemReinstate);

        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workflow/workflowitems/" + workflowItemReinstate.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    private Item createItem(String title, Collection collection) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .build();
    }

    private WorkspaceItem createWorkspaceItem(String title, Collection collection) {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle(title)
            .withSubmitter(submitter)
            .build();
    }

    private WorkflowItem createWorkflowItem(String title, Collection collection) {
        return WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle(title)
            .withSubmitter(submitter)
            .build();
    }

    private String patchDetectDuplication(String itemId, String decision, String note) {
        Map<String, String> value = note != null ? Map.of("value", decision, "note", note) : Map.of("value", decision);
        Operation op = new AddOperation("/sections/detect-duplicate/matches/" + itemId + "/workflowDecision", value);
        return getPatchContent(List.of(op));
    }

    private void claimTask(WorkflowItem workflowItem, EPerson user) throws Exception {
        List<PoolTask> poolTasks = poolTaskService.find(context, (XmlWorkflowItem) workflowItem);
        PoolTask poolTask = poolTasks.get(0);
        performActionOnPoolTaskViaRest(user, poolTask);
    }

    private void performActionOnPoolTaskViaRest(EPerson user, PoolTask task) throws Exception {
        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/pooltasks/{id}", task.getID())
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());
    }

    private WorkspaceItem requestForItemCorrection(EPerson user, Item item) throws Exception {
        return requestForItemCopy(user, item, "isCorrectionOfItem");
    }

    private WorkspaceItem requestForItemWithdraw(EPerson user, Item item) throws Exception {
        return requestForItemCopy(user, item, "isWithdrawOfItem");
    }

    private WorkspaceItem requestForItemReinstate(EPerson user, Item item) throws Exception {
        return requestForItemCopy(user, item, "isReinstatementOfItem");
    }

    private WorkspaceItem requestForItemCopy(EPerson user, Item item, String relationship) throws Exception {
        AtomicInteger idRef = new AtomicInteger();

        getClient(getAuthToken(user.getEmail(), password))
            .perform(post("/api/submission/workspaceitems")
                .param("relationship", relationship)
                .param("item", item.getID().toString())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        return workspaceItemService.find(context, idRef.get());
    }

    private RelationshipType createIsWithdrawOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, WITHDRAW.getLeftType(), WITHDRAW.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsReinstatementOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, REINSTATE.getLeftType(), REINSTATE.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsCorrectionOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1).build();
    }

}
