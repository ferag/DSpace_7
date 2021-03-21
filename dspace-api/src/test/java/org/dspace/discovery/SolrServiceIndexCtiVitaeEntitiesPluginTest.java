/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.perucris.ctivitae.CvRelatedEntitiesService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * Unit tests for {@link SolrServiceIndexCtiVitaeEntitiesPlugin}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class SolrServiceIndexCtiVitaeEntitiesPluginTest {

    private final ItemService itemService = mock(ItemService.class);
    private final ResearcherProfileService researcherProfileService = mock(ResearcherProfileService.class);

    private final Context context = mock(Context.class);
    private final CvRelatedEntitiesService cvRelatedEntitiesService = mock(CvRelatedEntitiesService.class);
    private final IndexingService indexingService = mock(IndexingService.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private SolrServiceIndexCtiVitaeEntitiesPlugin indexPlugin;

    @Before
    public void setUp() throws Exception {
        indexPlugin = new SolrServiceIndexCtiVitaeEntitiesPlugin(itemService,
            researcherProfileService,
            cvRelatedEntitiesService,
            indexingService,
            configurationService);
    }

    @Test
    public void nullItemInIndexableItem() {

        SolrInputDocument document = new SolrInputDocument();
        IndexableItem indexableItem = indexableItem(null);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    @Test
    public void notAnIndexableItem() {

        IndexableCollection indexableCollection = mock(IndexableCollection.class);
        SolrInputDocument document = new SolrInputDocument();
        indexPlugin.additionalIndex(context, indexableCollection, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    @Test
    public void documentNotUpdatedForCvProjectClone() {

        SolrInputDocument document = new SolrInputDocument();
        Item item = ownedItem("CvProjectClone", UUID.randomUUID().toString());
        IndexableItem indexableItem = indexableItem(item);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    @Test
    public void documentNotUpdatedForCvPatentClone() {

        SolrInputDocument document = new SolrInputDocument();
        Item item = ownedItem("CvPatentClone", UUID.randomUUID().toString());
        IndexableItem indexableItem = indexableItem(item);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    @Test
    public void documentNotUpdatedForCvPublicationClone() {

        SolrInputDocument document = new SolrInputDocument();
        Item item = ownedItem("CvPublicationClone", UUID.randomUUID().toString());
        IndexableItem indexableItem = indexableItem(item);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    @Test
    public void documentUpdatedForCvPublication() throws SQLException, AuthorizeException {

        SolrInputDocument document = new SolrInputDocument();
        UUID ownerAuthority = UUID.randomUUID();
        UUID ownedProfileId = UUID.randomUUID();

        expectRelatedResearcherProfile(ownerAuthority, ownedProfileId);

        Item item = ownedItem("CvPublication", ownerAuthority.toString());
        IndexableItem indexableItem = indexableItem(item);

        when(cvRelatedEntitiesService.entityWithCvReferences("CvPublication"))
            .thenReturn(true);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getField("ctivitae.owner").getValue(), is(UUIDUtils.toString(ownedProfileId)));
        verifyNoInteractions(indexingService);
    }

    @Test
    public void documentUpdatedForCvProject() throws SQLException, AuthorizeException {

        SolrInputDocument document = new SolrInputDocument();
        UUID profileId = UUID.randomUUID();
        UUID cvPersonId = UUID.randomUUID();


        Item item = ownedItem("CvProject", profileId.toString());
        IndexableItem indexableItem = indexableItem(item);

        when(cvRelatedEntitiesService.entityWithCvReferences("CvProject"))
            .thenReturn(true);

        expectRelatedResearcherProfile(profileId, cvPersonId);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getField("ctivitae.owner").getValue(), is(UUIDUtils.toString(cvPersonId)));
        verifyNoInteractions(indexingService);
    }

    @Test
    public void documentUpdatedForCvPatent() throws SQLException, AuthorizeException {

        SolrInputDocument document = new SolrInputDocument();
        UUID ownerAuthority = UUID.randomUUID();
        UUID ownedProfileId = UUID.randomUUID();

        expectRelatedResearcherProfile(ownerAuthority, ownedProfileId);

        Item item = ownedItem("CvPatent", ownerAuthority.toString());
        IndexableItem indexableItem = indexableItem(item);

        when(cvRelatedEntitiesService.entityWithCvReferences("CvPatent"))
            .thenReturn(true);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getField("ctivitae.owner").getValue(), is(UUIDUtils.toString(ownedProfileId)));
        verifyNoInteractions(indexingService);
    }

    /**
     * this test checks the scenario when a Cv item has a counterpart into the Directorio, and such counterpart
     * is related to other CvPersons: one does own its own copy of Directorio entity and one does not.
     *
     * The test proves that Directorio entity index is updated adding as ctivitae owner id, only the id of the CTI Vitae
     * person that does not appear as owner of a Ctivitae entity that is a copy of the Directorio entity.
     *
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Test
    public void documentWithThreeAuthorsTwoWithCvEntityAndOneNot() throws SQLException, AuthorizeException {

        UUID firstOwnerProfileId = UUID.randomUUID();
        UUID firstOwnerCvPersonId = UUID.randomUUID();
        UUID secondOwnerCvPersonId = UUID.randomUUID();
        Item directorioItem = item("Publication");

        Item firstRelatedCvItem = ownedItem("CvPublication", firstOwnerProfileId.toString());

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(true);

        SolrInputDocument document = new SolrInputDocument();

        when(cvRelatedEntitiesService.findDirectorioRelated(context, firstRelatedCvItem))
            .thenReturn(Optional.of(directorioItem));

        when(cvRelatedEntitiesService.findCtiVitaeRelationsForDirectorioItem(context, directorioItem))
            .thenReturn(asList(secondOwnerCvPersonId.toString()));

        indexPlugin.additionalIndex(context, indexableItem(firstRelatedCvItem), document);

        assertThat(document.getField("ctivitae.owner").getValue(), is(firstOwnerCvPersonId.toString()));
        verify(indexingService).updateCtiVitaeReferences(context, directorioItem.getID(),
            Collections.singletonList(secondOwnerCvPersonId.toString()));

    }

    @Test
    public void directorioItemNotIndexed() throws SQLException, AuthorizeException {

        UUID firstOwnerProfileId = UUID.randomUUID();
        UUID firstOwnerCvPersonId = UUID.randomUUID();
        UUID secondOwnerProfileId = UUID.randomUUID();
        UUID secondOwnerCvPersonId = UUID.randomUUID();
        UUID thirdOwnerProfileId = UUID.randomUUID();
        UUID thirdOwnerCvPersonId = UUID.randomUUID();

        UUID directorioCommunityId = UUID.randomUUID();
        Item directorioItem = item("Publication", directorioCommunityId);

        when(cvRelatedEntitiesService.entityWithCvReferences("Publication"))
            .thenReturn(false);

        when(configurationService.getProperty("directorios.community-id"))
            .thenReturn(UUIDUtils.toString(directorioCommunityId));

        Item firstRelatedCvItem = ownedItem("CvPublication", firstOwnerProfileId.toString());
        Item secondRelatedCvItem = ownedItem("CvPublication", secondOwnerProfileId.toString());

        SolrInputDocument document = new SolrInputDocument();

        Item firstOwnerCvPerson = expectRelatedResearcherProfile(firstOwnerProfileId, firstOwnerCvPersonId).getItem();
        Item secondOwnerCvPerson =
            expectRelatedResearcherProfile(secondOwnerProfileId, secondOwnerCvPersonId).getItem();
        Item thirdOwnerCvPerson = expectRelatedResearcherProfile(thirdOwnerProfileId, thirdOwnerCvPersonId).getItem();

        when(cvRelatedEntitiesService.findDirectorioRelated(context, firstRelatedCvItem))
            .thenReturn(Optional.of(directorioItem));

        when(cvRelatedEntitiesService.findCTIVitaeRelated(context, directorioItem))
            .thenReturn(asList(firstRelatedCvItem, secondRelatedCvItem));

        when(cvRelatedEntitiesService.findCtiVitaeRelatedProfiles(context, directorioItem))
            .thenReturn(asList(firstOwnerCvPerson, secondOwnerCvPerson, thirdOwnerCvPerson));


        indexPlugin.additionalIndex(context, indexableItem(directorioItem), document);

        assertNull(document.getField("ctivitae.owner"));
    }

    private ResearcherProfile expectRelatedResearcherProfile(UUID profileId, UUID cvProfileId)
        throws SQLException, AuthorizeException {
        ResearcherProfile researcherProfile = researcherProfile(cvProfileId);
        when(researcherProfileService.findById(context, profileId)).thenReturn(researcherProfile);
        return researcherProfile;
    }

    @Test
    public void documentWithoutOwnerDocumentNotUpdated() throws SQLException, AuthorizeException {

        SolrInputDocument document = new SolrInputDocument();

        Item item = item("CvPatent");
        IndexableItem indexableItem = indexableItem(item);

        indexPlugin.additionalIndex(context, indexableItem, document);
        assertNull(document.getField("ctivitae.owner"));
    }

    private ResearcherProfile researcherProfile(UUID ownedProfileId) throws SQLException {
        ResearcherProfile researcherProfile = mock(ResearcherProfile.class);
        Item item = item("CvProfile");
        when(item.getID()).thenReturn(ownedProfileId);
        when(researcherProfile.getItem()).thenReturn(item);
        return researcherProfile;
    }


    private Item item(String entityType, UUID...owningCollectionCommunities) throws SQLException {
        Item item = mock(Item.class);
        when(itemService.getMetadata(item, "search.entitytype")).thenReturn(entityType);
        Collection collection = mock(Collection.class);
        List<Community> communities = Arrays.stream(owningCollectionCommunities)
            .map(id -> {
                Community c = mock(Community.class);
                when(c.getID()).thenReturn(id);
                return c;
            }).collect(Collectors.toList());
        when(collection.getCommunities()).thenReturn(communities);
        when(item.getOwningCollection()).thenReturn(collection);
        return item;
    }


    private Item ownedItem(String entityType, String ownerAuthority) {
        Item item = mock(Item.class);
        List<MetadataValue> ownerMetadata = metadataValue("owner", ownerAuthority);
        when(itemService.getMetadata(item, "relationship.type")).thenReturn(entityType);
        when(itemService.getMetadataByMetadataString(item, "cris.owner")).thenReturn(ownerMetadata);
        Collection collection = mock(Collection.class);
        when(collection.getID()).thenReturn(UUID.randomUUID());
        when(item.getOwningCollection()).thenReturn(collection);
        return item;
    }

    private List<MetadataValue> metadataValue(String value, String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn(value);
        when(metadataValue.getAuthority()).thenReturn(authority);
        return Collections.singletonList(metadataValue);
    }

    private IndexableItem indexableItem(Item item) {
        IndexableItem indexableItem = mock(IndexableItem.class);
        when(indexableItem.getIndexedObject()).thenReturn(item);
        return indexableItem;
    }

}