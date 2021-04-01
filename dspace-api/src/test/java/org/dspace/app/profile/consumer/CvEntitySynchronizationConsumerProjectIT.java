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
 * Integration tests for {@link CvEntitySynchronizationConsumer} with projects
 * test cases.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntitySynchronizationConsumerProjectIT extends AbstractIntegrationTestWithDatabase {

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

    private Collection projects;

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

        EntityType projectType = createEntityType("Project");
        EntityType cvProjectType = createEntityType("CvProject");
        EntityType cvProjectCloneType = createEntityType("CvProjectClone");

        shadowCopy = createHasShadowCopyRelationship(cvProjectCloneType, projectType);
        isCorrectionOf = createIsCorrectionOfRelationship(projectType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvProjectCloneType);
        isCloneOf = createCloneRelationship(cvProjectCloneType, cvProjectType);

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

        projects = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Projects")
            .withRelationshipType("Project")
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
            .withName("Projects")
            .withRelationshipType("CvProject")
            .withSubmitterGroup(submitter)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Projects")
            .withRelationshipType("CvProjectClone")
            .withWorkflow("institutionWorkflow")
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());
        configurationService.setProperty("researcher-profile.project.collection.uuid",
            cvCollection.getID().toString());

        configurationService.setProperty("cti-vitae.clone.project-collection-id",
            cvCloneCollection.getID().toString());

        configurationService.setProperty("item.enable-virtual-metadata", false);

        context.restoreAuthSystemState();

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        itemService.findByCollection(context, cvCollection).forEachRemaining(this::deleteItem);
        collectionRoleService.deleteByCollection(context, cvCloneCollection);
        collectionRoleService.deleteByCollection(context, projects);
        workflowItemService.deleteByCollection(context, cvCloneCollection);
        workflowItemService.deleteByCollection(context, projects);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testNewCvProjectSubmissionWhenSynchronizationIsDisabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvProject, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, projects), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvProjectSubmissionWhenSynchronizationIsFalse() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withCvProjectSyncEnabled(false)
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvProject, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, projects), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvProjectSubmissionWhenSynchronizationIsEnabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .withCvProjectSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvProject, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvProjectClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvProjectClone.isArchived(), is(false));
        assertThat(cvProjectClone.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(cvProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(workflowItems, hasSize(1));

        Item project = workflowItems.get(0).getItem();
        assertThat(project.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        List<Relationship> shadowCopyRelations = findRelations(cvProjectClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(project));

    }

    @Test
    public void testNewCvProjectSubmissionAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .withCvProjectSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvProject, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvProjectClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvProjectClone.isArchived(), is(false));

        addMetadata(cvProject, "crispj", "coordinator", null, "White, Walter");
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(cvProjectClone, nullValue());

        cloneOfRelations = findRelations(cvProject, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item newProjectClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(newProjectClone.isArchived(), is(false));
        assertThat(newProjectClone.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(newProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(newProjectClone.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(workflowItems, hasSize(1));

        Item project = workflowItems.get(0).getItem();
        assertThat(project.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(project.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        List<Relationship> shadowCopyRelations = findRelations(newProjectClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(project));

    }

    @Test
    public void testNewCvProjectSubmissionWithSyncDisabledAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvProject, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, projects), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvProject, "crispj", "coordinator", null, "White, Walter");
        cvProject = updateItem(cvProject);

        assertThat(findRelations(cvProject, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, projects), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvProjectSubmissionWithSyncDisabledAndThenEnabledBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvProject, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, projects), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvProject, "crispj", "coordinator", null, "White, Walter");
        addMetadata(cvProject, "perucris", "cvProject", "syncEnabled", "true");
        cvProject = updateItem(cvProject);

        List<Relationship> cloneOfRelations = findRelations(cvProject, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvProjectClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvProjectClone.isArchived(), is(false));
        assertThat(cvProjectClone.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(cvProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(cvProjectClone.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(workflowItems, hasSize(1));

        Item project = workflowItems.get(0).getItem();
        assertThat(project.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(project.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        List<Relationship> shadowCopyRelations = findRelations(cvProjectClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(project));

    }

    @Test
    public void testCvProjectCorrectionWithSyncDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .withCvProjectSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> projectWorkflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(projectWorkflowItems, hasSize(1));

        Item project = projectWorkflowItems.get(0).getItem();
        installItemService.installItem(context, projectWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvProjectClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        addMetadata(cvProject, "crispj", "coordinator", null, "White, Walter");
        setMetadataSingleValue(cvProject, "perucris", "cvProject", "syncEnabled", "false");
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(getMetadata(cvProjectClone, "crispj", "coordinator", null), empty());

        project = reloadItem(project);
        assertThat(getMetadata(project, "crispj", "coordinator", null), empty());

        assertThat(findRelations(cvProjectClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(project, isCorrectionOf), empty());

    }

    @Test
    public void testCvProjectCorrectionsWithSyncEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .withCvProjectSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> projectWorkflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(projectWorkflowItems, hasSize(1));

        Item project = projectWorkflowItems.get(0).getItem();
        installItemService.installItem(context, projectWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvProjectClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.commit();
        cvProject = reloadItem(cvProject);

        context.restoreAuthSystemState();

        addMetadata(cvProject, "crispj", "coordinator", null, "White, Walter");
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(getMetadata(cvProjectClone, "crispj", "coordinator", null), empty());

        project = reloadItem(project);
        assertThat(getMetadata(project, "crispj", "coordinator", null), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(cvProjectClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        List<Relationship> projectCorrectionRelations = findRelations(project, isCorrectionOf);
        assertThat(projectCorrectionRelations, hasSize(1));

        Item correction = projectCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));
        assertThat(correction.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        removeMetadata(cvProject, "oairecerif", "acronym", null);
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(getMetadata(cvProjectClone, "crispj", "coordinator", null), empty());
        assertThat(cvProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        project = reloadItem(project);
        assertThat(getMetadata(project, "crispj", "coordinator", null), empty());
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        cloneCorrectionRelations = findRelations(cvProjectClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(cloneCorrection.getMetadata(), hasItem(not(with("oairecerif.acronym", "TP"))));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

        projectCorrectionRelations = findRelations(project, isCorrectionOf);
        assertThat(projectCorrectionRelations, hasSize(1));

        correction = projectCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Project")));
        assertThat(correction.getMetadata(), hasItem(not(with("oairecerif.acronym", "TP"))));
        assertThat(correction.getMetadata(), hasItem(with("crispj.coordinator", "White, Walter", 0, 400)));

    }

    @Test
    public void testCvProjectEditWithChangeReversion() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvProject = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Project")
            .withAcronym("TP")
            .withCvProjectSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> projectWorkflowItems = workflowItemService.findByCollection(context, projects);
        assertThat(projectWorkflowItems, hasSize(1));

        Item project = projectWorkflowItems.get(0).getItem();
        installItemService.installItem(context, projectWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvProjectClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        replaceMetadata(cvProject, "oairecerif", "acronym", null, "NEW-TP", 0);
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(cvProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        project = reloadItem(project);
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        List<Relationship> cloneCorrectionRelations = findRelations(cvProjectClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.acronym", "NEW-TP")));

        List<Relationship> projectCorrectionRelations = findRelations(project, isCorrectionOf);
        assertThat(projectCorrectionRelations, hasSize(1));

        Item correction = projectCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.acronym", "NEW-TP")));

        replaceMetadata(cvProject, "oairecerif", "acronym", null, "TP", 0);
        cvProject = updateItem(cvProject);

        cvProjectClone = reloadItem(cvProjectClone);
        assertThat(cvProjectClone.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        project = reloadItem(project);
        assertThat(project.getMetadata(), hasItem(with("oairecerif.acronym", "TP")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        assertThat(findRelations(cvProjectClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(project, isCorrectionOf), empty());

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
