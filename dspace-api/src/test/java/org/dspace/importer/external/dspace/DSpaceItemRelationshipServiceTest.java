/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DSpaceItemRelationshipService}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceItemRelationshipServiceTest {

    private DSpaceItemRelationshipService dSpaceItemRelationshipService;

    private RelationshipTypeService relationshipTypeService = mock(RelationshipTypeService.class);
    private RelationshipService relationshipService = mock(RelationshipService.class);
    private EntityTypeService entityTypeService = mock(EntityTypeService.class);
    private ItemService itemService = mock(ItemService.class);

    private Context context = mock(Context.class);

    @Before
    public void setUp() throws Exception {
        dSpaceItemRelationshipService = new DSpaceItemRelationshipService(relationshipTypeService, relationshipService,
            entityTypeService, itemService);
    }

    @Test(expected = DSpaceItemRelationshipService.RelationshipCoordinatesException.class)
    public void missingRelationshipCoordinates() throws Exception {
        dSpaceItemRelationshipService.setLeftEntityType("left");

        dSpaceItemRelationshipService.afterPropertiesSet();
    }

    @Test
    public void relationshipCreated() throws SQLException, AuthorizeException {

        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(relatedItemId);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.create(context, item, relatedItem, relationshipType,
            -1, -1)).thenReturn(relationship);

        dSpaceItemRelationshipService.create(context, item, relatedItem);

        verify(relationshipService).updateItem(context, item);
        verify(relationshipService).updateItem(context, relatedItem);
    }

    @Test
    public void relationshipDeleted() throws SQLException, AuthorizeException {

        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(relatedItemId);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.findByItemAndRelationshipType(context, item, relationshipType))
            .thenReturn(Collections.singletonList(relationship));

        List<MetadataValue> relationMetadataList = Collections.singletonList(metadataValue("relationValue"));

        when(itemService.getMetadata(item, "relation", rightwardType, null, Item.ANY))
            .thenReturn(relationMetadataList);

        dSpaceItemRelationshipService.delete(context, item);

        verify(relationshipService, atLeastOnce()).delete(context, relationship, true);
            verify(itemService, atLeastOnce()).removeMetadataValues(eq(context), eq(item), any());
    }

    private MetadataValue metadataValue(String value) {
        MetadataValue mv = mock(MetadataValue.class);
        when(mv.getValue()).thenReturn(value);
        return mv;
    }

    @Test
    public void relationshipTypeNotFound() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(null);


        dSpaceItemRelationshipService.create(context, item(itemId), item(relatedItemId));

        verifyNoInteractions(relationshipService);

    }

    @Test
    public void relationshipTypeNotFoundWhileDeleting() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(null);

        dSpaceItemRelationshipService.delete(context, item(itemId));

        verifyNoInteractions(relationshipService);

    }

    @Test
    public void relationshipToBeDeletedNotFound() throws SQLException, AuthorizeException {

        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(relatedItemId);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.findByItemAndRelationshipType(context, item, relationshipType))
            .thenReturn(Collections.emptyList());

        dSpaceItemRelationshipService.delete(context, item);

        verify(relationshipService, never()).delete(context, relationship, true);
    }

    @Test(expected = SQLException.class)
    public void sqlExceptionWhileFindingRelationship() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        doThrow(new SQLException("SQL Exception"))
            .when(relationshipTypeService).findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType);

        dSpaceItemRelationshipService.create(context, item(itemId), item(relatedItemId));

    }

    @Test(expected = AuthorizeException.class)
    public void authorizeExceptionWhileCreatingRelation() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(relatedItemId);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.create(context, item, relatedItem, relationshipType,
            -1, -1)).thenReturn(relationship);

        doThrow(new AuthorizeException("Authorize Exception"))
            .when(relationshipService).updateItem(context, item);

        dSpaceItemRelationshipService.create(context, item, relatedItem);

    }

    @Test(expected = AuthorizeException.class)
    public void authorizeExceptionWhileDeletingRelation() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID relatedItemId = UUID.randomUUID();

        dSpaceItemRelationshipService.setLeftEntityType(leftEntity.getLabel());
        dSpaceItemRelationshipService.setRightEntityType(rightEntity.getLabel());
        dSpaceItemRelationshipService.setLeftwardType(leftwardType);
        dSpaceItemRelationshipService.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(relatedItemId);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.findByItemAndRelationshipType(context, item, relationshipType))
            .thenReturn(Collections.singletonList(relationship));

        doThrow(new AuthorizeException("Authorize Exception"))
            .when(relationshipService).delete(context, relationship, true);

        dSpaceItemRelationshipService.delete(context, item);

    }

    private void expectEntityType(EntityType leftEntity) throws SQLException {
        when(entityTypeService.findByEntityType(context, leftEntity.getLabel()))
            .thenReturn(leftEntity);
    }

    private RelationshipType relationshipType(Integer id) {
        RelationshipType relationshipType = mock(RelationshipType.class);

        when(relationshipType.getID()).thenReturn(id);

        return relationshipType;
    }

    private Relationship relationship(Item item, Item relatedItem) {
        Relationship relationship = mock(Relationship.class);
        when(relationship.getLeftItem()).thenReturn(item);
        when(relationship.getRightItem()).thenReturn(relatedItem);
        return relationship;
    }

    private Item item(UUID itemId) {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(itemId);
        return item;
    }

    private EntityType entityType(String label) {
        EntityType entityType = mock(EntityType.class);
        when(entityType.getLabel()).thenReturn(label);
        return entityType;
    }
}