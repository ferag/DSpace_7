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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.junit.Before;
import org.junit.Test;

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
    private DSpaceItemRelationshipService dSpaceItemRelationshipService = mock(DSpaceItemRelationshipService.class);

    @Before
    public void setUp() throws Exception {
        dspaceAfterImportAction = new DspaceAfterImportAction(itemService,
            dSpaceItemRelationshipService);
    }


    @Test
    public void relationshipCreated() throws SQLException, AuthorizeException {

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();
        Item item = item(itemId);

        Item relatedItem = item(externalObjectId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId.toString());

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verify(dSpaceItemRelationshipService).create(context, item, relatedItem);

    }

    @Test(expected = SQLException.class)
    public void sqlExceptionWhileCreatingRelation() throws SQLException, AuthorizeException {

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        Item item = item(itemId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId.toString());

        Item relatedItem = item(externalObjectId);

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        doThrow(new SQLException("exception"))
            .when(dSpaceItemRelationshipService).create(context, item, relatedItem);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);
    }

    @Test(expected = AuthorizeException.class)
    public void authorizeExceptionWhileCreatingRelation() throws SQLException, AuthorizeException {

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();

        Item item = item(itemId);
        ExternalDataObject externalDataObject = externalDataObject(externalObjectId.toString());

        Item relatedItem = item(externalObjectId);

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        doThrow(new AuthorizeException("Authorize Exception"))
            .when(dSpaceItemRelationshipService).create(context, item, relatedItem);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

    }

    @Test
    public void notADspaceExternalObject() throws SQLException, AuthorizeException {
        UUID itemId = UUID.randomUUID();
        String externalObjectId = "0000-1111-2222-3333";
        Item item = item(itemId);

        ExternalDataObject externalDataObject = externalDataObject(externalObjectId);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verify(itemService, never()).find(eq(context), any());
        verifyNoInteractions(dSpaceItemRelationshipService);
    }

    @Test
    public void notFoundDspaceExternalObject() throws SQLException, AuthorizeException {
        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();
        Item item = item(itemId);

        ExternalDataObject externalDataObject = externalDataObject(externalObjectId.toString());

        when(itemService.find(context, externalObjectId))
            .thenReturn(null);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verifyNoInteractions(dSpaceItemRelationshipService);
    }

    @Test
    public void mergeWithDSpaceSourceRelationshipCreated() throws SQLException, AuthorizeException {

        UUID itemId = UUID.randomUUID();
        UUID externalObjectId = UUID.randomUUID();
        Item item = item(itemId);

        Item relatedItem = item(externalObjectId);
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setId("merged::123123+" + externalObjectId.toString());
        externalDataObject.setSource("merged::reniec+DSPACE");

        when(itemService.find(context, externalObjectId))
            .thenReturn(relatedItem);

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verify(dSpaceItemRelationshipService).create(context, item, relatedItem);

    }

    @Test
    public void mergeWithoutDSpaceSourceRelationshipNotCreated() throws SQLException, AuthorizeException {

        UUID itemId = UUID.randomUUID();
        Item item = item(itemId);

        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setId("merged::123123+0000-1111-2222-3333");
        externalDataObject.setSource("merged::reniec+orcid");

        dspaceAfterImportAction.applyTo(context, item, externalDataObject);

        verifyNoInteractions(dSpaceItemRelationshipService);
        verify(itemService, never()).find(eq(context), any());

    }

    private ExternalDataObject externalDataObject(String externalDataObjectId) {
        ExternalDataObject externalDataObject = mock(ExternalDataObject.class);
        when(externalDataObject.getId()).thenReturn(externalDataObjectId);
        return externalDataObject;
    }

    private Item item(UUID itemId) {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(itemId);
        return item;
    }

}