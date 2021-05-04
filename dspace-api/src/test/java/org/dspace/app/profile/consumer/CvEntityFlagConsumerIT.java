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
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.net.URI;
import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.profile.ResearcherProfile;
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
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
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
 * Integration tests for {@link CvEntityFlagConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityFlagConsumerIT extends AbstractIntegrationTestWithDatabase {

    private ConfigurationService configurationService;

    private ItemService itemService;

    private ResearcherProfileService researcherProfileService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private MetadataFieldService metadataFieldService;

    private EPerson submitter;

    private Group directorioEditorGroup;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection directorioPersons;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    private EntityType personType;

    @Before
    public void before() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionRoleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

        context.turnOffAuthorisationSystem();

        personType = createEntityType("Person");
        EntityType cvPersonType = createEntityType("CvPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);

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

        directorioPersons = CollectionBuilder
            .createCollection(context, directorioCommunity)
            .withWorkflow("directorioWorkflow")
            .withName("Persons")
            .withEntityType("Person")
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
            .withEntityType("CvPerson")
            .withSubmitterGroup(submitter)
            .build();

        cvCloneCollection = CollectionBuilder.createCollection(context, ctiVitaeCloneCommunity)
            .withName("Profiles")
            .withEntityType("CvPersonClone")
            .withWorkflow("institutionWorkflow")
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID().toString());
        configurationService.setProperty("researcher-profile.collection.uuid", cvCollection.getID().toString());
        configurationService.setProperty("cti-vitae.clone.person-collection-id", cvCloneCollection.getID().toString());
        configurationService.setProperty("item.enable-virtual-metadata", false);
        configurationService.setProperty("claimable.entityType", "Person");

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
    public void testClaimedProfileEdit() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .withBirthDate("1992-06-26")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();

        addMetadata(profile, "perucris", "phone", null, "1112223333");
        removeMetadata(profile, "person", "birthDate", null);

        profile = updateItem(profile);

        context.restoreAuthSystemState();

        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.personbirthDate", "false")));
        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.perucrisphone", "false")));

    }

    @Test
    public void testNewProfileCreationAndCorrection() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvCollection)
            .withTitle("White, Walter")
            .withPhone("1112223333")
            .build();

        context.restoreAuthSystemState();

        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.dctitle", "false")));
        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.perucrisphone", "false")));

        addMetadata(profile, "person", "birthDate", null, "1992-06-26");
        removeMetadata(profile, "perucris", "phone", null);

        context.turnOffAuthorisationSystem();
        profile = updateItem(profile);
        context.restoreAuthSystemState();

        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.dctitle", "false")));
        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.perucrisphone", "false")));
        assertThat(profile.getMetadata(), hasItem(with("perucris.flagcv.personbirthDate", "false")));
        assertThat(itemService.getMetadataByMetadataString(profile, "perucris.flagcv.perucrisphone"), hasSize(1));

    }

    private void createPersonCrisLayout() throws Exception {

        CrisLayoutBox publicBox = createBuilder(context, personType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        createMetadataField(context, metadataField("dc", "title", null), 1, 1)
            .withBox(publicBox)
            .build();

        createMetadataField(context,metadataField("person", "birthDate", null), 2, 1)
            .withBox(publicBox)
            .build();
    }

    private MetadataField metadataField(String schema, String element, String qualifier) throws SQLException {
        return metadataFieldService.findByElement(context,schema, element, qualifier);
    }

    private void removeMetadata(Item item, String schema, String element, String qualifier) throws SQLException {
        itemService.removeMetadataValues(context, item, schema, element, qualifier, Item.ANY);
    }

    private Item updateItem(Item item) throws Exception {
        itemService.update(context, item);
        context.commit();
        return reloadItem(item);
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value)
        throws SQLException {
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
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
}
