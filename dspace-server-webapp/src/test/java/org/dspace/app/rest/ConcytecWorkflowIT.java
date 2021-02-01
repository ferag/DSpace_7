/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static java.util.List.of;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.content.Item.ANY;
import static org.dspace.xmlworkflow.ConcytecFeedback.APPROVE;
import static org.dspace.xmlworkflow.ConcytecFeedback.REJECT;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.HAS_SHADOW_COPY_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_ENRICHED_BY_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_MERGED_IN_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_ORIGINATED_FROM_IN_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_ORIGIN_OF_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_REINSTATED_BY_ITEM_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_REINSTATEMENT_OF_ITEM_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_SHADOW_COPY_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_WITHDRAWN_BY_ITEM_RELATIONSHIP;
import static org.dspace.xmlworkflow.service.ConcytecWorkflowService.IS_WITHDRAW_OF_ITEM_RELATIONSHIP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
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
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration test for the CONCYTEC workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ConcytecWorkflowIT extends AbstractControllerIntegrationTest {

    /**
     * 
     */
    private static final String ITEM_TITLE = "Submission Item";

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

    @Autowired
    private DedupUtils dedupUtils;

    private Collection institutionCollection;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EPerson submitter;

    private EPerson firstDirectorioUser;

    private EPerson secondDirectorioUser;

    private EPerson institutionUser;

    private Community directorioCommunity;

    private Collection directorioPublications;

    private Group reviewGroup;

    private Group directorioEditorGroup;

    private Group directorioReviewGroup;

    private RelationshipType hasShadowCopy;

    private RelationshipType isCorrectionOf;

    private RelationshipType institutionIsCorrectionOf;

    private RelationshipType isWithdrawOf;

    private RelationshipType institutionIsWithdrawOf;

    private RelationshipType isReinstateOf;

    private RelationshipType institutionIsReinstateOf;

    private RelationshipType isMergedIn;

    private RelationshipType isOriginatedFrom;

    @Before
    public void before() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        EntityType institutionPublicationType = createEntityType("InstitutionPublication");

        EntityType publicationType = createEntityType("Publication");

        hasShadowCopy = createHasShadowCopyRelationship(institutionPublicationType, publicationType);

        isCorrectionOf = createIsCorrectionOfRelationship(publicationType);
        institutionIsCorrectionOf = createIsCorrectionOfRelationship(institutionPublicationType);

        isWithdrawOf = createIsWithdrawOfRelationship(publicationType);
        institutionIsWithdrawOf = createIsWithdrawOfRelationship(institutionPublicationType);
        isReinstateOf = createIsReinstatementOfRelationship(publicationType);
        institutionIsReinstateOf = createIsReinstatementOfRelationship(institutionPublicationType);

        isMergedIn = createIsMergedInRelationship(publicationType);
        isOriginatedFrom = createIsOriginatedFromRelationship(publicationType, institutionPublicationType);

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
            .addMember(secondDirectorioUser)
            .build();

        directorioPublications = CollectionBuilder
            .createCollection(context, directorioSubCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Publications")
            .withRelationshipType("Publication")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", directorioReviewGroup)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        reviewGroup = GroupBuilder.createGroup(context)
            .withName("Reviewer group")
            .addMember(institutionUser)
            .build();

        institutionCollection = createCollection(context, parentCommunity, "123456789/institution-workflow-test")
            .withName("Institution collection")
            .withRelationshipType("InstitutionPublication")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());

        List<String> groupIds = of(directorioReviewGroup.getID().toString(), directorioEditorGroup.getID().toString());
        configurationService.addPropertyValue("directorio.security.policy-groups", groupIds);

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        collectionRoleService.deleteByCollection(context, institutionCollection);
        collectionRoleService.deleteByCollection(context, directorioPublications);
        workflowItemService.deleteByCollection(context, institutionCollection);
        workflowItemService.deleteByCollection(context, directorioPublications);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testItemSubmissionWithInstitutionReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));
        assertThat(getWorkflowItem(shadowItemCopy), notNullValue());

        List<MetadataValue> shadowItemMetadata = shadowItemCopy.getMetadata();
        assertThat(shadowItemMetadata, hasSize(10));
        assertThat(shadowItemMetadata, hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(shadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(shadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.editor", "Test editor")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(), null,
            directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(), null,
            directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem workflowItem = getWorkflowItem(item);
        assertThat(workflowItem, notNullValue());

        List<PoolTask> poolTasks = poolTaskService.find(context, workflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(reviewGroup));

        performActionOnPoolTaskViaRest(institutionUser, poolTask);

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(getWorkspaceItem(item), notNullValue());
        assertThat(reloadItem(shadowItemCopy), nullValue());

    }

    @Test
    public void testItemSubmissionWithConcytecApprove() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItemWithFulltext(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));

        List<MetadataValue> shadowItemMetadata = shadowItemCopy.getMetadata();
        assertThat(shadowItemMetadata, hasSize(10));
        assertThat(shadowItemMetadata, hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(shadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(shadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.editor", "Test editor")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(), null,
            directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(), null,
            directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(APPROVE.name()));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(true));

    }

    @Test
    public void testItemSubmissionWithConcytecEditorReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));

        List<MetadataValue> shadowItemMetadata = shadowItemCopy.getMetadata();
        assertThat(shadowItemCopy.getMetadata(), hasSize(10));

        assertThat(shadowItemMetadata, hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(shadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(shadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.editor", "Test editor")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(), null,
            directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(), null,
            directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

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

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        assertThat(shadowItemCopy, not(equalTo(item)));

        List<MetadataValue> shadowItemMetadata = shadowItemCopy.getMetadata();
        assertThat(shadowItemCopy.getMetadata(), hasSize(10));
        assertThat(shadowItemMetadata, hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(shadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(shadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.editor", "Test editor")));
        assertThat(shadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(), null,
            directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(shadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(), null,
            directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        String directorioTitle = "Submission Item Edited";
        itemService.replaceMetadata(context, shadowItemCopy, "dc", "title", null, null, directorioTitle, null, -1, 0);

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(APPROVE.name()));
        assertThat(item.getMetadata(), hasItem(with("dc.title", ITEM_TITLE)));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", directorioTitle)));

    }

    @Test
    public void testItemCorrectionWithConcytecApproval() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        assertThat(getFirstMetadata(shadowItemCopy, "dc.contributor.author").getAuthority(),
            equalTo("will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3"));
        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();

        Relationship correctionRelation = findRelation(item, institutionIsCorrectionOf);
        assertThat(correctionRelation.getLeftItem(), equalTo(correctionWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Submission Item new title");
        removeEditor(context, correctionWorkspaceItem.getItem());
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(correctionItem), notNullValue());

        Relationship correctionItemShadowCopyRelation = findRelation(correctionItem, hasShadowCopy);
        Item correctionItemShadowCopy = correctionItemShadowCopyRelation.getRightItem();

        Relationship correctedItemShadowCopyRelation = findRelation(correctionItemShadowCopy, isCorrectionOf);
        assertThat(correctedItemShadowCopyRelation, notNullValue());
        assertThat(correctionItemShadowCopy, equalTo(correctedItemShadowCopyRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(correctedItemShadowCopyRelation.getRightItem()));

        List<MetadataValue> correctionShadowItemMetadata = correctionItemShadowCopy.getMetadata();
        assertThat(correctionShadowItemMetadata, hasSize(14));
        assertThat(getFirstMetadata(correctionItemShadowCopy, "dc.contributor.editor"), nullValue());
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.title", "Submission Item new title")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(correctionShadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(correctionShadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(),
            null, directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(),
            null, directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem correctionWorkflowItemShadowCopy = getWorkflowItem(correctionItemShadowCopy);
        assertThat(correctionWorkflowItemShadowCopy, notNullValue());

        claimTaskAndApprove(correctionWorkflowItemShadowCopy, secondDirectorioUser, directorioEditorGroup);

        assertThat(reloadItem(correctionItem), nullValue());
        assertThat(reloadItem(correctionItemShadowCopy), nullValue());

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.getMetadata(), hasItem(with("dc.title", "Submission Item new title")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", "Submission Item new title")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());

    }

    @Test
    public void testItemCorrectionWithConcytecRejection() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItemWithFulltext(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();

        Relationship correctionRelation = findRelation(item, institutionIsCorrectionOf);
        assertThat(correctionRelation.getLeftItem(), equalTo(correctionWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Submission Item new title");
        removeEditor(context, correctionWorkspaceItem.getItem());
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(correctionItem), notNullValue());

        Relationship correctionItemShadowCopyRelation = findRelation(correctionItem, hasShadowCopy);
        Item correctionItemShadowCopy = correctionItemShadowCopyRelation.getRightItem();

        Relationship correctedItemShadowCopyRelation = findRelation(correctionItemShadowCopy, isCorrectionOf);
        assertThat(correctedItemShadowCopyRelation, notNullValue());
        assertThat(correctionItemShadowCopy, equalTo(correctedItemShadowCopyRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(correctedItemShadowCopyRelation.getRightItem()));

        List<MetadataValue> correctionShadowItemMetadata = correctionItemShadowCopy.getMetadata();
        assertThat(getFirstMetadata(correctionItemShadowCopy, "dc.contributor.editor"), nullValue());
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.title", "Submission Item new title")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(correctionShadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(correctionShadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(),
            null, directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(),
            null, directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem correctionWorkflowItemShadowCopy = getWorkflowItem(correctionItemShadowCopy);
        assertThat(correctionWorkflowItemShadowCopy, notNullValue());

        claimTaskAndReject(correctionWorkflowItemShadowCopy, firstDirectorioUser, directorioEditorGroup, "Wrong title");

        assertThat(reloadItem(correctionItem), nullValue());
        assertThat(reloadItem(correctionItemShadowCopy), nullValue());

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.getMetadata(), hasItem(with("dc.title", "Submission Item new title")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.contributor.editor", "Test editor")));

    }

    @Test
    public void testItemCorrectionWithInstitutionRejection() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();

        Relationship correctionRelation = findRelation(item, institutionIsCorrectionOf);
        assertThat(correctionRelation.getLeftItem(), equalTo(correctionWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Submission Item new title");
        removeEditor(context, correctionWorkspaceItem.getItem());
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

        Relationship correctionItemShadowCopyRelation = findRelation(correctionItem, hasShadowCopy);
        Item correctionItemShadowCopy = correctionItemShadowCopyRelation.getRightItem();

        Relationship correctedItemShadowCopyRelation = findRelation(correctionItemShadowCopy, isCorrectionOf);
        assertThat(correctedItemShadowCopyRelation, notNullValue());
        assertThat(correctionItemShadowCopy, equalTo(correctedItemShadowCopyRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(correctedItemShadowCopyRelation.getRightItem()));

        List<MetadataValue> correctionShadowItemMetadata = correctionItemShadowCopy.getMetadata();
        assertThat(getFirstMetadata(correctionItemShadowCopy, "dc.contributor.editor"), nullValue());
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.title", "Submission Item new title")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(correctionShadowItemMetadata, hasItem(with("relationship.type", "Publication")));
        assertThat(correctionShadowItemMetadata, hasItem(with("oairecerif.author.affiliation", "4Science")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.contributor.author", "Mario Rossi", null,
            "will be referenced::SHADOW::9bab4959-c210-4b6d-9d94-ff75cade84c3", 0, 600)));

        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(),
            null, directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(),
            null, directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem correctionWorkflowItem = getWorkflowItem(correctionItem);
        assertThat(correctionWorkflowItem, notNullValue());

        List<PoolTask> poolTasks = poolTaskService.find(context, correctionWorkflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(reviewGroup));

        performActionOnPoolTaskViaRest(institutionUser, poolTask);

        item = reloadItem(item);
        assertThat(item.getMetadata(), hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(item.getMetadata(), hasItem(with("dc.contributor.editor", "Test editor")));
        assertThat(getWorkspaceItem(item), nullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.contributor.editor", "Test editor")));

        correctionItem = reloadItem(correctionItem);
        assertThat(correctionItem, notNullValue());
        assertThat(getWorkspaceItem(correctionItem), notNullValue());
        assertThat(getWorkflowItem(correctionItem), nullValue());

        correctionItemShadowCopy = reloadItem(correctionItemShadowCopy);
        assertThat(correctionItemShadowCopy, nullValue());

    }

    @Test
    public void testItemSubmissionWithConcytecEditAndFollowingCorrection() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);
        assertThat(shadowWorkflowItemCopy, notNullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        itemService.replaceMetadata(context, shadowItemCopy, "dc", "date", "issued", null, "2020-12-31", null, -1, 0);
        itemService.replaceMetadata(context, shadowItemCopy, "dc", "title", null, null, "Concytec title", null, -1, 0);

        claimTaskAndApprove(shadowWorkflowItemCopy, firstDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(item.getMetadata(), hasItem(with("dc.title", ITEM_TITLE)));
        assertThat(item.getMetadata(), hasItem(with("dc.date.issued", "2017-10-17")));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", "Concytec title")));
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.date.issued", "2020-12-31")));

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();
        assertThat(correctionItem.getMetadata(), hasItem(with("dc.date.issued", "2017-10-17")));

        Relationship correctionRelation = findRelation(item, institutionIsCorrectionOf);
        assertThat(correctionRelation.getLeftItem(), equalTo(correctionWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Submission Item new title");
        removeEditor(context, correctionWorkspaceItem.getItem());
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

        Relationship correctionItemShadowCopyRelation = findRelation(correctionItem, hasShadowCopy);
        Item correctionItemShadowCopy = correctionItemShadowCopyRelation.getRightItem();

        Relationship correctedItemShadowCopyRelation = findRelation(correctionItemShadowCopy, isCorrectionOf);
        assertThat(correctedItemShadowCopyRelation, notNullValue());
        assertThat(correctionItemShadowCopy, equalTo(correctedItemShadowCopyRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(correctedItemShadowCopyRelation.getRightItem()));

        List<MetadataValue> correctionShadowItemMetadata = correctionItemShadowCopy.getMetadata();
        assertThat(getFirstMetadata(correctionItemShadowCopy, "dc.contributor.editor"), nullValue());
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.title", "Submission Item new title")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.date.issued", "2020-12-31")));

        XmlWorkflowItem correctionWorkflowItem = getWorkflowItem(correctionItem);
        assertThat(correctionWorkflowItem, notNullValue());

        XmlWorkflowItem correctionWorkflowItemShadowCopy = getWorkflowItem(correctionItemShadowCopy);
        assertThat(correctionWorkflowItemShadowCopy, notNullValue());

        claimTaskAndApprove(correctionWorkflowItemShadowCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.getMetadata(), hasItem(with("dc.title", "Submission Item new title")));
        assertThat(item.getMetadata(), hasItem(with("dc.date.issued", "2017-10-17")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());
        assertThat(getWorkspaceItem(item), nullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", "Submission Item new title")));
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.date.issued", "2020-12-31")));
        assertThat(getFirstMetadata(shadowItemCopy, "dc.contributor.editor"), nullValue());

        correctionItem = reloadItem(correctionItem);
        assertThat(correctionItem, nullValue());

        correctionItemShadowCopy = reloadItem(correctionItemShadowCopy);
        assertThat(correctionItemShadowCopy, nullValue());

    }

    @Test
    public void testItemSubmissionWithConcytecAssign() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndAssignTo(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup, firstDirectorioUser);

        List<ClaimedTask> tasks = claimedTaskService.findByWorkflowItem(context, shadowWorkflowItemCopy);
        assertThat(tasks, hasSize(1));
        assertThat(tasks.get(0).getOwner(), equalTo(firstDirectorioUser));

        assertThat(reloadItem(item).isArchived(), is(false));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(false));

        approve(shadowWorkflowItemCopy, firstDirectorioUser);

        assertThat(reloadItem(item).isArchived(), is(true));
        assertThat(reloadItem(shadowItemCopy).isArchived(), is(true));

    }

    @Test
    public void testItemDirectSubmissionInTheDirectorio() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(directorioPublications);

        Item item = workspaceItem.getItem();
        assertThat(item.isArchived(), is(false));

        XmlWorkflowItem workflowItem = workflowService.start(context, workspaceItem);

        claimTaskAndApprove(workflowItem, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(getConcytecFeedbackMetadataValue(item), equalTo(APPROVE.name()));

    }

    @Test
    public void testItemCorrectionWithoutShadowCopyInTheDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, institutionCollection)
            .withTitle("Test publication")
            .withEditor("Test editor")
            .withIssueDate("2021-01-04")
            .build();
        context.restoreAuthSystemState();

        WorkspaceItem correctionWorkspaceItem = requestForItemCorrection(admin, item);
        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();

        Relationship correctionRelation = findRelation(item, institutionIsCorrectionOf);
        assertThat(correctionRelation.getLeftItem(), equalTo(correctionWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        replaceTitle(context, correctionWorkspaceItem.getItem(), "Test publication new title");
        removeEditor(context, correctionWorkspaceItem.getItem());
        workflowService.start(context, correctionWorkspaceItem);
        context.restoreAuthSystemState();

        Relationship itemShadowCopyRelation = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = itemShadowCopyRelation.getRightItem();
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        assertThat(getWorkflowItem(correctionItem), notNullValue());

        Relationship correctionItemShadowCopyRelation = findRelation(correctionItem, hasShadowCopy);
        Item correctionItemShadowCopy = correctionItemShadowCopyRelation.getRightItem();

        Relationship correctedItemShadowCopyRelation = findRelation(correctionItemShadowCopy, isCorrectionOf);
        assertThat(correctedItemShadowCopyRelation, notNullValue());
        assertThat(correctionItemShadowCopy, equalTo(correctedItemShadowCopyRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(correctedItemShadowCopyRelation.getRightItem()));

        List<MetadataValue> correctionShadowItemMetadata = correctionItemShadowCopy.getMetadata();
        assertThat(correctionItem.getMetadata(), hasSize(6));

        assertThat(getFirstMetadata(correctionItemShadowCopy, "dc.contributor.editor"), nullValue());
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.title", "Test publication new title")));
        assertThat(correctionShadowItemMetadata, hasItem(with("dc.date.issued", "2021-01-04")));
        assertThat(correctionShadowItemMetadata, hasItem(with("relationship.type", "Publication")));

        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioReviewGroup.getName(),
            null, directorioReviewGroup.getID().toString(), 0, 600)));
        assertThat(correctionShadowItemMetadata, hasItem(with("cris.policy.group", directorioEditorGroup.getName(),
            null, directorioEditorGroup.getID().toString(), 1, 600)));

        XmlWorkflowItem correctionWorkflowItemShadowCopy = getWorkflowItem(correctionItemShadowCopy);
        assertThat(correctionWorkflowItemShadowCopy, notNullValue());

        claimTaskAndApprove(correctionWorkflowItemShadowCopy, secondDirectorioUser, directorioEditorGroup);

        assertThat(reloadItem(correctionItem), nullValue());
        assertThat(reloadItem(correctionItemShadowCopy), nullValue());

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.getMetadata(), hasItem(with("dc.title", "Test publication new title")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.getMetadata(), hasItem(with("dc.title", "Test publication new title")));
        assertThat(getFirstMetadata(item, "dc.contributor.editor"), nullValue());
    }

    @Test
    public void testPublicationAndAuthorSubmission() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType institutionPersonType = createEntityType("InstitutionPerson");

        EntityType personType = createEntityType("Person");

        RelationshipType personHasShadowCopy = createHasShadowCopyRelationship(institutionPersonType, personType);

        createIsCorrectionOfRelationship(personType);
        createIsCorrectionOfRelationship(institutionPersonType);
        createIsWithdrawOfRelationship(personType);
        createIsWithdrawOfRelationship(institutionPersonType);
        createIsReinstatementOfRelationship(personType);
        createIsReinstatementOfRelationship(institutionPersonType);

        Collection directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withRelationshipType("Person")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        Collection institutionPersons = createCollection(context, parentCommunity)
            .withWorkflow("institutionWorkflow")
            .withName("Institution person collection")
            .withRelationshipType("InstitutionPerson")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        WorkspaceItem personWorkspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, institutionPersons)
            .withTitle("White, Walter")
            .build();

        Item personItem = personWorkspaceItem.getItem();
        String personItemId = personItem.getID().toString();

        WorkspaceItem publicationWorkspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, institutionCollection)
            .withTitle("Test publication")
            .withAuthor("White, Walter", personItemId)
            .build();

        Item publicationItem = publicationWorkspaceItem.getItem();

        context.restoreAuthSystemState();

        workflowService.start(context, publicationWorkspaceItem);

        Relationship publicationRelationship = findRelation(publicationItem, hasShadowCopy);
        Item publicationItemCopy = publicationRelationship.getRightItem();

        XmlWorkflowItem publicationWorkflowItemCopy = getWorkflowItem(publicationItemCopy);

        claimTaskAndApprove(publicationWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        publicationItem = reloadItem(publicationItem);
        assertThat(publicationItem.isArchived(), is(true));

        assertThat(publicationItem.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", null,
            personItemId, 0, 600)));

        publicationItemCopy = reloadItem(publicationItemCopy);
        assertThat(publicationItemCopy.isArchived(), is(true));

        assertThat(publicationItemCopy.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", null,
            "will be referenced::SHADOW::" + personItemId, 0, 600)));

        personWorkspaceItem = context.reloadEntity(personWorkspaceItem);
        workflowService.start(context, personWorkspaceItem);

        Relationship personRelationship = findRelation(personItem, personHasShadowCopy);
        Item personItemCopy = personRelationship.getRightItem();

        XmlWorkflowItem personWorkflowItemCopy = getWorkflowItem(personItemCopy);

        claimTaskAndApprove(personWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        personItem = reloadItem(personItem);
        assertThat(personItem.isArchived(), is(true));

        personItemCopy = reloadItem(personItemCopy);
        assertThat(personItemCopy.isArchived(), is(true));
        assertThat(personItemCopy.getOwningCollection(), is(directorioPersons));

        publicationItemCopy = reloadItem(publicationItemCopy);
        assertThat(publicationItemCopy.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", null,
            personItemCopy.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemWithdrawWithConcytecApprove() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem withdrawnWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(withdrawnWorkspaceItem, notNullValue());

        Item withdrawnItem = withdrawnWorkspaceItem.getItem();

        Relationship withdrawnRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(withdrawnRelation.getLeftItem(), equalTo(withdrawnWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, withdrawnWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(withdrawnItem), notNullValue());

        Relationship withdrawnItemShadowCopyRelation = findRelation(withdrawnItem, hasShadowCopy);
        Item withdrawnItemShadowCopy = withdrawnItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(withdrawnItemShadowCopy, isWithdrawOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(withdrawnItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem withdrawnWorkflowItem = getWorkflowItem(withdrawnItem);
        assertThat(withdrawnWorkflowItem, notNullValue());

        XmlWorkflowItem withdrawnWorkflowItemShadowCopy = getWorkflowItem(withdrawnItemShadowCopy);
        assertThat(withdrawnWorkflowItemShadowCopy, notNullValue());

        claimTaskAndApprove(withdrawnWorkflowItemShadowCopy, secondDirectorioUser, directorioReviewGroup);

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        withdrawnItem = reloadItem(withdrawnItem);
        assertThat(withdrawnItem, nullValue());

        withdrawnItemShadowCopy = reloadItem(withdrawnItemShadowCopy);
        assertThat(withdrawnItemShadowCopy, nullValue());
    }

    @Test
    public void testItemWithdrawWithConcytecReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));

        WorkspaceItem withdrawnWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(withdrawnWorkspaceItem, notNullValue());

        Item withdrawnItem = withdrawnWorkspaceItem.getItem();

        Relationship withdrawnRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(withdrawnRelation.getLeftItem(), equalTo(withdrawnWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, withdrawnWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(withdrawnItem), notNullValue());

        Relationship withdrawnItemShadowCopyRelation = findRelation(withdrawnItem, hasShadowCopy);
        Item withdrawnItemShadowCopy = withdrawnItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(withdrawnItemShadowCopy, isWithdrawOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(withdrawnItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem withdrawnWorkflowItem = getWorkflowItem(withdrawnItem);
        assertThat(withdrawnWorkflowItem, notNullValue());

        XmlWorkflowItem withdrawnWorkflowItemShadowCopy = getWorkflowItem(withdrawnItemShadowCopy);
        assertThat(withdrawnWorkflowItemShadowCopy, notNullValue());

        claimTaskAndReject(withdrawnWorkflowItemShadowCopy, secondDirectorioUser, directorioReviewGroup, "to keep");

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        withdrawnItem = reloadItem(withdrawnItem);
        assertThat(withdrawnItem, nullValue());

        withdrawnItemShadowCopy = reloadItem(withdrawnItemShadowCopy);
        assertThat(withdrawnItemShadowCopy, nullValue());
    }

    @Test
    public void testItemWithdrawWithInstitutionReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        WorkspaceItem withdrawnWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(withdrawnWorkspaceItem, notNullValue());

        Item withdrawnItem = withdrawnWorkspaceItem.getItem();

        Relationship withdrawnRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(withdrawnRelation.getLeftItem(), equalTo(withdrawnWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, withdrawnWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(withdrawnItem), notNullValue());

        Relationship withdrawnItemShadowCopyRelation = findRelation(withdrawnItem, hasShadowCopy);
        Item withdrawnItemShadowCopy = withdrawnItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(withdrawnItemShadowCopy, isWithdrawOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(withdrawnItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem withdrawnWorkflowItem = getWorkflowItem(withdrawnItem);
        assertThat(withdrawnWorkflowItem, notNullValue());

        XmlWorkflowItem withdrawnWorkflowItemShadowCopy = getWorkflowItem(withdrawnItemShadowCopy);
        assertThat(withdrawnWorkflowItemShadowCopy, notNullValue());

        List<PoolTask> poolTasks = poolTaskService.find(context, withdrawnWorkflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(reviewGroup));

        performActionOnPoolTaskViaRest(institutionUser, poolTask);

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        withdrawnItem = reloadItem(withdrawnItem);
        assertThat(withdrawnItem, nullValue());

        withdrawnItemShadowCopy = reloadItem(withdrawnItemShadowCopy);
        assertThat(withdrawnItemShadowCopy, nullValue());
    }

    @Test
    public void testItemWithdrawWithoutItemInDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, institutionCollection)
            .withTitle("Test publication")
            .withEditor("Test editor")
            .withIssueDate("2021-01-04")
            .build();
        context.restoreAuthSystemState();

        WorkspaceItem withdrawnWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(withdrawnWorkspaceItem, notNullValue());

        Item withdrawnItem = withdrawnWorkspaceItem.getItem();

        Relationship withdrawnRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(withdrawnRelation.getLeftItem(), equalTo(withdrawnWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, withdrawnWorkspaceItem);
        context.restoreAuthSystemState();

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        withdrawnItem = reloadItem(withdrawnItem);
        assertThat(withdrawnItem, nullValue());

    }

    @Test
    public void testItemWithdrawWithAlreadyWithdrawnItemInDirectorio() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        context.turnOffAuthorisationSystem();
        itemService.withdraw(context, shadowItemCopy);
        context.restoreAuthSystemState();

        WorkspaceItem withdrawnWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(withdrawnWorkspaceItem, notNullValue());

        Item withdrawnItem = withdrawnWorkspaceItem.getItem();

        Relationship withdrawnRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(withdrawnRelation.getLeftItem(), equalTo(withdrawnWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, withdrawnWorkspaceItem);
        context.restoreAuthSystemState();

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        withdrawnItem = reloadItem(withdrawnItem);
        assertThat(withdrawnItem, nullValue());

    }

    @Test
    public void testItemReinstateWithConcytecApprove() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        withdrawItems(item, shadowItemCopy);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        WorkspaceItem reinstateWorkspaceItem = requestForItemReinstate(admin, item);
        assertThat(reinstateWorkspaceItem, notNullValue());

        Item reinstateItem = reinstateWorkspaceItem.getItem();

        Relationship reinstateRelation = findRelation(item, institutionIsReinstateOf);
        assertThat(reinstateRelation.getLeftItem(), equalTo(reinstateWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, reinstateWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(reinstateItem), notNullValue());

        Relationship reinstateItemShadowCopyRelation = findRelation(reinstateItem, hasShadowCopy);
        Item reinstateItemShadowCopy = reinstateItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(reinstateItemShadowCopy, isReinstateOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(reinstateItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem reinstateWorkflowItem = getWorkflowItem(reinstateItem);
        assertThat(reinstateWorkflowItem, notNullValue());

        XmlWorkflowItem reinstateWorkflowItemShadowCopy = getWorkflowItem(reinstateItemShadowCopy);
        assertThat(reinstateWorkflowItemShadowCopy, notNullValue());

        claimTaskAndApprove(reinstateWorkflowItemShadowCopy, secondDirectorioUser, directorioReviewGroup);

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        reinstateItem = reloadItem(reinstateItem);
        assertThat(reinstateItem, nullValue());

        reinstateItemShadowCopy = reloadItem(reinstateItemShadowCopy);
        assertThat(reinstateItemShadowCopy, nullValue());
    }

    @Test
    public void testItemReinstateWithConcytecReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        withdrawItems(item, shadowItemCopy);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        WorkspaceItem reinstateWorkspaceItem = requestForItemReinstate(admin, item);
        assertThat(reinstateWorkspaceItem, notNullValue());

        Item reinstateItem = reinstateWorkspaceItem.getItem();

        Relationship reinstateRelation = findRelation(item, institutionIsReinstateOf);
        assertThat(reinstateRelation.getLeftItem(), equalTo(reinstateWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, reinstateWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(reinstateItem), notNullValue());

        Relationship reinstateItemShadowCopyRelation = findRelation(reinstateItem, hasShadowCopy);
        Item reinstateItemShadowCopy = reinstateItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(reinstateItemShadowCopy, isReinstateOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(reinstateItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem reinstateWorkflowItem = getWorkflowItem(reinstateItem);
        assertThat(reinstateWorkflowItem, notNullValue());

        XmlWorkflowItem reinstateWorkflowItemShadowCopy = getWorkflowItem(reinstateItemShadowCopy);
        assertThat(reinstateWorkflowItemShadowCopy, notNullValue());

        claimTaskAndReject(reinstateWorkflowItemShadowCopy, secondDirectorioUser, directorioReviewGroup, "to remove");

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        reinstateItem = reloadItem(reinstateItem);
        assertThat(reinstateItem, nullValue());

        reinstateItemShadowCopy = reloadItem(reinstateItemShadowCopy);
        assertThat(reinstateItemShadowCopy, nullValue());
    }

    @Test
    public void testItemReinstateWithInstitutionReject() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        withdrawItems(item, shadowItemCopy);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        WorkspaceItem reinstateWorkspaceItem = requestForItemReinstate(admin, item);
        assertThat(reinstateWorkspaceItem, notNullValue());

        Item reinstateItem = reinstateWorkspaceItem.getItem();

        Relationship reinstateRelation = findRelation(item, institutionIsReinstateOf);
        assertThat(reinstateRelation.getLeftItem(), equalTo(reinstateWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, reinstateWorkspaceItem);
        context.restoreAuthSystemState();

        assertThat(getWorkflowItem(reinstateItem), notNullValue());

        Relationship reinstateItemShadowCopyRelation = findRelation(reinstateItem, hasShadowCopy);
        Item reinstateItemShadowCopy = reinstateItemShadowCopyRelation.getRightItem();

        Relationship itemShadowCopyToBeWithdrawRelation = findRelation(reinstateItemShadowCopy, isReinstateOf);
        assertThat(itemShadowCopyToBeWithdrawRelation, notNullValue());
        assertThat(reinstateItemShadowCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getLeftItem()));
        assertThat(shadowItemCopy, equalTo(itemShadowCopyToBeWithdrawRelation.getRightItem()));

        XmlWorkflowItem reinstateWorkflowItem = getWorkflowItem(reinstateItem);
        assertThat(reinstateWorkflowItem, notNullValue());

        XmlWorkflowItem reinstateWorkflowItemShadowCopy = getWorkflowItem(reinstateItemShadowCopy);
        assertThat(reinstateWorkflowItemShadowCopy, notNullValue());

        List<PoolTask> poolTasks = poolTaskService.find(context, reinstateWorkflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(reviewGroup));

        performActionOnPoolTaskViaRest(institutionUser, poolTask);

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        reinstateItem = reloadItem(reinstateItem);
        assertThat(reinstateItem, nullValue());

        reinstateItemShadowCopy = reloadItem(reinstateItemShadowCopy);
        assertThat(reinstateItemShadowCopy, nullValue());
    }

    @Test
    public void testItemReinstateWithoutItemInDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, institutionCollection)
            .withTitle("Test publication")
            .withEditor("Test editor")
            .withIssueDate("2021-01-04")
            .withdrawn()
            .build();
        context.restoreAuthSystemState();

        WorkspaceItem reinstateWorkspaceItem = requestForItemWithdraw(admin, item);
        assertThat(reinstateWorkspaceItem, notNullValue());

        Item reinstateItem = reinstateWorkspaceItem.getItem();

        Relationship reinstateRelation = findRelation(item, institutionIsWithdrawOf);
        assertThat(reinstateRelation.getLeftItem(), equalTo(reinstateWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, reinstateWorkspaceItem);
        context.restoreAuthSystemState();

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        reinstateItem = reloadItem(reinstateItem);
        assertThat(reinstateItem, nullValue());

    }

    @Test
    public void testItemReinstateWithNoWithdrawnItemInDirectorio() throws Exception {

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        claimTaskAndApprove(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        withdrawItems(item);

        item = reloadItem(item);
        assertThat(item.isArchived(), is(false));
        assertThat(item.isWithdrawn(), is(true));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        WorkspaceItem reinstateWorkspaceItem = requestForItemReinstate(admin, item);
        assertThat(reinstateWorkspaceItem, notNullValue());

        Item reinstateItem = reinstateWorkspaceItem.getItem();

        Relationship reinstateRelation = findRelation(item, institutionIsReinstateOf);
        assertThat(reinstateRelation.getLeftItem(), equalTo(reinstateWorkspaceItem.getItem()));

        context.turnOffAuthorisationSystem();
        workflowService.start(context, reinstateWorkspaceItem);
        context.restoreAuthSystemState();

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        reinstateItem = reloadItem(reinstateItem);
        assertThat(reinstateItem, nullValue());

    }

    @Test
    public void testItemSubmissionWithEntityLookup() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withRelationshipType("Person")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        Item firstPerson = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("Mario Rossi")
            .withOrcidIdentifier("0000-0002-1825-0097")
            .build();

        Item secondPerson = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("Walter White")
            .withScopusAuthorIdentifier("SC-01")
            .build();

        Item thirdPerson = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("Jesse Pinkman")
            .withResearcherIdentifier("R-01")
            .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, institutionCollection)
            .withTitle("Test item")
            .withIssueDate("2017-10-17")
            .withAuthor("Mario Rossi")
            .withAuthorAffilitation("4Science")
            .withAuthorOrcid("0000-0002-1825-0097")
            .withAuthorScopusIdentifier(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthorResearcherId(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorOrcid(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthorScopusIdentifier("SC-01")
            .withAuthorResearcherId(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Jesse Pinkman")
            .withAuthorOrcid(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthorScopusIdentifier(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthorResearcherId("R-01")
            .withEditor("Test editor")
            .grantLicense()
            .build();

        context.restoreAuthSystemState();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        List<MetadataValue> metadata = shadowItemCopy.getMetadata();
        String firstPersonId = firstPerson.getID().toString();
        String secondPersonId = secondPerson.getID().toString();
        String thirdPersonId = thirdPerson.getID().toString();

        assertThat(metadata, hasItem(with("dc.title", "Test item")));
        assertThat(metadata, hasItem(with("dc.contributor.author", "Mario Rossi", null, firstPersonId, 0, 600)));
        assertThat(metadata, hasItem(with("dc.contributor.author", "Walter White", null, secondPersonId, 1, 600)));
        assertThat(metadata, hasItem(with("dc.contributor.author", "Jesse Pinkman", null, thirdPersonId, 2, 600)));
    }

    @Test
    public void testItemSubmissionWithDuplicationVerificationAndItemApproval() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem institutionWorkspaceItem = createWorkspaceItem(institutionCollection);

        workflowService.start(context, institutionWorkspaceItem);

        Item institutionItem = institutionWorkspaceItem.getItem();
        Item duplicationItem = findRelation(institutionItem, hasShadowCopy).getRightItem();

        claimTaskAndApprove(getWorkflowItem(duplicationItem), secondDirectorioUser, directorioEditorGroup);

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        context.restoreAuthSystemState();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Item shadowItemCopy = findRelation(item, hasShadowCopy).getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        ClaimedTask claimedTask = claimTask(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", claimedTask.getID())
                .param("submit_approve", "submit_approve")
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isUnprocessableEntity());

        String patchBody = patchDetectDuplication(duplicationItem.getID().toString(), "verify", "note");

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(patch("/api/workflow/workflowitems/" + shadowWorkflowItemCopy.getID())
            .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        approve(shadowWorkflowItemCopy, secondDirectorioUser);

        duplicationItem = reloadItem(duplicationItem);
        assertThat(duplicationItem, notNullValue());
        assertThat(duplicationItem.isArchived(), is(false));
        assertThat(duplicationItem.isWithdrawn(), is(true));

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        Relationship isMergedInRelation = findRelation(duplicationItem, isMergedIn);
        assertThat(isMergedInRelation.getLeftItem().getID(), is(duplicationItem.getID()));
        assertThat(isMergedInRelation.getRightItem().getID(), is(shadowItemCopy.getID()));

        Relationship isOriginatedFromRelation = findRelation(shadowItemCopy, isOriginatedFrom);
        assertThat(isOriginatedFromRelation.getLeftItem().getID(), is(shadowItemCopy.getID()));
        assertThat(isOriginatedFromRelation.getRightItem().getID(), is(institutionItem.getID()));

    }

    @Test
    public void testItemSubmissionWithDuplicationVerificationAndItemRejection() throws Exception {

        context.turnOffAuthorisationSystem();

        Item duplicationItem = ItemBuilder.createItem(context, directorioPublications)
            .withTitle(ITEM_TITLE)
            .build();

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        context.restoreAuthSystemState();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        String patchBody = patchDetectDuplication(duplicationItem.getID().toString(), "verify", "note");

        ClaimedTask claimTask = claimTask(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(patch("/api/workflow/workflowitems/" + shadowWorkflowItemCopy.getID())
                .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        rejectClaimedTaskViaRest(secondDirectorioUser, claimTask, "Choose other item");

        duplicationItem = reloadItem(duplicationItem);
        assertThat(duplicationItem, notNullValue());
        assertThat(duplicationItem.isArchived(), is(true));
        assertThat(duplicationItem.isWithdrawn(), is(false));

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        Relationship isMergedInRelation = findRelation(duplicationItem, isMergedIn);
        assertThat(isMergedInRelation.getLeftItem().getID(), is(shadowItemCopy.getID()));
        assertThat(isMergedInRelation.getRightItem().getID(), is(duplicationItem.getID()));

        Relationship isOriginatedFromRelation = findRelation(duplicationItem, isOriginatedFrom);
        assertThat(isOriginatedFromRelation.getLeftItem().getID(), is(duplicationItem.getID()));
        assertThat(isOriginatedFromRelation.getRightItem().getID(), is(item.getID()));

    }

    @Test
    public void testItemSubmissionWithDuplicationRejectionAndItemApproval() throws Exception {

        context.turnOffAuthorisationSystem();

        Item duplicationItem = ItemBuilder.createItem(context, directorioPublications)
            .withTitle(ITEM_TITLE)
            .build();

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        context.restoreAuthSystemState();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        ClaimedTask claimedTask = claimTask(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", claimedTask.getID())
                .param("submit_approve", "submit_approve")
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isUnprocessableEntity());

        String patchBody = patchDetectDuplication(duplicationItem.getID().toString(), "reject", null);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(patch("/api/workflow/workflowitems/" + shadowWorkflowItemCopy.getID())
                .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        approve(shadowWorkflowItemCopy, secondDirectorioUser);

        duplicationItem = reloadItem(duplicationItem);
        assertThat(duplicationItem, notNullValue());
        assertThat(duplicationItem.isArchived(), is(true));
        assertThat(duplicationItem.isWithdrawn(), is(false));

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(true));
        assertThat(shadowItemCopy.isWithdrawn(), is(false));

        assertThat(findRelations(item, isMergedIn), empty());
        assertThat(findRelations(shadowItemCopy, isOriginatedFrom), empty());
        assertThat(findRelations(duplicationItem, isOriginatedFrom), empty());

    }

    @Test
    public void testItemSubmissionWithDuplicationRejectionAndItemRejection() throws Exception {

        context.turnOffAuthorisationSystem();

        Item duplicationItem = ItemBuilder.createItem(context, directorioPublications)
            .withTitle(ITEM_TITLE)
            .build();

        WorkspaceItem workspaceItem = createWorkspaceItem(institutionCollection);

        context.restoreAuthSystemState();

        workflowService.start(context, workspaceItem);

        Item item = workspaceItem.getItem();
        assertThat(getWorkspaceItem(item), nullValue());

        Relationship relationship = findRelation(item, hasShadowCopy);
        Item shadowItemCopy = relationship.getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(shadowItemCopy);

        String patchBody = patchDetectDuplication(duplicationItem.getID().toString(), "reject", null);

        ClaimedTask claimTask = claimTask(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(patch("/api/workflow/workflowitems/" + shadowWorkflowItemCopy.getID())
                .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        rejectClaimedTaskViaRest(secondDirectorioUser, claimTask, "Choose other item");

        duplicationItem = reloadItem(duplicationItem);
        assertThat(duplicationItem, notNullValue());
        assertThat(duplicationItem.isArchived(), is(true));
        assertThat(duplicationItem.isWithdrawn(), is(false));

        item = reloadItem(item);
        assertThat(item, notNullValue());
        assertThat(item.isArchived(), is(true));
        assertThat(item.isWithdrawn(), is(false));

        shadowItemCopy = reloadItem(shadowItemCopy);
        assertThat(shadowItemCopy, notNullValue());
        assertThat(shadowItemCopy.isArchived(), is(false));
        assertThat(shadowItemCopy.isWithdrawn(), is(true));

        assertThat(findRelations(item, isMergedIn), empty());
        assertThat(findRelations(shadowItemCopy, isOriginatedFrom), empty());
        assertThat(findRelations(duplicationItem, isOriginatedFrom), empty());

    }

    @Test
    public void testMergeOfItemLinkedToAnotherItemByAuthority() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType institutionPersonType = createEntityType("InstitutionPerson");

        EntityType personType = createEntityType("Person");

        RelationshipType personHasShadowCopy = createHasShadowCopyRelationship(institutionPersonType, personType);
        createIsMergedInRelationship(personType);
        createIsOriginatedFromRelationship(institutionPersonType, personType);

        Collection directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withRelationshipType("Person")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        Item personItem = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        Item publicationItem = ItemBuilder.createItem(context, directorioPublications)
            .withTitle("Test publication")
            .withAuthor("White, Walter", personItem.getID().toString())
            .build();

        Collection institutionPersons = createCollection(context, parentCommunity)
            .withWorkflow("institutionWorkflow")
            .withName("Institution person collection")
            .withRelationshipType("InstitutionPerson")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        WorkspaceItem newPersonWorkspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, institutionPersons)
            .withTitle("White, Walter")
            .build();

        context.restoreAuthSystemState();

        workflowService.start(context, newPersonWorkspaceItem);

        Item newPersonItem = newPersonWorkspaceItem.getItem();
        assertThat(getWorkspaceItem(newPersonItem), nullValue());

        Item newPersonShadowItemCopy = findRelation(newPersonItem, personHasShadowCopy).getRightItem();

        XmlWorkflowItem shadowWorkflowItemCopy = getWorkflowItem(newPersonShadowItemCopy);

        ClaimedTask claimedTask = claimTask(shadowWorkflowItemCopy, secondDirectorioUser, directorioEditorGroup);

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", claimedTask.getID())
                .param("submit_approve", "submit_approve")
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isUnprocessableEntity());

        String patchBody = patchDetectDuplication(personItem.getID().toString(), "verify", "note");

        getClient(getAuthToken(secondDirectorioUser.getEmail(), password))
            .perform(patch("/api/workflow/workflowitems/" + shadowWorkflowItemCopy.getID())
                .content(patchBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        approveClaimedTaskViaRest(secondDirectorioUser, claimedTask);

        personItem = reloadItem(personItem);
        assertThat(personItem, notNullValue());
        assertThat(personItem.isArchived(), is(false));
        assertThat(personItem.isWithdrawn(), is(true));

        newPersonItem = reloadItem(newPersonItem);
        assertThat(newPersonItem, notNullValue());
        assertThat(newPersonItem.isArchived(), is(true));
        assertThat(newPersonItem.isWithdrawn(), is(false));

        publicationItem = reloadItem(publicationItem);
        assertThat(publicationItem, notNullValue());
        assertThat(publicationItem.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
            null, newPersonShadowItemCopy.getID().toString(), 0, 600)));
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType institutionType, EntityType directorioType) {
        return createRelationshipTypeBuilder(context, institutionType, directorioType, HAS_SHADOW_COPY_RELATIONSHIP,
            IS_SHADOW_COPY_RELATIONSHIP, 0, 1, 0, 1).build();
    }

    private RelationshipType createIsOriginatedFromRelationship(EntityType institutionType, EntityType directorioType) {
        return createRelationshipTypeBuilder(context, institutionType, directorioType,
            IS_ORIGINATED_FROM_IN_RELATIONSHIP, IS_ORIGIN_OF_RELATIONSHIP, 0, null, 0, 1).build();
    }

    private RelationshipType createIsWithdrawOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, IS_WITHDRAW_OF_ITEM_RELATIONSHIP, IS_WITHDRAWN_BY_ITEM_RELATIONSHIP, 0, 1, 0, 1).build();
    }

    private RelationshipType createIsReinstatementOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, IS_REINSTATEMENT_OF_ITEM_RELATIONSHIP, IS_REINSTATED_BY_ITEM_RELATIONSHIP, 0, 1, 0, 1).build();
    }

    private RelationshipType createIsCorrectionOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1).build();
    }

    private RelationshipType createIsMergedInRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, IS_MERGED_IN_RELATIONSHIP, IS_ENRICHED_BY_RELATIONSHIP, 0, 1, 0, null).build();
    }

    private void claimTaskAndApprove(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user, expectedGroup);
        approveClaimedTaskViaRest(user, claimedTask);
    }

    private void claimTaskAndReject(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup, String reason)
        throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user, expectedGroup);
        rejectClaimedTaskViaRest(user, claimedTask, reason);
    }

    private void claimTaskAndAssignTo(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup,
        EPerson userToAssign) throws Exception {
        ClaimedTask claimedTask = claimTask(workflowItem, user, expectedGroup);
        assignClaimedTaskViaRest(user, claimedTask, userToAssign);
    }

    private ClaimedTask claimTask(XmlWorkflowItem workflowItem, EPerson user, Group expectedGroup)
        throws SQLException, Exception {
        List<PoolTask> poolTasks = poolTaskService.find(context, workflowItem);
        assertThat(poolTasks, hasSize(1));

        PoolTask poolTask = poolTasks.get(0);
        assertThat(poolTask.getGroup().getMemberGroups(), contains(expectedGroup));

        performActionOnPoolTaskViaRest(user, poolTask);

        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);
        assertThat(claimedTask, notNullValue());
        return claimedTask;
    }

    private void approve(XmlWorkflowItem workflowItem, EPerson user) throws Exception {
        ClaimedTask claimedTask = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, user);
        assertThat(claimedTask, notNullValue());
        approveClaimedTaskViaRest(user, claimedTask);
    }

    private WorkspaceItem createWorkspaceItem(Collection collection) throws IOException {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle(ITEM_TITLE)
            .withIssueDate("2017-10-17")
            .withAuthor("Mario Rossi", "9bab4959-c210-4b6d-9d94-ff75cade84c3")
            .withAuthorAffilitation("4Science")
            .withEditor("Test editor")
            .grantLicense()
            .build();
    }

    private WorkspaceItem createWorkspaceItemWithFulltext(Collection collection) throws IOException {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle(ITEM_TITLE)
            .withIssueDate("2017-10-17")
            .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", simpleArticle.getInputStream())
            .withAuthor("Mario Rossi", "9bab4959-c210-4b6d-9d94-ff75cade84c3")
            .withAuthorAffilitation("4Science")
            .withEditor("Test editor")
            .grantLicense()
            .build();
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

    private void assignClaimedTaskViaRest(EPerson user, ClaimedTask task, EPerson userToAssign) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_assign", "submit_assign");
        params.add("user", userToAssign.getID().toString());
        performActionOnClaimedTaskViaRest(user, task, params);
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

    private String getConcytecCommentMetadataValue(Item item) {
        return itemService.getMetadataFirstValue(item, "perucris", "concytec", "comment", ANY);
    }

    private String getConcytecFeedbackMetadataValue(Item item) {
        return itemService.getMetadataFirstValue(item, "perucris", "concytec", "feedback", ANY);
    }

    private void replaceTitle(Context context, Item item, String newTitle) throws SQLException, AuthorizeException {
        itemService.replaceMetadata(context, item, "dc", "title", null, null, newTitle, null, -1, 0);
        itemService.update(context, item);
    }

    private MetadataValue getFirstMetadata(Item item, String metadataField) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        return CollectionUtils.isNotEmpty(values) ? values.get(0) : null;
    }

    private void removeEditor(Context context, Item item) throws SQLException {
        itemService.removeMetadataValues(context, item, "dc", "contributor", "editor", Item.ANY);
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

    private Relationship findRelation(Item item, RelationshipType type) throws SQLException {
        List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context, item, type);
        assertThat(relationships, hasSize(1));
        return relationships.get(0);
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private Item reloadItem(Item item) throws SQLException {
        return context.reloadEntity(item);
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private void withdrawItems(Item... items) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        for (Item item : items) {
            itemService.withdraw(context, reloadItem(item));
        }
        context.restoreAuthSystemState();
    }

    private String patchDetectDuplication(String itemId, String decision, String note) {
        Map<String, String> value = note != null ? Map.of("value", decision, "note", note) : Map.of("value", decision);
        Operation op = new AddOperation("/sections/detect-duplicate/matches/" + itemId + "/workflowDecision", value);
        return getPatchContent(List.of(op));
    }
}
