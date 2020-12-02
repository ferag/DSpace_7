/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;


import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.dspace.DspaceAfterImportAction.RelationshipCoordinatesException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DspaceAfterImportActionTest {

    private DspaceAfterImportAction dspaceAfterImportAction;
    private RelationshipTypeService relationshipTypeService = mock(RelationshipTypeService.class);
    private RelationshipService relationshipService = mock(RelationshipService.class);
    private ItemService itemService = mock(ItemService.class);
    private EntityTypeService entityTypeService = mock(EntityTypeService.class);

    private Context context = mock(Context.class);

    @Before
    public void setUp() throws Exception {
        dspaceAfterImportAction = new DspaceAfterImportAction(relationshipTypeService, relationshipService, itemService,
            entityTypeService);
    }

    @Test(expected = RelationshipCoordinatesException.class)
    public void missingRelationshipCoordinates() throws Exception {
        dspaceAfterImportAction.setLeftEntityType("left");

        dspaceAfterImportAction.afterPropertiesSet();
    }

    @Test
    public void relationshipCreated() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        dspaceAfterImportAction.setLeftEntityType(leftEntity.getLabel());
        dspaceAfterImportAction.setRightEntityType(rightEntity.getLabel());
        dspaceAfterImportAction.setLeftwardType(leftwardType);
        dspaceAfterImportAction.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(externalObjectId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId);

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.create(context, item, relatedItem, relationshipType,
            -1, -1)).thenReturn(relationship);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verify(relationshipService).updateItem(context, item);
        verify(relationshipService).updateItem(context, relatedItem);

    }

    @Test
    public void relationshipTypeNotFound() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        dspaceAfterImportAction.setLeftEntityType(leftEntity.getLabel());
        dspaceAfterImportAction.setRightEntityType(rightEntity.getLabel());
        dspaceAfterImportAction.setLeftwardType(leftwardType);
        dspaceAfterImportAction.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(null);


        Item item = item(itemId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId);


        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verifyNoInteractions(relationshipService);

    }

    @Test(expected = SQLException.class)
    public void sqlExceptionWhileFindingRelation() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        dspaceAfterImportAction.setLeftEntityType(leftEntity.getLabel());
        dspaceAfterImportAction.setRightEntityType(rightEntity.getLabel());
        dspaceAfterImportAction.setLeftwardType(leftwardType);
        dspaceAfterImportAction.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        doThrow(new SQLException("SQL Exception"))
            .when(relationshipTypeService).findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType);


        Item item = item(itemId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId);


        dspaceAfterImportAction.applyTo(context, item, externalDataObject);
    }

    @Test(expected = AuthorizeException.class)
    public void authorizeExceptionWhileUpdatingItem() throws SQLException, AuthorizeException {
        EntityType leftEntity = entityType("leftEntity");
        EntityType rightEntity = entityType("rightEntity");
        String leftwardType = "leftwardType";
        String rightwardType = "rightwardType";
        Integer relationshipTypeId = 123;

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        dspaceAfterImportAction.setLeftEntityType(leftEntity.getLabel());
        dspaceAfterImportAction.setRightEntityType(rightEntity.getLabel());
        dspaceAfterImportAction.setLeftwardType(leftwardType);
        dspaceAfterImportAction.setRightwardType(rightwardType);

        expectEntityType(leftEntity);
        expectEntityType(rightEntity);

        RelationshipType relationshipType = relationshipType(relationshipTypeId);
        when(relationshipTypeService.findbyTypesAndTypeName(context, leftEntity, rightEntity,
            leftwardType, rightwardType)).thenReturn(relationshipType);


        Item item = item(itemId);
        Item relatedItem = item(externalObjectId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId);

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        Relationship relationship = relationship(item, relatedItem);

        when(relationshipService.create(context, item, relatedItem, relationshipType,
            -1, -1)).thenReturn(relationship);

        doThrow(new AuthorizeException("Authorize Exception"))
            .when(relationshipService).updateItem(context, item);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);


    }

    private OngoingStubbing<EntityType> expectEntityType(EntityType leftEntity) throws SQLException {
        return when(entityTypeService.findByEntityType(context, leftEntity.getLabel()))
            .thenReturn(leftEntity);
    }

    private Relationship relationship(Item item, Item relatedItem) {
        Relationship relationship = mock(Relationship.class);
        when(relationship.getLeftItem()).thenReturn(item);
        when(relationship.getRightItem()).thenReturn(relatedItem);
        return relationship;
    }

    private ExternalDataObject externalDataObject(UUID uuid) {
        ExternalDataObject externalDataObject = mock(ExternalDataObject.class);
        when(externalDataObject.getId()).thenReturn(uuid.toString());
        return externalDataObject;
    }

    private Item item(UUID itemId) {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(itemId);
        return item;
    }

    private RelationshipType relationshipType(Integer id) {
        RelationshipType relationshipType = mock(RelationshipType.class);

        when(relationshipType.getID()).thenReturn(id);

        return relationshipType;
    }

    private EntityType entityType(String label) {
        EntityType entityType = mock(EntityType.class);
        when(entityType.getLabel()).thenReturn(label);
        return entityType;
    }
}