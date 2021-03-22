/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * This IT verifies the scenarios of PGC Custom CTIVitae community entities that can be related with
 * Directorio community entities.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CtiVitaeEntitiesIndexIT extends AbstractIntegrationTestWithDatabase {

    private final SearchService searchService = SearchUtils.getSearchService();

    private final ConfigurationService configurationService = DSpaceServicesFactory
        .getInstance().getConfigurationService();

    private Collection publications;
    private Collection people;
    private Collection cvPublications;
    private Collection cvProfiles;
    private Collection cvPublicationsClone;
    private RelationshipType cvPublicationCloneToCvPublication;
    private RelationshipType cvPublicationCloneToPublication;
    private RelationshipType cvPersonToPerson;
    private RelationshipType mergedPublicationRelationship;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        Community directorio = CommunityBuilder.createCommunity(context).build();
        Community ctiVitae = CommunityBuilder.createCommunity(context).withName("CTI Vitae").build();

        publications = CollectionBuilder.createCollection(context, directorio)
            .withRelationshipType("Publication").build();

        people = CollectionBuilder.createCollection(context, directorio)
            .withRelationshipType("Person").build();

        cvPublications = CollectionBuilder.createCollection(context, ctiVitae)
            .withRelationshipType("CvPublication").build();

        cvProfiles = CollectionBuilder.createCollection(context, ctiVitae)
            .withRelationshipType("CvPerson").build();

        cvPublicationsClone = CollectionBuilder.createCollection(context, ctiVitae)
            .withRelationshipType("CvPublicationClone").build();

        configurationService.setProperty("directorios.community-id", directorio.getID().toString());

        EntityType cvPublicationClone =
            EntityTypeBuilder.createEntityTypeBuilder(context, "CvPublicationClone").build();
        EntityType cvPublication = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPublication").build();
        EntityType cvPerson = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        cvPublicationCloneToCvPublication =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, cvPublicationClone, cvPublication,
                "isCloneOfItem", "isClonedByItem", 0, 1, 0, 1)
                .build();

        cvPublicationCloneToPublication =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, cvPublicationClone, publication,
                "hasShadowCopy", "isShadowCopy", 0, 1, 0, 1)
                .build();

        cvPersonToPerson = RelationshipTypeBuilder.createRelationshipTypeBuilder(context, cvPerson, person,
            "isPersonOwner", "isOwnedByCvPerson", 0, 1, 0, 1)
            .build();

        mergedPublicationRelationship =
            RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, publication,
                "isMergedIn", "isMergeOf", 0, 999, 0, 999).build();

        context.restoreAuthSystemState();
    }

    /**
     * CTIVitae Publication is indexed with ctivitae.owner field
     *
     * @throws SQLException
     * @throws SearchServiceException
     */
    @Test
    public void cvPublicationIndexed() throws SQLException, SearchServiceException {

        context.turnOffAuthorisationSystem();


        EPerson owner = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Owner", "Smith")
            .withEmail("foo@bar.com").build();

        Item cvProfile = ItemBuilder.createItem(context, cvProfiles)
            .withCrisOwner(owner.getName(), owner.getID().toString())
            .build();

        Item cvPublication = ItemBuilder.createItem(context, cvPublications)
            .withCrisOwner(owner.getName(), owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        DiscoverResult discoverResult = discoveryLookup(cvPublication.getID(), "ctivitae.owner", "search.resourceid");
        IndexableObject idxObj = discoverResult.getIndexableObjects().get(0);
        assertThat(discoverResult.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("ctivitae.owner").get(0), is(cvProfile.getID().toString()));

    }

    /**
     * A publication without references into the CTIVitae should be indexed accordingly, without
     * any reference to CTIVitae collection (no ctivitae.owner field).
     */
    @Test
    public void directorioPublicationWithoutCtiVitaeReference() throws SearchServiceException {
        context.turnOffAuthorisationSystem();

        Item author = ItemBuilder.createItem(context, people)
            .withTitle("Smith John")
            .build();
        Item editor = ItemBuilder.createItem(context, people)
            .withTitle("Doe Jane")
            .build();

        Item publication = ItemBuilder.createItem(context, publications)
            .withAuthor(author.getName(), UUIDUtils.toString(author.getID()))
            .withEditor(editor.getName(), UUIDUtils.toString(editor.getID()))
            .build();

        context.restoreAuthSystemState();

        DiscoverResult idLookup = discoveryLookup(publication.getID(), "search.resourceid", "search.resourceid");
        IndexableObject idxObj = idLookup.getIndexableObjects().get(0);
        assertThat(idLookup.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("search.resourceid").get(0), is(publication.getID().toString()));

        try {
            discoveryLookup(publication.getID(), "ctivitae.owner", "search.resourceid");
        } catch (Exception e) {
            assertThat(e.getClass().getName(), is(SearchServiceException.class.getName()));
        }
    }

    @Test
    public void directorioPublicationWithReferenceIndexed() throws SQLException, SearchServiceException {

        context.turnOffAuthorisationSystem();

        Item author = ItemBuilder.createItem(context, people)
            .withFullName("First Author").build();

        EPerson authorEperson = EPersonBuilder.createEPerson(context)
            .withEmail("first@mailinator.com").build();

        Item authorCvPerson = buildCvPerson(author, authorEperson);

        Item publication = ItemBuilder.createItem(context, publications)
            .withAuthor(author.getName(), UUIDUtils.toString(author.getID()))
            .build();

        context.restoreAuthSystemState();

        DiscoverResult discoverResult = discoveryLookup(publication.getID(), "ctivitae.owner", "search.resourceid");
        IndexableObject idxObj = discoverResult.getIndexableObjects().get(0);
        assertThat(discoverResult.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("search.resourceid").get(0), is(publication.getID().toString()));
        assertThat(discoverResult.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("ctivitae.owner").get(0), is(authorCvPerson.getID().toString()));


    }

    /**
     * Given a publication with two authors:
     * - an author that has a CTIVitae profile and a clone of this publication in his CTIVitae space
     * - two authors that have a CTIVitae profile but not a clone of this publication in their CTIVitae space
     * - an editor that hasn't a CTIVitae profile
     * - an editor that hasn't neither a CTIVitae profile nor an authority
     * - two editors that have a CTIVitae profile and a clone of this publication in their CTIVitae space
     * <p>
     * When indexing is performed
     * <p>
     * Then:
     * - Directorio solr document has ctivitae owner references to second and third authors
     * - CTIVitae publication of first author has ctivitae owner reference to first author
     * - CTIVitae publication of third editor has a ctivitae owner reference to third editor
     * - CTIVitae publication of fourth editor has a ctivitae owner reference to fourth editor
     *
     * @throws SQLException
     * @throws SearchServiceException
     */
    @Test
    public void directorioPublicationWithPartialReferences() throws SQLException, SearchServiceException {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, people)
            .withFullName("First Author").build();

        EPerson firstAuthorEPerson = EPersonBuilder.createEPerson(context)
            .withEmail("first@mailinator.com").build();

        Item firstAuthorCvPerson = buildCvPerson(firstAuthor, firstAuthorEPerson);

        Item firstAuthorCvPublication = buildCvPublicationOwnedBy(firstAuthorEPerson);

        Item firstAuthorCvPublicationClone = buildClone(cvPublicationsClone);

        RelationshipBuilder.createRelationshipBuilder(context, firstAuthorCvPublicationClone,
            firstAuthorCvPublication, cvPublicationCloneToCvPublication);

        Item secondAuthor = ItemBuilder.createItem(context, people)
            .withFullName("Second Author").build();

        EPerson secondAuthorEPerson = EPersonBuilder.createEPerson(context)
            .withEmail("second@mailinator.com").build();

        Item secondAuthorCvPerson = buildCvPerson(secondAuthor, secondAuthorEPerson);

        Item thirdAuthor = ItemBuilder.createItem(context, people)
            .withFullName("Third Author").build();

        EPerson thirdAuthorEPerson = EPersonBuilder.createEPerson(context)
            .withEmail("third@mailinator.com").build();

        Item thirdAuthorCvPerson = buildCvPerson(thirdAuthor, thirdAuthorEPerson);

        Item firstEditor = ItemBuilder.createItem(context, people)
            .withFullName("First Editor").build();

        Item thirdEditor = ItemBuilder.createItem(context, people)
            .withFullName("Third Editor").build();

        EPerson thirdEditorEPerson = EPersonBuilder.createEPerson(context)
            .withEmail("third.editor@mailinator.com").build();

        Item thirdEditorCvPerson = buildCvPerson(thirdEditor, thirdEditorEPerson);

        Item thirdEditorCvPublication = ItemBuilder.createItem(context, cvPublications)
            .withCrisOwner(thirdEditorEPerson.getEmail(), UUIDUtils.toString(thirdEditorEPerson.getID()))
            .build();

        Item thirdEditorCvPublicationClone = buildClone(cvPublicationsClone);

        Item thirdEditorPublicationMerged = buildClone(publications);

        RelationshipBuilder.createRelationshipBuilder(context, thirdEditorCvPublicationClone,
            thirdEditorPublicationMerged, cvPublicationCloneToPublication);

        RelationshipBuilder.createRelationshipBuilder(context, thirdEditorCvPublicationClone,
            thirdEditorCvPublication, cvPublicationCloneToCvPublication);

        Item fourthEditor = ItemBuilder.createItem(context, people)
            .withFullName("fourth Editor").build();

        EPerson fourthEditorEPerson = EPersonBuilder.createEPerson(context)
            .withEmail("fourth.editor@mailinator.com").build();

        Item fourthEditorCvPerson = buildCvPerson(fourthEditor, fourthEditorEPerson);

        Item fourthEditorCvPublication = ItemBuilder.createItem(context, cvPublications)
            .withCrisOwner(fourthEditorEPerson.getEmail(), UUIDUtils.toString(fourthEditorEPerson.getID()))
            .build();

        Item fourthEditorCvPublicationClone = buildClone(cvPublicationsClone);

        Item fourthEditorPublicationMerged = buildClone(publications);

        RelationshipBuilder.createRelationshipBuilder(context, fourthEditorCvPublicationClone,
            fourthEditorPublicationMerged, cvPublicationCloneToPublication);

        RelationshipBuilder.createRelationshipBuilder(context, fourthEditorCvPublicationClone,
            fourthEditorCvPublication, cvPublicationCloneToCvPublication);


        Item publication = ItemBuilder.createItem(context, publications)
            .withAuthor(firstAuthor.getName(), UUIDUtils.toString(firstAuthor.getID()))
            .withAuthor(secondAuthor.getName(), UUIDUtils.toString(secondAuthor.getID()))
            .withAuthor(thirdAuthor.getName(), UUIDUtils.toString(thirdAuthor.getID()))
            .withEditor(firstEditor.getName(), UUIDUtils.toString(firstEditor.getID()))
            .withEditor("Second Editor")
            .withEditor(thirdEditor.getName(), UUIDUtils.toString(thirdEditor.getID()))
            .withEditor(fourthEditor.getName(), UUIDUtils.toString(fourthEditor.getID()))
            .build();


        RelationshipBuilder.createRelationshipBuilder(context, firstAuthorCvPublicationClone,
            publication, cvPublicationCloneToPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, thirdEditorPublicationMerged,
            publication, mergedPublicationRelationship).build();
        RelationshipBuilder.createRelationshipBuilder(context, fourthEditorPublicationMerged,
            publication, mergedPublicationRelationship).build();


        context.restoreAuthSystemState();

        checkCtiVitaeOwnerFieldValueIs(publication.getID(), secondAuthorCvPerson.getID(),
            thirdAuthorCvPerson.getID());
        checkCtiVitaeOwnerFieldValueIs(firstAuthorCvPublication.getID(),
            firstAuthorCvPerson.getID());
        checkCtiVitaeOwnerFieldValueIs(thirdEditorCvPublication.getID(),
            thirdEditorCvPerson.getID());
        checkCtiVitaeOwnerFieldValueIs(fourthEditorCvPublication.getID(),
            fourthEditorCvPerson.getID());

    }

    private Item buildClone(Collection cvPublicationsClone) {
        return ItemBuilder.createItem(context, cvPublicationsClone)
            .build();
    }

    private Item buildCvPublicationOwnedBy(EPerson firstAuthorEPerson) {
        return ItemBuilder.createItem(context, cvPublications)
            .withCrisOwner(firstAuthorEPerson.getEmail(), UUIDUtils.toString(firstAuthorEPerson.getID()))
            .build();
    }

    private Item buildCvPerson(Item firstAuthor, EPerson firstAuthorEPerson) {
        Item firstAuthorCvPerson = ItemBuilder.createItem(context, cvProfiles)
            .withCrisOwner(firstAuthorEPerson.getEmail(), UUIDUtils.toString(firstAuthorEPerson.getID()))
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, firstAuthorCvPerson, firstAuthor,
            cvPersonToPerson).build();
        return firstAuthorCvPerson;
    }

    private void checkCtiVitaeOwnerFieldValueIs(UUID itemId, UUID... ids)
        throws SearchServiceException {
        DiscoverResult discoverResult = discoveryLookup(itemId, "ctivitae.owner");

        IndexableObject idxObj = discoverResult.getIndexableObjects().get(0);

        List<String> ownderIds = Arrays.stream(ids).map(UUIDUtils::toString).collect(Collectors.toList());

        assertThat(discoverResult.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("ctivitae.owner").size(), is(ownderIds.size()));

        assertTrue(discoverResult.getSearchDocument(idxObj).get(0).getSearchFields()
            .get("ctivitae.owner").containsAll(ownderIds));
    }

    private DiscoverResult discoveryLookup(UUID resourceId, String... searchFields) throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.addFilterQueries("search.resourceid:" + UUIDUtils.toString(resourceId));
        Arrays.stream(searchFields).forEach(discoverQuery::addSearchField);
        return searchService.search(context, discoverQuery);
    }
}
