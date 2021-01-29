/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
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

    private Collection collection;

    private EPerson submitter;

    private EPerson editor;

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

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withSubmitterGroup(submitter)
            .withWorkflowGroup(2, editor)
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testWorkspaceSubmissionDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", "Publication");

        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", "Publication");

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testInstitutionWorkspaceSubmissionDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", "InstitutionPublication");

        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", "InstitutionPublication");

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testInstitutionWorkflowDoesNotCauseDuplicationDetection() throws Exception {

        context.turnOffAuthorisationSystem();

        createItem("Test publication", "InstitutionPublication");

        WorkflowItem workflowItem = createWorkflowItem("Test publication", "InstitutionPublication");

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);
        getClient(authToken).perform(get("/api/workflow/workflowitems/" + workflowItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['detect-duplicate']", anEmptyMap()));

    }

    @Test
    public void testWorkflowDuplicationRejection() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem("Test publication", "Publication");
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", "Publication");

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

        Item item = createItem("Test publication", "Publication");
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", "Publication");

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

        Item item = createItem("Test publication", "Publication");
        String itemId = item.getID().toString();

        WorkflowItem workflowItem = createWorkflowItem("Test publication", "Publication");

        createItem("Test publication", "InstitutionPublication");

        createWorkflowItem("Test publication", "Publication");

        createWorkspaceItem("Test publication", "Publication");

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

    private Item createItem(String title, String relationshipType) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withRelationshipType(relationshipType)
            .build();
    }

    private WorkspaceItem createWorkspaceItem(String title, String relationshipType) {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle(title)
            .withRelationshipType(relationshipType)
            .withSubmitter(submitter)
            .build();
    }

    private WorkflowItem createWorkflowItem(String title, String relationshipType) {
        return WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle(title)
            .withRelationshipType(relationshipType)
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

}
