/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import static org.dspace.builder.CrisLayoutBoxBuilder.createBuilder;
import static org.dspace.builder.CrisLayoutFieldBuilder.createMetadataField;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.profile.CvEntity;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.CvEntityService;
import org.dspace.app.profile.service.ResearcherProfileService;
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
import org.dspace.content.MetadataField;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link DeleteCvEntititesAction}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DeleteCvEntititesActionIT extends AbstractIntegrationTestWithDatabase {

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private ResearcherProfileService researcherProfileService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private InstallItemService installItemService;

    private EPerson submitter;

    private Group directorioGroup;

    private RelationshipType personCloneHasShadowCopy;

    private RelationshipType personIsCloneOf;

    private RelationshipType personIsWithdrawOf;

    private RelationshipType publicationCloneHasShadowCopy;

    private RelationshipType publicationIsCloneOf;

    private RelationshipType publicationIsWithdrawOf;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection directorioPersons;

    private Collection directorioPublications;

    private Collection cvPersonCollection;

    private Collection cvPersonCloneCollection;

    private Collection cvPublicationCollection;

    private Collection cvPublicationCloneCollection;

    private MetadataFieldService metadataFieldService;

    private DeleteCvEntititesAction deleteCvEntititesAction;

    private CvEntityService cvEntityService;

    @Before
    public void before() throws Exception {

        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionRoleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

        deleteCvEntititesAction = new DSpace().getServiceManager()
            .getServiceByName("deleteCvEntititesAction", DeleteCvEntititesAction.class);

        cvEntityService = new DSpace().getSingletonService(CvEntityService.class);

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonType = createEntityType("CvPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");

        EntityType publicationType = createEntityType("Publication");
        EntityType cvPublicationType = createEntityType("CvPublication");
        EntityType cvPublicationCloneType = createEntityType("CvPublicationClone");

        personCloneHasShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        personIsCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        personIsWithdrawOf = createIsWithdrawOfRelationship(personType);

        publicationCloneHasShadowCopy = createHasShadowCopyRelationship(cvPublicationCloneType, publicationType);
        publicationIsCloneOf = createCloneRelationship(cvPublicationCloneType, cvPublicationType);
        publicationIsWithdrawOf = createIsWithdrawOfRelationship(publicationType);

        submitter = createEPerson("submitter@example.com");
        context.setCurrentUser(submitter);

        EPerson directorioUser = createEPerson("directorioUser@example.com");

        directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        directorioGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(directorioUser)
            .build();

        directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withEntityType("Person")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioGroup)
            .withRoleGroup("reviewer", directorioGroup)
            .build();

        directorioPublications = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Publications")
            .withEntityType("Publication")
            .withSubmitterGroup(submitter)
            .withRoleGroup("editor", directorioGroup)
            .withRoleGroup("reviewer", directorioGroup)
            .build();

        ctiVitaeCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae Community")
            .build();

        ctiVitaeCloneCommunity = CommunityBuilder.createCommunity(context)
            .withName("CTIVitae clone Community")
            .build();

        cvPersonCollection = CollectionBuilder.createCollection(context, ctiVitaeCommunity)
            .withName("Profiles")
            .withEntityType("CvPerson")
            .withSubmitterGroup(submitter)
            .build();

        cvPersonCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Profiles")
            .withEntityType("CvPersonClone")
            .withWorkflow("institutionWorkflow")
            .build();

        cvPublicationCollection = CollectionBuilder.createCollection(context, ctiVitaeCommunity)
            .withName("Publications")
            .withEntityType("CvPublication")
            .withSubmitterGroup(submitter)
            .build();

        cvPublicationCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Publications")
            .withEntityType("CvPublicationClone")
            .withWorkflow("institutionWorkflow")
            .build();

        setConfigurationProperty("directorios.community-id", directorioCommunity.getID());
        setConfigurationProperty("researcher-profile.collection.uuid", cvPersonCollection.getID());
        setConfigurationProperty("cti-vitae.clone.person-collection-id", cvPersonCloneCollection.getID());
        setConfigurationProperty("item.enable-virtual-metadata", false);
        setConfigurationProperty("claimable.entityType", "Person");

        setConfigurationProperty("researcher-profile.publication.collection.uuid", cvPublicationCollection.getID());
        setConfigurationProperty("cti-vitae.clone.publication-collection-id", cvPublicationCloneCollection.getID());

        CrisLayoutBox publicBox = createBuilder(context, personType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        createMetadataField(context, metadataField("dc", "title", null), 1, 1)
            .withBox(publicBox)
            .build();

        createMetadataField(context, metadataField("person", "birthDate", null), 2, 1)
            .withBox(publicBox)
            .build();

        context.restoreAuthSystemState();

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();

        collectionRoleService.deleteByCollection(context, cvPersonCloneCollection);
        collectionRoleService.deleteByCollection(context, directorioPersons);
        workflowItemService.deleteByCollection(context, cvPersonCloneCollection);
        workflowItemService.deleteByCollection(context, directorioPersons);
        collectionRoleService.deleteByCollection(context, cvPublicationCloneCollection);
        collectionRoleService.deleteByCollection(context, directorioPublications);
        workflowItemService.deleteByCollection(context, cvPublicationCloneCollection);
        workflowItemService.deleteByCollection(context, directorioPublications);

        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testDeleteWithoutCvEntities() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);

        context.restoreAuthSystemState();

        Item profile = researcherProfile.getItem();
        Item profileClone = getCvPersonClone(profile);

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

    }

    @Test
    public void testDeleteWithClaimedCvEntity() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);

        Item publication = ItemBuilder.createItem(context, directorioPublications)
            .withTitle("Test publication")
            .build();

        CvEntity cvPublicationEntity = cvEntityService.createFromItem(context, publication);
        Item cvPublication = cvPublicationEntity.getItem();
        Item cvPublicationClone = getCvPublicationClone(cvPublication);

        context.restoreAuthSystemState();

        Item profile = researcherProfile.getItem();
        Item profileClone = getCvPersonClone(profile);

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvPublication), nullValue());
        assertThat(reloadItem(cvPublicationClone), nullValue());
        assertThat(reloadItem(publication), notNullValue());
        assertThat(findRelations(publication, publicationIsWithdrawOf), hasSize(1));

    }

    @Test
    public void testDeleteWithCvEntityCreatedFromScratch() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);
        Item profile = researcherProfile.getItem();

        Item cvPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Test publication")
            .withCtiVitaeOwner(profile)
            .build();

        context.commit();

        Item profileClone = getCvPersonClone(profile);
        assertThat(findRelations(cvPublication, publicationIsCloneOf), empty());

        context.restoreAuthSystemState();

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvPublication), nullValue());
    }

    @Test
    public void testDeleteWithCvEntityCreatedFromScratchNotAlreadySubmittedOnDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);
        Item profile = researcherProfile.getItem();

        Item cvPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Test publication")
            .withCtiVitaeOwner(profile)
            .withCvPublicationSyncEnabled(true)
            .build();

        context.commit();

        Item profileClone = getCvPersonClone(profile);
        Item cvPublicationClone = getCvPublicationClone(cvPublication);
        Item publication = getPublicationFromCvPublicationClone(cvPublicationClone);
        assertThat(publication.isArchived(), is(false));

        context.restoreAuthSystemState();

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvPublication), nullValue());
        assertThat(reloadItem(cvPublicationClone), nullValue());
        assertThat(reloadItem(publication), nullValue());
    }

    @Test
    public void testDeleteWithCvEntityCreatedFromScratchAlreadySubmittedOnDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);
        Item profile = researcherProfile.getItem();

        Item cvPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Test publication")
            .withCtiVitaeOwner(profile)
            .withCvPublicationSyncEnabled(true)
            .build();

        context.commit();

        Item profileClone = getCvPersonClone(profile);
        Item cvPublicationClone = getCvPublicationClone(cvPublication);
        Item publication = getPublicationFromCvPublicationClone(cvPublicationClone);
        assertThat(publication.isArchived(), is(false));

        installItemService.installItem(context, workflowItemService.findByItem(context, publication));

        context.restoreAuthSystemState();

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvPublication), nullValue());
        assertThat(reloadItem(cvPublicationClone), nullValue());
        assertThat(reloadItem(publication), notNullValue());
        assertThat(findRelations(publication, publicationIsWithdrawOf), hasSize(1));
    }

    @Test
    public void testDeleteWithManyCvEntities() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        ResearcherProfile researcherProfile = createProfileFromItem(person);
        Item profile = researcherProfile.getItem();
        Item profileClone = getCvPersonClone(profile);

        Item firstPublication = ItemBuilder.createItem(context, directorioPublications)
            .withTitle("First test publication")
            .build();

        CvEntity cvFirstPublicationEntity = cvEntityService.createFromItem(context, firstPublication);
        Item cvFirstPublication = cvFirstPublicationEntity.getItem();
        Item cvFirstPublicationClone = getCvPublicationClone(cvFirstPublication);

        Item cvSecondPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Second Test publication")
            .withCtiVitaeOwner(profile)
            .withCvPublicationSyncEnabled(true)
            .build();

        Item cvSecondPublicationClone = getCvPublicationClone(cvSecondPublication);
        Item secondPublication = getPublicationFromCvPublicationClone(cvSecondPublicationClone);
        assertThat(secondPublication.isArchived(), is(false));

        context.restoreAuthSystemState();

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvFirstPublication), nullValue());
        assertThat(reloadItem(cvFirstPublicationClone), nullValue());
        assertThat(reloadItem(firstPublication), notNullValue());
        assertThat(findRelations(firstPublication, publicationIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvSecondPublication), nullValue());
        assertThat(reloadItem(cvSecondPublicationClone), nullValue());
        assertThat(reloadItem(secondPublication), nullValue());

    }

    @Test
    public void testDeleteWithCvProfileCreatedFromScratch() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvPersonCollection)
            .withTitle("White, Walter")
            .build();

        assertThat(findRelations(profile, personIsCloneOf), empty());

        context.restoreAuthSystemState();

        deleteCvEntititesAction.apply(context, profile);
        assertThat(reloadItem(profile), notNullValue());

    }

    @Test
    public void testDeleteWithCvProfileCreatedFromScratchNotAlreadySubmittedOnDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvPersonCollection)
            .withTitle("White, Walter")
            .withCvPersonBasicInfoSyncEnabled(true)
            .build();

        Item profileClone = getCvPersonClone(profile);
        Item person = getPersonFromCvPersonClone(profileClone);
        assertThat(person.isArchived(), is(false));

        Item cvPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Test publication")
            .withCtiVitaeOwner(profile)
            .withCvPublicationSyncEnabled(true)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        Item cvPublicationClone = getCvPublicationClone(cvPublication);
        Item publication = getPublicationFromCvPublicationClone(cvPublicationClone);
        assertThat(publication.isArchived(), is(false));

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(person), nullValue());

        assertThat(reloadItem(cvPublication), nullValue());
        assertThat(reloadItem(cvPublicationClone), nullValue());
        assertThat(reloadItem(publication), nullValue());

    }

    @Test
    public void testDeleteWithCvProfileCreatedFromScratchAlreadySubmittedOnDirectorio() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvPersonCollection)
            .withTitle("White, Walter")
            .withCvPersonBasicInfoSyncEnabled(true)
            .build();

        Item profileClone = getCvPersonClone(profile);
        Item person = getPersonFromCvPersonClone(profileClone);
        assertThat(person.isArchived(), is(false));

        installItemService.installItem(context, workflowItemService.findByItem(context, person));

        Item cvPublication = ItemBuilder.createItem(context, cvPublicationCollection)
            .withTitle("Test publication")
            .withCtiVitaeOwner(profile)
            .withCvPublicationSyncEnabled(true)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        Item cvPublicationClone = getCvPublicationClone(cvPublication);
        Item publication = getPublicationFromCvPublicationClone(cvPublicationClone);
        assertThat(publication.isArchived(), is(false));

        deleteCvEntititesAction.apply(context, profile);

        assertThat(reloadItem(profile), notNullValue());
        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(person), notNullValue());
        assertThat(findRelations(person, personIsWithdrawOf), hasSize(1));

        assertThat(reloadItem(cvPublication), nullValue());
        assertThat(reloadItem(cvPublicationClone), nullValue());
        assertThat(reloadItem(publication), nullValue());

    }

    private Item reloadItem(Item item) throws SQLException {
        return context.reloadEntity(item);
    }

    private ResearcherProfile createProfileFromItem(Item person) throws Exception {
        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        return researcherProfileService.createFromSource(context, context.getCurrentUser(),
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsWithdrawOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, WITHDRAW.getLeftType(),
            WITHDRAW.getRightType(), 0, 1, 0, 1).build();
    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withPassword(password)
            .build();
    }

    private MetadataField metadataField(String schema, String element, String qualifier) throws SQLException {
        return metadataFieldService.findByElement(context, schema, element, qualifier);
    }

    private void setConfigurationProperty(String key, Object value) {
        configurationService.setProperty(key, value != null ? value.toString() : null);
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private Item getCvPersonClone(Item cvPerson) throws SQLException {
        return getRelatedItem(cvPerson, personIsCloneOf);
    }

    private Item getPersonFromCvPersonClone(Item cvPersonClone) throws SQLException {
        return getRelatedItem(cvPersonClone, personCloneHasShadowCopy);
    }

    private Item getCvPublicationClone(Item cvPublication) throws SQLException {
        return getRelatedItem(cvPublication, publicationIsCloneOf);
    }

    private Item getPublicationFromCvPublicationClone(Item cvPublicationClone) throws SQLException {
        return getRelatedItem(cvPublicationClone, publicationCloneHasShadowCopy);
    }

    private Item getRelatedItem(Item item, RelationshipType relationshipType) throws SQLException {
        List<Relationship> relations = findRelations(item, relationshipType);
        assertThat(relations, hasSize(1));
        Relationship relation = relations.get(0);
        return relation.getLeftItem().equals(item) ? relation.getRightItem() : relation.getLeftItem();
    }

}
