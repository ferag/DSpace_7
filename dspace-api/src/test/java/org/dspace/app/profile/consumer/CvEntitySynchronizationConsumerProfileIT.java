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
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.authority.Choices;
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
 * Integration tests for {@link ClaimedProfileEdit} with profile test cases.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntitySynchronizationConsumerProfileIT extends AbstractIntegrationTestWithDatabase {

    private static final String PLACEHOLDER = PLACEHOLDER_PARENT_METADATA_VALUE;

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private ItemService itemService;

    private ResearcherProfileService researcherProfileService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private InstallItemService installItemService;

    private EPerson submitter;

    private Group directorioEditorGroup;

    private RelationshipType cloneHasShadowCopy;

    private RelationshipType isCorrectionOf;

    private RelationshipType cloneIsCorrectionOf;

    private RelationshipType isCloneOf;

    private RelationshipType isPersonOwner;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection directorioPersons;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    private MetadataFieldService metadataFieldService;

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

        EntityType personType = createEntityType("Person");
        EntityType cvPersonType = createEntityType("CvPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");

        cloneHasShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        isCorrectionOf = createIsCorrectionOfRelationship(personType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvPersonCloneType);
        isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        isPersonOwner = createRelationshipTypeBuilder(context, cvPersonType, personType, "isPersonOwner",
            "isOwnedByCvPerson", 0, null, 0, null).build();

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
        collectionRoleService.deleteByCollection(context, cvCloneCollection);
        collectionRoleService.deleteByCollection(context, directorioPersons);
        workflowItemService.deleteByCollection(context, cvCloneCollection);
        workflowItemService.deleteByCollection(context, directorioPersons);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void testClaimedProfileEditWhenSynchronizationIsDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp","education", null, "First education");
        addMetadata(profile, "crisrp","education", "start", "2015-01-01");
        addMetadata(profile, "crisrp","education", "end", "2018-01-01");
        addMetadata(profile, "crisrp","education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        addMetadata(profile, "crisrp", "education", null, "Second education");
        addMetadata(profile, "crisrp", "education", "start", "2018-01-02");
        addMetadata(profile, "crisrp", "education", "end", PLACEHOLDER);
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", PLACEHOLDER);
        addMetadata(profile, "perucris", "education", "country", "England");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "First education", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "Second education", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2018-01-02", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "England", 1)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp","education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(person, isCorrectionOf), empty());
    }

    @Test
    public void testClaimedProfileEditWhenSynchronizationIsSetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "education", null, "First education");
        addMetadata(profile, "crisrp", "education", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "education", "end", "2018-01-01");
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        addMetadata(profile, "crisrp", "education", null, "Second education");
        addMetadata(profile, "crisrp", "education", "start", "2018-01-02");
        addMetadata(profile, "crisrp", "education", "end", PLACEHOLDER);
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", PLACEHOLDER);
        addMetadata(profile, "perucris", "education", "country", "England");

        addMetadata(profile, "perucris", "cvPerson", "syncEducation", "false");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "First education", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "Second education", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2018-01-02", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "England", 1)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(person, isCorrectionOf), empty());
    }

    @Test
    public void testClaimedProfileEditWhenEducationSynchronizationIsEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "education", null, "First education");
        addMetadata(profile, "crisrp", "education", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "education", "end", "2018-01-01");
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        addMetadata(profile, "crisrp", "education", null, "Second education");
        addMetadata(profile, "crisrp", "education", "start", "2018-01-02");
        addMetadata(profile, "crisrp", "education", "end", PLACEHOLDER);
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", PLACEHOLDER);
        addMetadata(profile, "perucris", "education", "country", "England");

        addMetadata(profile, "perucris", "subject", "ocde", "OCDE");

        addMetadata(profile, "perucris", "cvPerson", "syncEducation", "true");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "First education", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "Second education", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2018-01-02", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "England", 1)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());
        assertThat(getMetadata(profileClone, "perucris", "subject", "ocde"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());
        assertThat(getMetadata(person, "perucris", "subject", "ocde"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education", "First education", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education", "Second education", 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2018-01-02", 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.end", PLACEHOLDER, 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.grantor", PLACEHOLDER, 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.country", "England", 1)));

        assertThat(getMetadata(cloneCorrection, "perucris", "subject", "ocde"), empty());

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(personCorrection.isArchived(), is(false));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education", "First education", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education", "Second education", 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2018-01-02", 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.end", PLACEHOLDER, 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.grantor", PLACEHOLDER, 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.country", "England", 1)));

        assertThat(getMetadata(personCorrection, "perucris", "subject", "ocde"), empty());
    }

    @Test
    public void testClaimedProfileEditWhenAffiliationSynchronizationIsEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "education", null, "Education");
        addMetadata(profile, "crisrp", "education", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "education", "end", "2018-01-01");
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        addMetadata(profile, "oairecerif", "person", "affiliation", "4Science");
        addMetadata(profile, "oairecerif", "affiliation", "startDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "endDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "role", "Researcher");

        addMetadata(profile, "perucris", "cvPerson", "syncAffiliation", "true");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();

        assertThat(getMetadata(cloneCorrection, "crisrp", "education", null), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(cloneCorrection, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(cloneCorrection, "perucris", "education", "country"), empty());

        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();

        assertThat(getMetadata(personCorrection, "crisrp", "education", null), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(personCorrection, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(personCorrection, "perucris", "education", "country"), empty());

        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));
    }

    @Test
    public void testClaimedProfileEditWhenQualificationSynchronizationIsEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "qualification", null, "First qualification");
        addMetadata(profile, "crisrp", "qualification", "orgunit", "Group");
        addMetadata(profile, "crisrp", "qualification", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "qualification", "end", "2018-01-01");

        addMetadata(profile, "crisrp", "qualification", null, "Second qualification");
        addMetadata(profile, "crisrp", "qualification", "orgunit", "Group");
        addMetadata(profile, "crisrp", "qualification", "start", "2018-01-02");
        addMetadata(profile, "crisrp", "qualification", "end", PLACEHOLDER);

        addMetadata(profile, "perucris", "cvPerson", "syncQualification", "true");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification", "First qualification", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification", "Second qualification", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.start", "2018-01-02", 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.end", PLACEHOLDER, 1)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 1)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "orgunit"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "orgunit"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification", "First qualification", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.start", "2015-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.end", "2018-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification", "Second qualification", 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.start", "2018-01-02", 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.end", PLACEHOLDER, 1)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 1)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification", "First qualification", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.start", "2015-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.end", "2018-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification", "Second qualification", 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.start", "2018-01-02", 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.end", PLACEHOLDER, 1)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 1)));
    }

    @Test
    public void testClaimedProfileEditWhenBasicInfoSynchronizationIsEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "qualification", null, "Qualification");
        addMetadata(profile, "crisrp", "qualification", "orgunit", "Group");
        addMetadata(profile, "crisrp", "qualification", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "qualification", "end", "2018-01-01");

        addMetadata(profile, "perucris", "subject", "ocde", "OCDE");
        addMetadata(profile, "perucris", "address", "streetAddress", "My street");

        addMetadata(profile, "perucris", "cvPerson", "syncBasicInfo", "true");

        updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification", "Qualification", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.qualification.orgunit", "Group", 0)));

        assertThat(profile.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.address.streetAddress", "My street", 0)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "qualification", "orgunit"), empty());

        assertThat(getMetadata(profileClone, "perucris", "subject", "ocde"), empty());
        assertThat(getMetadata(profileClone, "perucris", "address", "streetAddress"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "qualification", "orgunit"), empty());

        assertThat(getMetadata(person, "perucris", "subject", "ocde"), empty());
        assertThat(getMetadata(person, "perucris", "address", "streetAddress"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(getMetadata(cloneCorrection, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "qualification", "orgunit"), empty());
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.address.streetAddress", "My street", 0)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(getMetadata(personCorrection, "crisrp", "qualification", null), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "qualification", "start"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "qualification", "end"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "qualification", "orgunit"), empty());
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.address.streetAddress", "My street", 0)));

    }

    @Test
    public void testClaimedProfileEditWhenEducationAndAffiliationSynchronizationAreEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "crisrp", "education", null, "Education");
        addMetadata(profile, "crisrp", "education", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "education", "end", "2018-01-01");
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        addMetadata(profile, "oairecerif", "person", "affiliation", "4Science");
        addMetadata(profile, "oairecerif", "affiliation", "startDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "endDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "role", "Researcher");

        addMetadata(profile, "perucris", "cvPerson", "syncAffiliation", "true");

        addMetadata(profile, "perucris", "cvPerson", "syncEducation", "true");

        profile = updateItem(profile);

        assertThat(profile.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(profile.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(profile.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(profile.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));
    }

    @Test
    public void testClaimedProfileEditWithManyConsecutiveCorrections() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        /**
         * Add education without synchronization.
         */

        addMetadata(profile, "crisrp", "education", null, "Education");
        addMetadata(profile, "crisrp", "education", "start", "2015-01-01");
        addMetadata(profile, "crisrp", "education", "end", "2018-01-01");
        addMetadata(profile, "crisrp", "education", "role", "Role");
        addMetadata(profile, "perucris", "education", "grantor", "Grantor");
        addMetadata(profile, "perucris", "education", "country", "Italy");

        profile = updateItem(profile);

        /**
         * Add affiliation with synchronization.
         */

        addMetadata(profile, "oairecerif", "person", "affiliation", "4Science");
        addMetadata(profile, "oairecerif", "affiliation", "startDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "endDate", PLACEHOLDER);
        addMetadata(profile, "oairecerif", "affiliation", "role", "Researcher");

        addMetadata(profile, "perucris", "cvPerson", "syncAffiliation", "true");

        profile = updateItem(profile);

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();

        assertThat(getMetadata(cloneCorrection, "crisrp", "education", null), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(cloneCorrection, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(cloneCorrection, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(cloneCorrection, "perucris", "education", "country"), empty());

        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();

        assertThat(getMetadata(personCorrection, "crisrp", "education", null), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(personCorrection, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(personCorrection, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(personCorrection, "perucris", "education", "country"), empty());

        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        /**
         * Add education's synchronization.
         */

        addMetadata(profile, "perucris", "cvPerson", "syncEducation", "true");

        profile = updateItem(profile);

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(profileClone, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(profileClone, "perucris", "education", "country"), empty());

        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(getMetadata(person, "crisrp", "education", "start"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "end"), empty());
        assertThat(getMetadata(person, "crisrp", "education", "role"), empty());
        assertThat(getMetadata(person, "perucris", "education", "grantor"), empty());
        assertThat(getMetadata(person, "perucris", "education", "country"), empty());

        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(personCorrection), nullValue());

        cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));

        personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education", "Education", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.start", "2015-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.end", "2018-01-01", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("crisrp.education.role", "Role", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.grantor", "Grantor", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.education.country", "Italy", 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0, 400)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER, 0)));
        assertThat(personCorrection.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher", 0)));
    }

    @Test
    public void testClaimedProfileEditWithChangeReversion() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        Item profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        Item profileClone = findRelations.get(0).getLeftItem();

        context.restoreAuthSystemState();

        addMetadata(profile, "perucris", "subject", "ocde", "OCDE");
        addMetadata(profile, "perucris", "cvPerson", "syncBasicInfo", "true");

        profile = updateItem(profile);
        assertThat(profile.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));

        profileClone = reloadItem(profileClone);
        assertThat(getMetadata(profileClone, "perucris", "subject", "ocde"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "perucris", "subject", "ocde"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
        assertThat(personCorrection.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE", 0)));

        removeMetadata(profile, "perucris", "subject", "ocde");

        profile = updateItem(profile);
        assertThat(getMetadata(profile, "perucris", "subject", "ocde"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "perucris", "subject", "ocde"), empty());

        assertThat(reloadItem(cloneCorrection), nullValue());
        assertThat(reloadItem(personCorrection), nullValue());

        assertThat(findRelations(profileClone, cloneIsCorrectionOf), empty());
        assertThat(findRelations(person, isCorrectionOf), empty());

    }

    @Test
    public void testNewProfileSubmissionWithSynchronizationDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test profile")
            .withBirthDate("1992-06-26")
            .withPersonAffiliation("4Science")
            .withPersonAffiliationStartDate("2020-07-01")
            .withPersonAffiliationEndDate(PLACEHOLDER)
            .withPersonAffiliationRole("Researcher")
            .build();

        context.restoreAuthSystemState();

        assertThat(findRelations(profile, isCloneOf), empty());

    }

    @Test
    public void testNewProfileSubmissionWithOneSectionSynchronizationEnabled() throws Exception {

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

        context.restoreAuthSystemState();

        List<Relationship> cloneRelations = findRelations(profile, isCloneOf);
        assertThat(cloneRelations, hasSize(1));
        Item profileClone = cloneRelations.get(0).getLeftItem();

        assertThat(profileClone.isArchived(), is(false));
        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());
        assertThat(profileClone.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(profileClone.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

        List<Relationship> cloneShadowCopyRelations = findRelations(profileClone, cloneHasShadowCopy);
        assertThat(cloneShadowCopyRelations, hasSize(1));
        Item person = cloneShadowCopyRelations.get(0).getRightItem();

        assertThat(person.isArchived(), is(false));
        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());
        assertThat(person.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(person.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

        List<Relationship> isPersonOwnerRelations = findRelations(profile, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getRightItem(), equalTo(person));

    }

    @Test
    public void testNewProfileSubmissionWithManySectionSynchronizationEnabled() throws Exception {

        context.turnOffAuthorisationSystem();

        Item profile = ItemBuilder.createItem(context, cvCollection)
            .withTitle("Test profile")
            .withBirthDate("1992-06-26")
            .withPersonAffiliation("4Science")
            .withPersonAffiliationStartDate("2020-07-01")
            .withPersonAffiliationEndDate(PLACEHOLDER)
            .withPersonAffiliationRole("Researcher")
            .withPersonEducation("High school")
            .withCvPersonBasicInfoSyncEnabled(true)
            .withCvPersonAffiliationSyncEnabled(true)
            .build();

        context.restoreAuthSystemState();

        List<Relationship> cloneRelations = findRelations(profile, isCloneOf);
        assertThat(cloneRelations, hasSize(1));
        Item profileClone = cloneRelations.get(0).getLeftItem();

        assertThat(profileClone.isArchived(), is(false));
        assertThat(profileClone.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0,
                                                            Choices.CF_UNSET)));
        assertThat(profileClone.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", "2020-07-01")));
        assertThat(profileClone.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER)));
        assertThat(profileClone.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher")));
        assertThat(profileClone.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(profileClone.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(getMetadata(profileClone, "crisrp", "education", null), empty());

        List<Relationship> cloneShadowCopyRelations = findRelations(profileClone, cloneHasShadowCopy);
        assertThat(cloneShadowCopyRelations, hasSize(1));
        Item person = cloneShadowCopyRelations.get(0).getRightItem();

        assertThat(person.isArchived(), is(false));
        assertThat(person.getMetadata(), hasItem(with("oairecerif.person.affiliation", "4Science", 0,
                                                      Choices.CF_UNSET)));
        assertThat(person.getMetadata(), hasItem(with("oairecerif.affiliation.startDate", "2020-07-01")));
        assertThat(person.getMetadata(), hasItem(with("oairecerif.affiliation.endDate", PLACEHOLDER)));
        assertThat(person.getMetadata(), hasItem(with("oairecerif.affiliation.role", "Researcher")));
        assertThat(person.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(person.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(getMetadata(person, "crisrp", "education", null), empty());

        List<Relationship> isPersonOwnerRelations = findRelations(profile, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getRightItem(), equalTo(person));

    }

    @Test
    public void testNewProfileSubmissionWithSubsequentModificationToSynchronize() throws Exception {

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

        context.restoreAuthSystemState();

        List<Relationship> cloneRelations = findRelations(profile, isCloneOf);
        assertThat(cloneRelations, hasSize(1));
        Item profileClone = cloneRelations.get(0).getLeftItem();
        assertThat(profileClone.isArchived(), is(false));

        List<Relationship> cloneShadowCopyRelations = findRelations(profileClone, cloneHasShadowCopy);
        assertThat(cloneShadowCopyRelations, hasSize(1));
        Item person = cloneShadowCopyRelations.get(0).getRightItem();
        assertThat(person.isArchived(), is(false));

        context.turnOffAuthorisationSystem();
        addMetadata(profile, "perucris", "subject", "ocde", "OCDE");
        profile = updateItem(profile);
        context.restoreAuthSystemState();

        assertThat(reloadItem(profileClone), nullValue());
        assertThat(reloadItem(person), nullValue());

        cloneRelations = findRelations(profile, isCloneOf);
        assertThat(cloneRelations, hasSize(1));
        Item newProfileClone = cloneRelations.get(0).getLeftItem();

        assertThat(newProfileClone.isArchived(), is(false));
        assertThat(getMetadata(newProfileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(newProfileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(newProfileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(newProfileClone, "oairecerif", "affiliation", "role"), empty());
        assertThat(newProfileClone.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(newProfileClone.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(newProfileClone.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE")));

        cloneShadowCopyRelations = findRelations(newProfileClone, cloneHasShadowCopy);
        assertThat(cloneShadowCopyRelations, hasSize(1));
        Item newPerson = cloneShadowCopyRelations.get(0).getRightItem();

        assertThat(newPerson.isArchived(), is(false));
        assertThat(getMetadata(newPerson, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(newPerson, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(newPerson, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(newPerson, "oairecerif", "affiliation", "role"), empty());
        assertThat(newPerson.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(newPerson.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(newPerson.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE")));

    }

    @Test
    public void testNewProfileSubmissionWithSubsequentModificationNotToBeSynchronized() throws Exception {

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

        context.restoreAuthSystemState();

        List<Relationship> cloneRelations = findRelations(profile, isCloneOf);
        assertThat(cloneRelations, hasSize(1));
        Item profileClone = cloneRelations.get(0).getLeftItem();
        assertThat(profileClone.isArchived(), is(false));

        List<Relationship> cloneShadowCopyRelations = findRelations(profileClone, cloneHasShadowCopy);
        assertThat(cloneShadowCopyRelations, hasSize(1));
        Item person = cloneShadowCopyRelations.get(0).getRightItem();
        assertThat(person.isArchived(), is(false));

        context.turnOffAuthorisationSystem();
        addMetadata(profile, "crisrp", "education", null, "High school");
        profile = updateItem(profile);
        context.restoreAuthSystemState();

        assertThat(reloadItem(profileClone), notNullValue());
        assertThat(profileClone.isArchived(), is(false));
        assertThat(getMetadata(profileClone, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(profileClone, "oairecerif", "affiliation", "role"), empty());
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(profileClone.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(profileClone.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

        assertThat(reloadItem(person), notNullValue());
        assertThat(person.isArchived(), is(false));
        assertThat(getMetadata(person, "oairecerif", "person", "affiliation"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "startDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "endDate"), empty());
        assertThat(getMetadata(person, "oairecerif", "affiliation", "role"), empty());
        assertThat(getMetadata(person, "crisrp", "education", null), empty());
        assertThat(person.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(person.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));

    }

    @Test
    public void testCvProfileCorrectionsWithSyncEnabled() throws Exception {

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

        Item cvPersonClone = cloneWorkflowItems.get(0).getItem();
        installItemService.installItem(context, cloneWorkflowItems.get(0));

        context.commit();
        profile = reloadItem(profile);

        addMetadata(profile, "perucris", "subject", "ocde", "OCDE");
        profile = updateItem(profile);

        context.restoreAuthSystemState();

        cvPersonClone = reloadItem(cvPersonClone);
        assertThat(getMetadata(cvPersonClone, "perucris", "subject", "ocde"), empty());

        person = reloadItem(person);
        assertThat(getMetadata(person, "perucris", "subject", "ocde"), empty());

        List<Relationship> cloneCorrectionRelations = findRelations(cvPersonClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
        assertThat(cloneCorrection.isArchived(), is(false));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(cloneCorrection.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE")));

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item correction = personCorrectionRelations.get(0).getLeftItem();
        assertThat(correction.isArchived(), is(false));
        assertThat(correction.getMetadata(), hasItem(with("dc.title", "Test profile")));
        assertThat(correction.getMetadata(), hasItem(with("person.birthDate", "1992-06-26")));
        assertThat(correction.getMetadata(), hasItem(with("perucris.subject.ocde", "OCDE")));

    }

    private Item updateItem(Item item) throws Exception {
        itemService.update(context, item);
        context.commit();
        return context.reloadEntity(item);
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

    private MetadataField metadataField(String schema, String element, String qualifier) throws SQLException {
        return metadataFieldService.findByElement(context, schema, element, qualifier);
    }
}
