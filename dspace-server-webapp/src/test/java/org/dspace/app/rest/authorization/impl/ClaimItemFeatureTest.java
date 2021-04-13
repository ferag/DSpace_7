/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ClaimItemFeatureTest {

    private ClaimItemFeature claimItemFeature;
    private final Context context = mock(Context.class);
    private final ItemService itemService = mock(ItemService.class);
    private final ResearcherProfileService researcherProfileService = mock(ResearcherProfileService.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final ConcytecWorkflowService concytecWorkflowService = mock(ConcytecWorkflowService.class);


    @Before
    public void setUp() throws Exception {
        claimItemFeature = new ClaimItemFeature(itemService, researcherProfileService, configurationService,
            relationshipService, concytecWorkflowService);
    }

    @Test
    public void unsupportedObjectType() throws Exception {

        CommunityRest object = new CommunityRest();
        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void epersonHasAlreadyAClonedProfile() throws Exception {

        UUID itemCollectionId = UUID.randomUUID();
        UUID ePersonId = UUID.randomUUID();

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] { "Person" });
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] { itemCollectionId.toString(), UUID.randomUUID().toString() });
        when(configurationService.getArrayProperty("claimable.relation.rightwardType"))
            .thenReturn(new String[] { "isOwnedBy", "testType" });

        EPerson eperson = buildEPerson(ePersonId);

        ResearcherProfile existingProfile = mock(ResearcherProfile.class);
        Item profileItem = mock(Item.class);
        when(existingProfile.getItem()).thenReturn(profileItem);
        when(concytecWorkflowService.findClone(context, profileItem)).thenReturn(mock(Item.class));

        when(researcherProfileService.findById(context, ePersonId)).thenReturn(existingProfile);

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id, UUID.randomUUID(), itemCollectionId);

        ItemRest object = buildItemRest(id);

        when(context.getCurrentUser()).thenReturn(eperson);

        when(itemService.find(context, UUID.fromString(id))).thenReturn(item);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));

        final Item relatedItem = itemWithOwner(null);
        Relationship itemExistingRelationship = itemCurrentRelationship(item, "isOwnedBy", relatedItem);

        when(relationshipService.findByItem(context, item))
            .thenReturn(Arrays.asList(itemExistingRelationship));

        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void epersonHasNotClonedProfile() throws SQLException, AuthorizeException {
        UUID itemCollectionId = UUID.randomUUID();
        UUID ePersonId = UUID.randomUUID();

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] { "Person" });
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] { itemCollectionId.toString(), UUID.randomUUID().toString() });
        when(configurationService.getArrayProperty("claimable.relation.rightwardType"))
            .thenReturn(new String[] { "isOwnedBy", "testType" });

        EPerson eperson = buildEPerson(ePersonId);

        ResearcherProfile existingProfile = mock(ResearcherProfile.class);
        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(existingProfile);

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id, UUID.randomUUID(), itemCollectionId);

        ItemRest object = buildItemRest(id);

        when(context.getCurrentUser()).thenReturn(eperson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));

        final Item relatedItem = itemWithOwner(null);
        Relationship itemExistingRelationship = itemCurrentRelationship(item, "isOwnedBy", relatedItem);

        when(relationshipService.findByItem(context, item))
            .thenReturn(Arrays.asList(itemExistingRelationship));

        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertTrue(authorized);
    }

    @Test
    public void nullClaimableCollectionList() throws SQLException, AuthorizeException {

        String id = UUID.randomUUID().toString();
        UUID ePersonId = UUID.randomUUID();
        ItemRest object = buildItemRest(id);

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(null);


        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void emptyClaimableCollectionList() throws SQLException, AuthorizeException {

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] {"Person"});
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] {});

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id);
        UUID ePersonId = UUID.randomUUID();
        ItemRest object = buildItemRest(id);

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);

        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(null);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));


        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void itemNotInClaimableCollection() throws SQLException, AuthorizeException {

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] {"Person"});
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()});

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id, UUID.randomUUID(), UUID.randomUUID());
        UUID ePersonId = UUID.randomUUID();
        ItemRest object = buildItemRest(id);

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);

        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(null);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));


        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void unclaimableEntityType() throws SQLException {

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] {"Person"});

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id);

        UUID ePersonId = UUID.randomUUID();

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);
        MetadataValue publication = metadataValue("Publication");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(publication));

        ItemRest object = buildItemRest(id);
        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void itemAlreadyInValidRelationship() throws SQLException, AuthorizeException {
        UUID itemCollectionId = UUID.randomUUID();

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] {"Person"});
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] {itemCollectionId.toString(), UUID.randomUUID().toString()});
        when(configurationService.getArrayProperty("claimable.relation.rightwardType"))
            .thenReturn(new String[] {"isOwnedBy", "testType"});


        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id, UUID.randomUUID(), itemCollectionId);

        UUID ePersonId = UUID.randomUUID();
        ItemRest object = buildItemRest(id);

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);

        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(null);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));

        Item relatedItem = itemWithOwner("owner");
        Relationship itemExistingRelationship = itemCurrentRelationship(item, "isOwnedBy", relatedItem);

        when(relationshipService.findByItem(context, item))
            .thenReturn(Arrays.asList(itemExistingRelationship));

        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertFalse(authorized);
    }

    @Test
    public void itemClaimable() throws SQLException, AuthorizeException {
        UUID itemCollectionId = UUID.randomUUID();

        when(configurationService.getArrayProperty("claimable.entityType"))
            .thenReturn(new String[] {"Person"});
        when(configurationService.getArrayProperty("claimable.collection.uuid"))
            .thenReturn(new String[] {itemCollectionId.toString(), UUID.randomUUID().toString()});
        when(configurationService.getArrayProperty("claimable.relation.rightwardType"))
            .thenReturn(new String[] {"isOwnedBy", "testType"});

        String id = UUID.randomUUID().toString();
        Item item = buildDspaceItem(id, UUID.randomUUID(), itemCollectionId);

        UUID ePersonId = UUID.randomUUID();
        ItemRest object = buildItemRest(id);

        EPerson ePerson = buildEPerson(ePersonId);

        when(context.getCurrentUser()).thenReturn(ePerson);

        when(itemService.find(context, UUID.fromString(id)))
            .thenReturn(item);

        when(researcherProfileService.findById(context, ePersonId))
            .thenReturn(null);

        MetadataValue personMetadata = metadataValue("Person");
        when(itemService.getMetadataByMetadataString(item, "dspace.entity.type"))
            .thenReturn(Collections.singletonList(personMetadata));

        final Item relatedItem = itemWithOwner(null);
        Relationship itemExistingRelationship = itemCurrentRelationship(item, "isOwnedBy", relatedItem);

        when(relationshipService.findByItem(context, item))
            .thenReturn(Arrays.asList(itemExistingRelationship));

        boolean authorized = claimItemFeature.isAuthorized(context, object);

        assertTrue(authorized);
    }

    private Item itemWithOwner(final String owner) {
        Item item = mock(Item.class);
        when(itemService.getMetadata(item, "cris.owner"))
            .thenReturn(owner);
        return item;
    }

    private Relationship itemCurrentRelationship(final Item item, final String rightWardType,
                                                 final Item relatedItem) {
        RelationshipType relationshipType = mock(RelationshipType.class);
        when(relationshipType.getRightwardType()).thenReturn(rightWardType);

        Relationship itemExistingRelationship = mock(Relationship.class);
        when(itemExistingRelationship.getRightItem()).thenReturn(item);
        when(itemExistingRelationship.getLeftItem()).thenReturn(relatedItem);
        when(itemExistingRelationship.getRelationshipType()).thenReturn(relationshipType);
        return itemExistingRelationship;
    }

    private EPerson buildEPerson(UUID ePersonId) {
        EPerson eperson = mock(EPerson.class);
        when(eperson.getID()).thenReturn(ePersonId);
        return eperson;
    }

    private MetadataValue metadataValue(String value) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getValue()).thenReturn(value);
        return metadataValue;
    }

    private Item buildDspaceItem(String id, UUID... collections) {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(UUID.fromString(id));
        List<Collection> collectionList = Arrays.stream(collections).map(this::toCollection)
                                                .collect(Collectors.toList());
        when(item.getCollections()).thenReturn(collectionList);
        return item;
    }

    private Collection toCollection(UUID s) {
        Collection collection = mock(Collection.class);
        when(collection.getID()).thenReturn(s);
        return collection;
    }

    private ItemRest buildItemRest(String id) {
        ItemRest object = new ItemRest();
        object.setUuid(id);
        return object;
    }
}
