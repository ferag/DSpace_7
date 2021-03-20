/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.ctivitae;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CvRelatedEntitiesService}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class CvRelatedEntitiesServiceTest {

    private CvRelatedEntitiesService service;
    private final ConcytecWorkflowService concytecWorkflowService = mock(ConcytecWorkflowService.class);
    private final ItemService itemService = mock(ItemService.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final RelationshipTypeService relationshipTypeService = mock(RelationshipTypeService.class);
    private final Context context = mock(Context.class);
    private final RelationshipType profileOwnershipRelation = mock(RelationshipType.class);

    @Before
    public void setUp() throws Exception {
        service = new CvRelatedEntitiesService(concytecWorkflowService, itemService, relationshipService,
            relationshipTypeService);
    }

    @Test
    public void itemNotRelatedWithDirectorio() throws SQLException {

        Item item = item(randomUUID(), "Publication");
        when(concytecWorkflowService.findClone(context, item)).thenReturn(null);
        Optional<Item> directorioRelated = service.findDirectorioRelated(context, item);

        assertThat(directorioRelated, is(Optional.empty()));
    }

    @Test
    public void itemRelatedWithDirectorio() throws SQLException {

        Item item = item(randomUUID(), "Publication");
        Item clone = item(randomUUID(), "Publication");
        Item cloneShadow = item(randomUUID(), "Publication");

        when(concytecWorkflowService.findClone(context, item)).thenReturn(clone);
        when(concytecWorkflowService.findShadowItemCopy(context, clone)).thenReturn(cloneShadow);

        Optional<Item> directorioRelated = service.findDirectorioRelated(context, item);

        assertThat(directorioRelated, is(Optional.of(cloneShadow)));
    }

    @Test
    public void exceptionWhileDirectorioLookupReturnsEmpty() throws SQLException {

        Item item = item(randomUUID(), "Publication");
        Item clone = item(randomUUID(), "Publication");

        when(concytecWorkflowService.findClone(context, item)).thenReturn(clone);
        doThrow(new SQLException("sql exception"))
            .when(concytecWorkflowService).findShadowItemCopy(context, clone);

        Optional<Item> directorioRelated = service.findDirectorioRelated(context, item);

        assertThat(directorioRelated, is(Optional.empty()));
    }

    @Test
    public void directorioEntityWithNoCtiVitaeRelations() throws SQLException {

        Item directorioItem = item(randomUUID(), "Publication");

        when(concytecWorkflowService.findMergedInItems(context, directorioItem)).thenReturn(emptyList());
        when(concytecWorkflowService.findCopiedItem(context, directorioItem)).thenReturn(null);

        Collection<Item> ctiVitaeRelated = service.findCTIVitaeRelated(context, directorioItem);

        assertThat(ctiVitaeRelated, is(emptyList()));
    }

    @Test
    public void directorioItemRelatedWithSingleCtiVitae() throws SQLException {

        Item directorioItem = item(randomUUID(), "Publication");
        Item cloneItem = item(randomUUID(), "Publication");
        Item ctiVitaeItem = item(randomUUID(), "Publication");

        when(concytecWorkflowService.findMergedInItems(context, directorioItem)).thenReturn(emptyList());
        expectDirectorioToCtiVitaeStructure(directorioItem, cloneItem, ctiVitaeItem);

        Collection<Item> ctiVitaeRelated = service.findCTIVitaeRelated(context, directorioItem);

        assertThat(ctiVitaeRelated, is(singletonList(ctiVitaeItem)));
    }

    @Test
    public void directorioItemRelatedWithManyCtiVitae() throws SQLException {

        Item directorioItem = item(randomUUID(), "Publication");
        Item cloneItem = item(randomUUID(), "Publication");
        Item ctiVitaeItem = item(randomUUID(), "Publication");

        Item mergedItemOne = item(randomUUID(), "Publication");
        Item cloneItemOfMergedOne = item(randomUUID(), "Publication");
        Item ctiVitaeItemOfMergedOne = item(randomUUID(), "Publication");

        Item mergedItemTwo = item(randomUUID(), "Publication");
        Item cloneItemOfMergedTwo = item(randomUUID(), "Publication");
        Item ctiVitaeItemOfMergedTwo = item(randomUUID(), "Publication");

        expectDirectorioToCtiVitaeStructure(directorioItem, cloneItem, ctiVitaeItem);
        expectDirectorioToCtiVitaeStructure(mergedItemOne, cloneItemOfMergedOne, ctiVitaeItemOfMergedOne);
        expectDirectorioToCtiVitaeStructure(mergedItemTwo, cloneItemOfMergedTwo, ctiVitaeItemOfMergedTwo);

        when(concytecWorkflowService.findMergedInItems(context, directorioItem))
            .thenReturn(Arrays.asList(mergedItemOne, mergedItemTwo));

        Collection<Item> ctiVitaeRelated = service.findCTIVitaeRelated(context, directorioItem);

        assertThat(ctiVitaeRelated, is(Arrays.asList(ctiVitaeItem, ctiVitaeItemOfMergedOne, ctiVitaeItemOfMergedTwo)));
    }

    @Test
    public void exceptionDuringDirectorioToCtiVitaeLookup() throws SQLException {

        Item directorioItem = item(randomUUID(), "Publication");

        doThrow(new SQLException("sql exception")).when(concytecWorkflowService)
            .findMergedInItems(context, directorioItem);

        Collection<Item> ctiVitaeRelated = service.findCTIVitaeRelated(context, directorioItem);

        assertThat(ctiVitaeRelated, is(emptyList()));
    }

    @Test
    public void publicationWithoutAuthorAuthority() throws SQLException {

        String entityType = "Publication";
        Map<String, List<String>> map = Collections.singletonMap(entityType,
            singletonList("dc.contributor.author"));

        service.setEntityToMetadataMap(map);
        Item publication = item(randomUUID(), entityType,
            metadataValue("dc.contributor.author", "Rossi Mario", null));

        Collection<Item> ctiVitaeRelatedProfiles =
            service.findCtiVitaeRelatedProfiles(context, publication);

        assertThat(ctiVitaeRelatedProfiles, is(emptyList()));

    }

    @Test
    public void projectWithoutAuthorInCtiVitae() throws SQLException {

        service.setEntityToMetadataMap(singletonMap("Project", Arrays.asList("crispj.investigator",
            "crispj.coinvestigators")));

        Item author = item(randomUUID(), "Person");
        Item project = item(randomUUID(), "Project",
            metadataValue("crispj.investigator", "Rossi Mario", author.getID().toString()));

        when(relationshipTypeService
            .findByItemAndTypeNames(context, author, false, "isPersonOwner", "isOwnedByCvPerson"))
            .thenReturn(singletonList(profileOwnershipRelation));

        when(relationshipService.findByItemAndRelationshipType(context, author, profileOwnershipRelation, false))
            .thenReturn(emptyList());

        Collection<Item> ctiVitaeRelatedProfiles =
            service.findCtiVitaeRelatedProfiles(context, project);

        assertThat(ctiVitaeRelatedProfiles, is(emptyList()));

    }

    @Test
    public void patentWithAuthorInCtiVitae() throws SQLException {

        service.setEntityToMetadataMap(Collections.singletonMap("Patent",
            singletonList("dc.contributor.author")));

        Item author = item(randomUUID(), "Person");

        Item patent = item(randomUUID(), "Patent",
            metadataValue("dc.contributor.author", "Rossi Mario", author.getID().toString()));

        Item ctiVitaeAuthor = item(randomUUID(), "CvPerson");

        expectPersonOwnerRelationship(author, ctiVitaeAuthor);

        Collection<Item> ctiVitaeRelatedProfiles =
            service.findCtiVitaeRelatedProfiles(context, patent);

        assertThat(ctiVitaeRelatedProfiles, is(singletonList(ctiVitaeAuthor)));

    }

    @Test
    public void publicationWithSomeAuthorsInCtiVitae() throws SQLException {

        Map<String, List<String>> metadataMap = Collections.singletonMap("Publication",
            Arrays.asList("dc.contributor.author", "dc.contributor.editor"));
        service.setEntityToMetadataMap(metadataMap);

        Item firstAuthor = item(randomUUID(), "Person");
        Item ctiVitaeFirstAuthor = item(randomUUID(), "CvPerson");

        Item secondAuthor = item(randomUUID(), "Person");

        Item thirdAuthor = item(randomUUID(), "Person");
        Item ctiVitaeThirdAuthor = item(randomUUID(), "CvPerson");

        Item publication = item(randomUUID(), "Publication",
            metadataValue("dc.contributor.author", "Rossi Mario", firstAuthor.getID().toString()),
            metadataValue("dc.contributor.author", "Smith John", secondAuthor.getID().toString()),
            metadataValue("dc.contributor.editor", "Doe Jane", thirdAuthor.getID().toString()),
            metadataValue("dc.contributor.editor", "Neri Daniele", null)
        );


        expectPersonOwnerRelationship(firstAuthor, ctiVitaeFirstAuthor);
        expectPersonOwnerRelationship(thirdAuthor, ctiVitaeThirdAuthor);
        expectNoPersonOwnerRelationship(secondAuthor);


        Collection<Item> ctiVitaeRelatedProfiles =
            service.findCtiVitaeRelatedProfiles(context, publication);

        assertThat(ctiVitaeRelatedProfiles, is(Arrays.asList(ctiVitaeFirstAuthor, ctiVitaeThirdAuthor)));

    }

    @Test
    public void projectWithMetadataNotSet() throws SQLException {

        service.setEntityToMetadataMap(singletonMap("Project", Arrays.asList("crispj.investigator",
            "crispj.coinvestigators")));

        Item author = item(randomUUID(), "Person");
        Item project = item(randomUUID(), "Project",
            metadataValue("other.metadata", "Rossi Mario", author.getID().toString()));

        when(relationshipTypeService
            .findByItemAndTypeNames(context, author, false, "isPersonOwner", "isOwnedByCvPerson"))
            .thenReturn(singletonList(profileOwnershipRelation));

        when(relationshipService.findByItemAndRelationshipType(context, author, profileOwnershipRelation, false))
            .thenReturn(emptyList());

        Collection<Item> ctiVitaeRelatedProfiles =
            service.findCtiVitaeRelatedProfiles(context, project);

        assertThat(ctiVitaeRelatedProfiles, is(emptyList()));

    }

    private Relationship relationship(RelationshipType relationshipType, Item leftItem, Item rightItem) {
        Relationship relationship = mock(Relationship.class);
        when(relationship.getRelationshipType()).thenReturn(relationshipType);
        when(relationship.getLeftItem()).thenReturn(leftItem);
        when(relationship.getRightItem()).thenReturn(rightItem);
        return relationship;
    }

    private void expectDirectorioToCtiVitaeStructure(Item directorioItem, Item cloneItem, Item ctiVitaeItem)
        throws SQLException {
        when(concytecWorkflowService.findMergedInItems(context, directorioItem)).thenReturn(emptyList());
        when(concytecWorkflowService.findCopiedItem(context, directorioItem)).thenReturn(cloneItem);
        when(concytecWorkflowService.findClonedItem(context, cloneItem)).thenReturn(ctiVitaeItem);
    }

    private Item item(UUID uuid, String entityType, MetadataValue... metadataValues) throws SQLException {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(uuid);

        when(itemService.getMetadata(item, "relationship.type")).thenReturn(entityType);

        Arrays.stream(metadataValues)
            .collect(Collectors.groupingBy(mv -> mv.getMetadataField().toString()))
            .forEach((key, value) -> when(itemService.getMetadataByMetadataString(item, key)).thenReturn(value));

        when(itemService.find(context, uuid)).thenReturn(item);
        return item;
    }

    private MetadataValue metadataValue(String field, String value, String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getAuthority()).thenReturn(authority);
        MetadataField metadataField = metadataField(field);
        when(metadataValue.getMetadataField()).thenReturn(metadataField);
        return metadataValue;
    }

    private MetadataField metadataField(String field) {
        MetadataField metadataField = mock(MetadataField.class);
        when(metadataField.toString()).thenReturn(field);
        return metadataField;
    }

    private void expectPersonOwnerRelationship(Item directorioItem, Item ctiVitaeItem) throws SQLException {
        Relationship relationship = relationship(profileOwnershipRelation, ctiVitaeItem, directorioItem);

        when(relationshipTypeService
            .findByItemAndTypeNames(context, directorioItem, false, "isPersonOwner", "isOwnedByCvPerson"))
            .thenReturn(singletonList(profileOwnershipRelation));

        when(
            relationshipService.findByItemAndRelationshipType(context, directorioItem, profileOwnershipRelation, false))
            .thenReturn(singletonList(relationship));
    }

    private void expectNoPersonOwnerRelationship(Item directorioItem) throws SQLException {
        when(relationshipTypeService
            .findByItemAndTypeNames(context, directorioItem, false, "isPersonOwner", "isOwnedByCvPerson"))
            .thenReturn(singletonList(profileOwnershipRelation));

        when(
            relationshipService.findByItemAndRelationshipType(context, directorioItem, profileOwnershipRelation, false))
            .thenReturn(emptyList());

    }
}