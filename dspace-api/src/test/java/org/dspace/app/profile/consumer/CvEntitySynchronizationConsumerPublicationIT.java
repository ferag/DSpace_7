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
import static org.dspace.content.authority.Choices.CF_UNSET;
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
import org.dspace.handle.dao.HandleDAO;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.PoolTaskDAO;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CvEntitySynchronizationConsumer} with publications test cases.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntitySynchronizationConsumerPublicationIT extends AbstractIntegrationTestWithDatabase {

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

    private Collection publications;

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

        EntityType publicationType = createEntityType("Publication");
        EntityType cvPublicationType = createEntityType("CvPublication");
        EntityType cvPublicationCloneType = createEntityType("CvPublicationClone");

        shadowCopy = createHasShadowCopyRelationship(cvPublicationCloneType, publicationType);
        isCorrectionOf = createIsCorrectionOfRelationship(publicationType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvPublicationCloneType);
        isCloneOf = createCloneRelationship(cvPublicationCloneType, cvPublicationType);

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

        publications = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Publications")
            .withEntityType("Publication")
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
            .withName("Publications")
            .withEntityType("CvPublication")
            .withSubmitterGroup(submitter)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Publications")
            .withEntityType("CvPublicationClone")
            .withWorkflow("institutionWorkflow")
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());
        configurationService.setProperty("researcher-profile.publication.collection.uuid",
            cvCollection.getID().toString());

        configurationService.setProperty("cti-vitae.clone.publication-collection-id",
            cvCloneCollection.getID().toString());

        configurationService.setProperty("item.enable-virtual-metadata", false);

        context.restoreAuthSystemState();

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        itemService.findByCollection(context, cvCollection).forEachRemaining(this::deleteItem);
        collectionRoleService.deleteByCollection(context, cvCloneCollection);
        collectionRoleService.deleteByCollection(context, publications);
        workflowItemService.deleteByCollection(context, cvCloneCollection);
        workflowItemService.deleteByCollection(context, publications);
        context.commit();
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testNewCvPublicationSubmissionWhenSynchronizationIsDisabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPublication, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, publications), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPublicationSubmissionWhenSynchronizationIsFalse() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withCvPublicationSyncEnabled(false)
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPublication, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, publications), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPublicationSubmissionWhenSynchronizationIsEnabled() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .withCvPublicationSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvPublication, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPublicationClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPublicationClone.isArchived(), is(false));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(workflowItems, hasSize(1));

        Item publication = workflowItems.get(0).getItem();
        assertThat(publication.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<Relationship> shadowCopyRelations = findRelations(cvPublicationClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(publication));

    }

    @Test
    public void testNewCvPublicationSubmissionAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .withCvPublicationSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneOfRelations = findRelations(cvPublication, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPublicationClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPublicationClone.isArchived(), is(false));

        addMetadata(cvPublication, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPublication, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(cvPublicationClone, nullValue());

        cloneOfRelations = findRelations(cvPublication, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item newPublicationClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(newPublicationClone.isArchived(), is(false));
        assertThat(newPublicationClone.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(newPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(newPublicationClone.getMetadata(),
            hasItem(with("dc.contributor.author", "White, Walter", 0, CF_UNSET)));
        assertThat(newPublicationClone.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(workflowItems, hasSize(1));

        Item publication = workflowItems.get(0).getItem();
        assertThat(publication.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(publication.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", 0, CF_UNSET)));
        assertThat(publication.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        List<Relationship> shadowCopyRelations = findRelations(newPublicationClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(publication));

    }

    @Test
    public void testNewCvPublicationSubmissionWithSyncDisabledAndEditBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPublication, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, publications), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvPublication, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPublication, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPublication = updateItem(cvPublication);

        assertThat(findRelations(cvPublication, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, publications), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

    }

    @Test
    public void testNewCvPublicationSubmissionWithSyncDisabledAndThenEnabledBeforeConcytecFeedback() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(cvPublication, isCloneOf), empty());
        assertThat(workflowItemService.findByCollection(context, publications), empty());
        assertThat(workflowItemService.findByCollection(context, cvCloneCollection), empty());

        addMetadata(cvPublication, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPublication, "oairecerif", "author", "affiliation", PLACEHOLDER);
        addMetadata(cvPublication, "perucris", "cvPublication", "syncEnabled", "true");
        cvPublication = updateItem(cvPublication);

        List<Relationship> cloneOfRelations = findRelations(cvPublication, isCloneOf);
        assertThat(cloneOfRelations, hasSize(1));

        Item cvPublicationClone = cloneOfRelations.get(0).getLeftItem();
        assertThat(cvPublicationClone.isArchived(), is(false));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", 0,
                                                                  Choices.CF_UNSET)));
        assertThat(cvPublicationClone.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(workflowItems, hasSize(1));

        Item publication = workflowItems.get(0).getItem();
        assertThat(publication.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(publication.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                           0, Choices.CF_UNSET)));
        assertThat(publication.getMetadata(), hasItem(with("oairecerif.author.affiliation",
            PLACEHOLDER, 0, 400)));

        List<Relationship> shadowCopyRelations = findRelations(cvPublicationClone, shadowCopy);
        assertThat(shadowCopyRelations, hasSize(1));

        assertThat(shadowCopyRelations.get(0).getRightItem(), is(publication));

    }

    @Test
    public void testCvPublicationCorrectionWithSyncDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .withCvPublicationSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> publicationWorkflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(publicationWorkflowItems, hasSize(1));

        Item publication = publicationWorkflowItems.get(0).getItem();
        installItemService.installItem(context, publicationWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPublicationClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        addMetadata(cvPublication, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPublication, "oairecerif", "author", "affiliation", PLACEHOLDER);
        setMetadataSingleValue(cvPublication, "perucris", "cvPublication", "syncEnabled", "false");
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(getMetadata(cvPublicationClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPublicationClone, "oairecerif", "author", "affiliation"), empty());

        publication = reloadItem(publication);
        assertThat(getMetadata(publication, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(publication, "oairecerif", "author", "affiliation"), empty());

        assertThat(findRelations(cvPublicationClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(publication, isCorrectionOf), empty());

    }

    @Test
    public void testCvPublicationCorrectionsWithSyncEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .withCvPublicationSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> publicationWorkflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(publicationWorkflowItems, hasSize(1));

        Item publication = publicationWorkflowItems.get(0).getItem();
        installItemService.installItem(context, publicationWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPublicationClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.commit();
        cvPublication = reloadItem(cvPublication);

        context.restoreAuthSystemState();

        addMetadata(cvPublication, "dc", "contributor", "author", "White, Walter");
        addMetadata(cvPublication, "oairecerif", "author", "affiliation", PLACEHOLDER);
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(getMetadata(cvPublicationClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPublicationClone, "oairecerif", "author", "affiliation"), empty());

        publication = reloadItem(publication);
        assertThat(getMetadata(publication, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(publication, "oairecerif", "author", "affiliation"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(cvPublicationClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", 0, CF_UNSET)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        List<Relationship> publicationCorrectionRelations = findRelations(publication, isCorrectionOf);
        assertThat(publicationCorrectionRelations, hasSize(1));

        Item correction = publicationCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(correction.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));
        assertThat(correction.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter", 0, CF_UNSET)));
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        removeMetadata(cvPublication, "dc", "date", "issued");
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(getMetadata(cvPublicationClone, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(cvPublicationClone, "oairecerif", "author", "affiliation"), empty());
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        publication = reloadItem(publication);
        assertThat(getMetadata(publication, "dc", "contributor", "author"), empty());
        assertThat(getMetadata(publication, "oairecerif", "author", "affiliation"), empty());
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        cloneCorrectionRelations = findRelations(cvPublicationClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(cloneCorrection.getMetadata(), hasItem(not(with("dc.date.issued", "2021-01-01"))));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                               0, Choices.CF_UNSET)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

        publicationCorrectionRelations = findRelations(publication, isCorrectionOf);
        assertThat(publicationCorrectionRelations, hasSize(1));

        correction = publicationCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test Publication")));
        assertThat(correction.getMetadata(), hasItem(not(with("dc.date.issued", "2021-01-01"))));
        assertThat(correction.getMetadata(), hasItem(with("dc.contributor.author", "White, Walter",
                                                          0, Choices.CF_UNSET)));
        assertThat(correction.getMetadata(), hasItem(with("oairecerif.author.affiliation", PLACEHOLDER, 0, 400)));

    }

    @Test
    public void testCvPublicationEditWithChangeReversion() throws Exception {

        context.turnOffAuthorisationSystem();

        Item cvPublication = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test Publication")
            .withIssueDate("2021-01-01")
            .withCvPublicationSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> publicationWorkflowItems = workflowItemService.findByCollection(context, publications);
        assertThat(publicationWorkflowItems, hasSize(1));

        Item publication = publicationWorkflowItems.get(0).getItem();
        installItemService.installItem(context, publicationWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item cvPublicationClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.restoreAuthSystemState();

        replaceMetadata(cvPublication, "dc", "date", "issued", "2021-04-04", 0);
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        publication = reloadItem(publication);
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        List<Relationship> cloneCorrectionRelations = findRelations(cvPublicationClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.date.issued", "2021-04-04")));

        List<Relationship> publicationCorrectionRelations = findRelations(publication, isCorrectionOf);
        assertThat(publicationCorrectionRelations, hasSize(1));

        Item correction = publicationCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.getMetadata(), hasItem(with("dc.date.issued", "2021-04-04")));

        replaceMetadata(cvPublication, "dc", "date", "issued", "2021-01-01", 0);
        cvPublication = updateItem(cvPublication);

        cvPublicationClone = reloadItem(cvPublicationClone);
        assertThat(cvPublicationClone.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        publication = reloadItem(publication);
        assertThat(publication.getMetadata(), hasItem(with("dc.date.issued", "2021-01-01")));

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(correction), nullValue());

        assertThat(findRelations(cvPublicationClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(publication, isCorrectionOf), empty());

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
