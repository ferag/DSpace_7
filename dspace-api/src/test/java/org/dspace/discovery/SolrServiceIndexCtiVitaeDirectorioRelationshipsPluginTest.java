/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.perucris.ctivitae.CvRelatedEntitiesService;
import org.dspace.util.UUIDUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SolrServiceIndexCtiVitaeDirectorioRelationshipsPluginTest {

    private Context context = mock(Context.class);

    private SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin plugin;
    private CvRelatedEntitiesService cvRelatedEntitiesService = mock(CvRelatedEntitiesService.class);
    private ItemService itemService = mock(ItemService.class);
    private CollectionService collectionService = mock(CollectionService.class);

    @Before
    public void setUp() throws Exception {
        plugin = new SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin(
            cvRelatedEntitiesService, itemService, collectionService);
    }

    @Test
    public void notADirectorioItem() throws SQLException {

        IndexableItem indexableItem = new IndexableItem(item("CvPerson"));
        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.entityWithCvReferences("CvPerson"))
            .thenReturn(false);
        plugin.additionalIndex(context, indexableItem, document);

        assertNull(document.getField("cti.owner"));
    }

    @Test
    public void notInDirectorioCollection() throws SQLException {

        Item item = item("Publication",
            UUID.randomUUID());
        IndexableItem indexableItem = new IndexableItem(item);
        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(true);

        when(collectionService.isDirectorioCollection(context, item.getOwningCollection()))
            .thenReturn(false);

        plugin.additionalIndex(context, indexableItem, document);

        assertNull(document.getField("cti.owner"));
    }

    @Test
    public void directorioItemUpdated() throws SQLException, AuthorizeException {

        UUID directorioCommunityId = UUID.randomUUID();
        Item item = item("Publication", directorioCommunityId);
        IndexableItem indexableItem = new IndexableItem(item);
        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(true);

        when(collectionService.isDirectorioCollection(context, item.getOwningCollection()))
            .thenReturn(true);

        UUID publicationOwnerId = UUID.randomUUID();
        String publicationOwnerName = "owner name";
        Item publicationOwner = mock(Item.class);
        when(publicationOwner.getID()).thenReturn(publicationOwnerId);
        when(publicationOwner.getName()).thenReturn(publicationOwnerName);

        when(cvRelatedEntitiesService.findCtiVitaeRelationsForDirectorioItem(context, item))
            .thenReturn(singletonList(publicationOwner));

        plugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("perucris.ctivitae.owner_authority").getValue(),
            is(singletonList(UUIDUtils.toString(publicationOwnerId))));
        assertThat(document.getField("perucris.ctivitae.owner").getValue(),
            is(singletonList(publicationOwnerName)));
    }

    @Test
    public void directorioItemUpdatedWithTwoAuthors() throws SQLException, AuthorizeException {

        UUID directorioCommunityId = UUID.randomUUID();
        Item item = item("Publication", directorioCommunityId);
        IndexableItem indexableItem = new IndexableItem(item);
        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(true);

        when(collectionService.isDirectorioCollection(context, item.getOwningCollection()))
            .thenReturn(true);

        UUID publicationOwnerId = UUID.randomUUID();
        String publicationOwnerName = "owner name";
        Item publicationOwner = publicationOwner(publicationOwnerId, publicationOwnerName);

        UUID secondOwnerId = UUID.randomUUID();
        String secondOwnerName = "second owner name";
        Item secondOwner = publicationOwner(secondOwnerId, secondOwnerName);

        when(cvRelatedEntitiesService.findCtiVitaeRelationsForDirectorioItem(context, item))
            .thenReturn(asList(publicationOwner, secondOwner));

        plugin.additionalIndex(context, indexableItem, document);

        List<String> ownerAuthorities = (List<String>) document
            .getField("perucris.ctivitae.owner_authority").getValue();
        List<String> owners = (List<String>) document.getField("perucris.ctivitae.owner").getValue();
        assertThat(ownerAuthorities,
            containsInAnyOrder(UUIDUtils.toString(publicationOwnerId), UUIDUtils.toString(secondOwnerId)));
        assertThat(owners,
            containsInAnyOrder(publicationOwnerName, secondOwnerName));
    }

    @Test
    public void exceptionDuringOwnerLookup() throws SQLException, AuthorizeException {

        UUID directorioCommunityId = UUID.randomUUID();
        Item item = item("Publication", directorioCommunityId);
        IndexableItem indexableItem = new IndexableItem(item);
        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(true);

        when(collectionService.isDirectorioCollection(context, item.getOwningCollection()))
            .thenReturn(true);

        doThrow(new SQLException("sql exception"))
            .when(cvRelatedEntitiesService)
            .findCtiVitaeRelationsForDirectorioItem(context, item);

        plugin.additionalIndex(context, indexableItem, document);

        assertNull(document.getField("cti.owner"));
    }

    private Item item(String relationshipType, UUID... owningCollectionsCommunities) throws SQLException {
        Item item = mock(Item.class);
        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn(relationshipType);
        Collection collection = collection(owningCollectionsCommunities);
        when(item.getOwningCollection())
            .thenReturn(collection);
        return item;
    }

    private Collection collection(UUID... owningCommunities) throws SQLException {
        Collection collection = mock(Collection.class);
        List<Community> communities = Arrays.stream(owningCommunities)
            .map(this::community).collect(Collectors.toList());
        when(collection.getCommunities())
            .thenReturn(communities);
        return collection;
    }

    private Community community(UUID uuid) {
        Community community = mock(Community.class);
        when(community.getID()).thenReturn(uuid);
        return community;
    }

    private Item publicationOwner(UUID id, String name) {
        Item owner = mock(Item.class);
        when(owner.getID()).thenReturn(id);
        when(owner.getName()).thenReturn(name);
        return owner;
    }
}