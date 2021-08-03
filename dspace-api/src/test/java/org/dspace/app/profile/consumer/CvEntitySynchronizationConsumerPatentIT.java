/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.content.authority.Choices.CF_AMBIGUOUS;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CORRECTION;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CvEntitySynchronizationConsumer} with patents
 * test cases.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntitySynchronizationConsumerPatentIT extends AbstractIntegrationTestWithDatabase {

    private static final String PLACEHOLDER = PLACEHOLDER_PARENT_METADATA_VALUE;

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private ItemService itemService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private InstallItemService installItemService;

    private EPerson submitter;

    private Group directorioEditorGroup;

    private RelationshipType shadowCopy;

    private RelationshipType isCorrectionOf;

    private RelationshipType cloneIsCorrectionOf;

    private RelationshipType isCloneOf;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection patents;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    @Before
    public void before() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionRoleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        context.turnOffAuthorisationSystem();

        EntityType patentType = createEntityType("Patent");
        EntityType cvPatentType = createEntityType("CvPatent");
        EntityType cvPatentCloneType = createEntityType("CvPatentClone");

        shadowCopy = createHasShadowCopyRelationship(cvPatentCloneType, patentType);
        isCorrectionOf = createIsCorrectionOfRelationship(patentType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvPatentCloneType);
        isCloneOf = createCloneRelationship(cvPatentCloneType, cvPatentType);

        submitter = createEPerson("submitter@example.com");
        context.setCurrentUser(submitter);

        EPerson directorioUser = createEPerson("directorioUser@example.com");

        directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        directorioEditorGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(directorioUser)
            .build();

        patents = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Patents")
            .withEntityType("Patent")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioEditorGroup)
            .build();

        ctiVitaeCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae Community")
            .build();

        ctiVitaeCloneCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae clone Community")
            .build();

        cvCollection = CollectionBuilder.createCollection(context, ctiVitaeCommunity)
            .withName("Patents")
            .withEntityType("CvPatent")
            .withSubmitterGroup(submitter)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Patents")
            .withEntityType("CvPatentClone")
            .withWorkflow("institutionWorkflow")
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());
        configurationService.setProperty("researcher-profile.patent.collection.uuid",
            cvCollection.getID().toString());

        configurationService.setProperty("cti-vitae.clone.patent-collection-id",
            cvCloneCollection.getID().toString());

        configurationService.setProperty("item.enable-virtual-metadata", false);

        context.restoreAuthSystemState();

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        itemService.findByCollection(context, cvCollection).forEachRemaining(this::deleteItem);
        collectionRoleService.deleteByCollection(context, cvCloneCollection);
        collectionRoleService.deleteByCollection(context, patents);
        workflowItemService.deleteByCollection(context, cvCloneCollection);
        workflowItemService.deleteByCollection(context, patents);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testNewCvPatentSubmissionWhenSynchronizationIsDisabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPatent, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, patents), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPatentSubmissionWhenSynchronizationIsFalse() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withCvPatentSyncEnabled(false)
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPatent, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, patents), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPatentSubmissionWhenSynchronizationIsEnabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .withCvPatentSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvPatent, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPatentClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPatentClone.isArchived(), is(false));
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(workflowItems, hasSize(1));

        Item patent = workflowItems.get(0).getItem();
        assertThat(patent.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<Relationship> shadowCopyRelations = findRelations(cvPatentClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(patent));

    }

    @Test
    public void testNewCvPatentSubmissionAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .withCvPatentSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvPatent, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPatentClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPatentClone.isArchived(), is(false));

        addMetadata(cvPatent, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPatent, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(cvPatentClone, nullValue());

        cloneOfRelations = findRelations(cvPatent, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item newPatentClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(newPatentClone.isArchived(), is(false));
        assertThat(newPatentClone.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(newPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(newPatentClone.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                              0, Choices.CF_UNSET)));
        assertThat(newPatentClone.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, Choices.CF_AMBIGUOUS)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(workflowItems, hasSize(1));

        Item patent = workflowItems.get(0).getItem();
        assertThat(patent.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(patent.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", 0, Choices.CF_UNSET)));
        assertThat(patent.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, CF_AMBIGUOUS)));

        List<Relationship> shadowCopyRelations = findRelations(newPatentClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(patent));

    }

    @Test
    public void testNewCvPatentSubmissionWithSyncDisabledAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPatent, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, patents), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvPatent, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPatent, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPatent = updateItem(cvPatent);

        assertThat(findRelations(cvPatent, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, patents), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPatentSubmissionWithSyncDisabledAndThenEnabledBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPatent, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, patents), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvPatent, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPatent, "oairecerif", "author", "affiliation", PLACEHOLDER);
        addMetadata(cvPatent, "perucris", "cvPatent", "syncEnabled", "true");
        cvPatent = updateItem(cvPatent);

        List<Relationship> cloneOfRelations = findRelations(cvPatent, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPatentClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPatentClone.isArchived(), is(false));
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                             0, Choices.CF_UNSET)));
        assertThat(cvPatentClone.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, Choices.CF_AMBIGUOUS)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(workflowItems, hasSize(1));

        Item patent = workflowItems.get(0).getItem();
        assertThat(patent.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(patent.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                      0, Choices.CF_UNSET)));
        assertThat(patent.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER,
                                                      0, Choices.CF_AMBIGUOUS)));

        List<Relationship> shadowCopyRelations = findRelations(cvPatentClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(patent));

    }

    @Test
    public void testCvPatentCorrectionWithSyncDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .withCvPatentSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> patentWorkflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(patentWorkflowItems, hasSize(1));

        Item patent = patentWorkflowItems.get(0).getItem();
        installItemService.installItem(context, patentWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPatentClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        addMetadata(cvPatent, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPatent, "oairecerif", "author", "affiliation", PLACEHOLDER);
        setMetadataSingleValue(cvPatent, "perucris", "cvPatent", "syncEnabled", "false");
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(getMetadata(cvPatentClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPatentClone, "oairecerif", "author", "affiliation"), empty());

        patent = reloadItem(patent);
        assertThat(getMetadata(patent, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(patent, "oairecerif", "author", "affiliation"), empty());

        assertThat(findRelations(cvPatentClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(patent, isCorrectionOf), empty());

    }

    @Test
    public void testCvPatentCorrectionsWithSyncEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .withCvPatentSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> patentWorkflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(patentWorkflowItems, hasSize(1));

        Item patent = patentWorkflowItems.get(0).getItem();
        installItemService.installItem(context, patentWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPatentClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.commit();
        cvPatent = reloadItem(cvPatent);

        context.restoreAuthSystemState();

        addMetadata(cvPatent, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPatent, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(getMetadata(cvPatentClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPatentClone, "oairecerif", "author", "affiliation"), empty());

        patent = reloadItem(patent);
        assertThat(getMetadata(patent, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(patent, "oairecerif", "author", "affiliation"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(cvPatentClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                               0, Choices.CF_UNSET)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER,
                                                               0, Choices.CF_AMBIGUOUS)));

        List<Relationship> patentCorrectionRelations = findRelations(patent, isCorrectionOf);
        assertThat(patentCorrectionRelations, hasSize(1));

        Item correction = patentCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(correction.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(correction.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                          0, Choices.CF_UNSET)));
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER,
                                                          0, Choices.CF_AMBIGUOUS)));

        removeMetadata(cvPatent, "dc", "date", "issued");
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(getMetadata(cvPatentClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPatentClone, "oairecerif", "author", "affiliation"), empty());
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        patent = reloadItem(patent);
        assertThat(getMetadata(patent, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(patent, "oairecerif", "author", "affiliation"), empty());
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        cloneCorrectionRelations = findRelations(cvPatentClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(cloneCorrection.getMetadata(), hasItem(not(with("dc.date.issued", "2021-01-01"))));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                               0, Choices.CF_UNSET)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER,
                                                               0, Choices.CF_AMBIGUOUS)));

        patentCorrectionRelations = findRelations(patent, isCorrectionOf);
        assertThat(patentCorrectionRelations, hasSize(1));

        correction = patentCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Patent")));
        assertThat(correction.getMetadata(), hasItem(not(with("dc.date.issued", "2021-01-01"))));
        assertThat(correction.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                          0, Choices.CF_UNSET)));
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER,
                                                          0, Choices.CF_AMBIGUOUS)));

    }

    @Test
    public void testCvPatentEditWithChangeReversion() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPatent = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Patent")
            .withIssueDate("2021-01-01")
            .withCvPatentSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> patentWorkflowItems = workflowItemService.findByCollection(context, patents);
        assertThat(patentWorkflowItems, hasSize(1));

        Item patent = patentWorkflowItems.get(0).getItem();
        installItemService.installItem(context, patentWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPatentClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        replaceMetadata(cvPatent, "dc", "date", "issued", "2021-04-04", 0);
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        patent = reloadItem(patent);
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<Relationship> cloneCorrectionRelations = findRelations(cvPatentClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.date.issued", "2021-04-04")));

        List<Relationship> patentCorrectionRelations = findRelations(patent, isCorrectionOf);
        assertThat(patentCorrectionRelations, hasSize(1));

        Item correction = patentCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.getMetadata(), hasItem(with("dc.date.issued", "2021-04-04")));

        replaceMetadata(cvPatent, "dc", "date", "issued", "2021-01-01", 0);
        cvPatent = updateItem(cvPatent);

        cvPatentClone = reloadItem(cvPatentClone);
        assertThat(cvPatentClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        patent = reloadItem(patent);
        assertThat(patent.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        assertThat(findRelations(cvPatentClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(patent, isCorrectionOf), empty());

    }

    private Item updateItem(Item item) throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.commit();
            return context.reloadEntity(item);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void setMetadataSingleValue(Item item, String schema, String element, String qualifier, String value)
        throws SQLException {
        itemService.setMetadataSingleValue(context, item, schema, element, qualifier, null, value);
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value)
        throws SQLException {
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
    }

    private void replaceMetadata(Item item, String schema, String element, String qualifier, String value, int place)
        throws SQLException {
        itemService.replaceMetadata(context, item, schema, element, qualifier, null, value, null, -1, place);
    }

    private void removeMetadata(Item item, String schema, String element, String qualifier) throws SQLException {
        itemService.removeMetadataValues(context, item, schema, element, qualifier, Item.ANY);
    }

    private List<MetadataValue> getMetadata(Item item, String schema, String element, String qualifier) {
        return itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
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

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsCorrectionOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, CORRECTION.getLeftType(),
            CORRECTION.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).build();
    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withPassword(password)
            .build();
    }

    private void deleteItem(Item item) {
        try {
            this.itemService.delete(context, item);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
