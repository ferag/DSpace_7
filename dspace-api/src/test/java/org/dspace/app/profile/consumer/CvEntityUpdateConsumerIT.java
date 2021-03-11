/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CrisLayoutBoxBuilder.createBuilder;
import static org.dspace.builder.CrisLayoutFieldBuilder.createMetadataField;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CORRECTION;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
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
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CvEntityUpdateConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityUpdateConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String PLACEHOLDER = PLACEHOLDER_PARENT_METADATA_VALUE;

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private ItemService itemService;

    private ResearcherProfileService researcherProfileService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private InstallItemService installItemService;

    private MetadataFieldService metadataFieldService;

    private EPerson submitter;

    private Group directorioEditorGroup;

    private RelationshipType isCorrectionOf;

    private RelationshipType cloneIsCorrectionOf;

    private RelationshipType isCloneOf;

    private EntityType personType;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection directorioPersons;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    @Before
    public void before() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionRoleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

        context.turnOffAuthorisationSystem();

        personType = createEntityType("Person");
        EntityType cvPersonType = createEntityType("CvPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        isCorrectionOf = createIsCorrectionOfRelationship(personType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvPersonCloneType);
        isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        createIsMergedInRelationship(personType);
        createIsOriginatedFromRelationship(personType, cvPersonCloneType);

        submitter = createEPerson("submitter@example.com");
        context.setCurrentUser(submitter);

        EPerson directorioUser = createEPerson("directorioUser@example.com");

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        directorioEditorGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(directorioUser)
            .build();

        directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withRelationshipType("Person")
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
            .withName("Profiles")
            .withRelationshipType("CvPerson")
            .withSubmitterGroup(submitter)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Profiles")
            .withRelationshipType("CvPersonClone")
            .withWorkflow("institutionWorkflow")
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());
        configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
        configurationService.setProperty("cti-vitae.clone.person-collection-id", cvCloneCollection.getID().toString());
        configurationService.setProperty("item.enable-virtual-metadata", false);

        createPersonCrisLayout();

        context.restoreAuthSystemState();

    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        collectionRoleService.deleteByCollection(context, cvCloneCollection);
        collectionRoleService.deleteByCollection(context, directorioPersons);
        workflowItemService.deleteByCollection(context, cvCloneCollection);
        workflowItemService.deleteByCollection(context, directorioPersons);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testUpdatesOnPersonAreAlsoMadeOnClaimedProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .withBirthDate("1992-06-26")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.commit();
        person = reloadItem(person);

        context.restoreAuthSystemState();

        addMetadata(person, "perucris", "phone", null, "1112223333");
        removeMetadata(person, "person", "birthDate", null);

        context.turnOffAuthorisationSystem();
        person = updateItem(person);
        context.restoreAuthSystemState();

        profile = reloadItem(profile);
        assertThat(profile.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(getMetadata(profile, "person", "birthDate", null), empty());

        profileClone = reloadItem(profileClone);
        assertThat(profileClone.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(getMetadata(profileClone, "person", "birthDate", null), empty());

        assertThat(findRelations(person, isCorrectionOf), empty());
        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());

    }

    @Test
    public void testUpdatesOnPersonRelatedToInstitutionPersonAreAlsoMadeOnClaimedProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection institutionPersonCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("People")
            .withRelationshipType("InstitutionPerson")
            .build();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .withBirthDate("1992-06-26")
            .build();

        Item institutionPerson = ItemBuilder.createItem(context, institutionPersonCollection)
            .withTitle("White, Walter")
            .withBirthDate("1992-06-26")
            .build();

        EntityType institutionPersonType = createEntityType("InstitutionPerson");
        RelationshipType institutionShadowCopy = createHasShadowCopyRelationship(institutionPersonType, personType);
        RelationshipBuilder.createRelationshipBuilder(context, institutionPerson, person, institutionShadowCopy);

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.commit();
        person = reloadItem(person);

        context.restoreAuthSystemState();

        addMetadata(person, "perucris", "phone", null, "1112223333");
        removeMetadata(person, "person", "birthDate", null);

        context.turnOffAuthorisationSystem();
        person = updateItem(person);
        context.restoreAuthSystemState();

        profile = reloadItem(profile);
        assertThat(profile.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(getMetadata(profile, "person", "birthDate", null), empty());

        profileClone = reloadItem(profileClone);
        assertThat(profileClone.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(getMetadata(profileClone, "person", "birthDate", null), empty());

        assertThat(findRelations(person, isCorrectionOf), empty());
        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());

    }

    @Test
    public void testUpdatesOnPersonAreAlsoMadeOnProfileCreatedFromScratchWithoutOverwritingMetadata() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test profile")
            .withBirthDate("1992-06-26")
            .withPersonAffiliation("4Science")
            .withPersonAffiliationStartDate("2020-07-01")
            .withPersonAffiliationEndDate(PLACEHOLDER)
            .withPersonAffiliationRole("Researcher")
            .withCvPersonBasicInfoSyncEnabled(true)
            .build();

        List<XmlWorkflowItem> personWorkflowItems = workflowItemService.findByCollection(context, directorioPersons);
        assertThat(personWorkflowItems, hasSize(1));

        Item person = personWorkflowItems.get(0).getItem();
        installItemService.installItem(context, personWorkflowItems.get(0));

        List<XmlWorkflowItem> cloneWorkflowItems = workflowItemService.findByCollection(context, cvCloneCollection);
        assertThat(cloneWorkflowItems, hasSize(1));

        Item profileClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.commit();
        person = reloadItem(person);

        context.restoreAuthSystemState();

        addMetadata(person, "perucris", "phone", null, "1112223333");
        removeMetadata(person, "person", "birthDate", null);

        context.turnOffAuthorisationSystem();
        person = updateItem(person);
        context.restoreAuthSystemState();

        profile = reloadItem(profile);
        assertThat(profile.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(profile.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

        profileClone = reloadItem(profileClone);
        assertThat(profileClone.getMetadata(), hasItem(with("perucris.phone", "1112223333")));
        assertThat(profileClone.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

        assertThat(findRelations(person, isCorrectionOf), empty());
        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());
    }

    private void createPersonCrisLayout() throws Exception {

        CrisLayoutBox publicBox = createBuilder(context, personType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        createMetadataField(context, metadataField("dc", "title", null), 1, 1)
            .withBox(publicBox)
            .build();

        createMetadataField(context, metadataField("person", "birthDate", null), 2, 1)
            .withBox(publicBox)
            .build();
    }

    private MetadataField metadataField(String schema, String element, String qualifier) throws SQLException {
        return metadataFieldService.findByElement(context, schema, element, qualifier);
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value)
        throws SQLException {
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
    }

    private void removeMetadata(Item item, String schema, String element, String qualifier) throws SQLException {
        itemService.removeMetadataValues(context, item, schema, element, qualifier, Item.ANY);
    }

    private List<MetadataValue> getMetadata(Item item, String schema, String element, String qualifier) {
        return itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
    }

    private Item updateItem(Item item) throws Exception {
        itemService.update(context, item);
        context.commit();
        return reloadItem(item);
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

    private RelationshipType createIsOriginatedFromRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType,
            ORIGINATED.getLeftType(), ORIGINATED.getRightType(), 0, null, 0, 1).build();
    }

    private RelationshipType createIsMergedInRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, MERGED.getLeftType(),
            MERGED.getRightType(), 0, 1, 0, null).build();
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withPassword(password)
            .build();
    }
}
