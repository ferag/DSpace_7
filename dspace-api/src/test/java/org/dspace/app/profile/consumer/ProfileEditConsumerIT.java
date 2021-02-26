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
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CORRECTION;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
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
 * Integration tests for {@link ProfileEditConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ProfileEditConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String PLACEHOLDER = PLACEHOLDER_PARENT_METADATA_VALUE;

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private ItemService itemService;

    private ResearcherProfileService researcherProfileService;

    private CollectionRoleService collectionRoleService;

    private XmlWorkflowItemService workflowItemService;

    private EPerson submitter;

    private Group directorioEditorGroup;

    private RelationshipType isCorrectionOf;

    private RelationshipType cloneIsCorrectionOf;

    private RelationshipType isCloneOf;

    private Community directorioCommunity;

    private Community ctiVitaeCommunity;

    private Community ctiVitaeCloneCommunity;

    private Collection directorioPersons;

    private Collection cvCollection;

    private Collection cvCloneCollection;

    private Item person;

    private Item profile;

    private Item profileClone;

    @Before
    public void before() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionRoleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonType = createEntityType("CvPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        isCorrectionOf = createIsCorrectionOfRelationship(personType);
        cloneIsCorrectionOf = createIsCorrectionOfRelationship(cvPersonCloneType);
        isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        createRelationshipTypeBuilder(context, cvPersonType, personType, "isPersonOwner",
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
        configurationService.setProperty("cti-vitae.clone.profile-collection-id", cvCloneCollection.getID().toString());
        configurationService.setProperty("item.enable-virtual-metadata", false);

        person = ItemBuilder.createItem(context, directorioPersons)
            .withTitle("White, Walter")
            .build();

        DSpaceServicesFactory.getInstance().getRequestService().startRequest();
        ResearcherProfile researcherProfile = researcherProfileService.createFromSource(context, submitter,
            URI.create("http://localhost:8080/server/api/core/items/" + person.getID()));

        profile = researcherProfile.getItem();
        List<Relationship> findRelations = findRelations(profile, isCloneOf);
        assertThat(findRelations, hasSize(1));

        profileClone = findRelations.get(0).getLeftItem();

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
    public void testProfileEditConsumerWhenSynchronizationIsDisabled() throws Exception {

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
    public void testProfileEditConsumerWhenSynchronizationIsSetToFalse() throws Exception {

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
    public void testProfileEditConsumerWhenEducationSynchronizationIsEnabled() throws Exception {

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

        List<Relationship> cloneCorrectionRelations = findRelations(profileClone, cloneIsCorrectionOf);
        assertThat(cloneCorrectionRelations, hasSize(1));

        Item cloneCorrection = cloneCorrectionRelations.get(0).getLeftItem();
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

        List<Relationship> personCorrectionRelations = findRelations(person, isCorrectionOf);
        assertThat(personCorrectionRelations, hasSize(1));

        Item personCorrection = personCorrectionRelations.get(0).getLeftItem();
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
    }

    @Test
    public void testProfileEditConsumerWhenAffiliationSynchronizationIsEnabled() throws Exception {

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
    public void testProfileEditConsumerWhenQualificationSynchronizationIsEnabled() throws Exception {

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
    public void testProfileEditConsumerWhenEducationAndAffiliationSynchronizationAreEnabled() throws Exception {

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
    public void testProfileEditConsumerWithManyConsecutiveCorrections() throws Exception {

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

    private Item updateItem(Item item) throws Exception {
        itemService.update(context, item);
        context.commit();
        return context.reloadEntity(item);
    }

    private void addMetadata(Item item, String schema, String element, String qualifier, String value)
        throws SQLException {
        itemService.addMetadata(context, item, schema, element, qualifier, null, value);
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
}
