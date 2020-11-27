/*
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
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DSpaceImportService} {@link org.dspace.importer.external.service.components.QuerySource}
 * implementation
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceImportServiceTest {

    private DSpaceImportService dSpaceImportService;

    private final ItemService itemService = mock(ItemService.class);
    private final Context context = mock(Context.class);
    private final DSpaceInternalMetadataFieldMapping metadataFieldMapping =
            mock(DSpaceInternalMetadataFieldMapping.class);

    @Before
    public void setUp() throws Exception {
        RequestService requestService = mock(RequestService.class);
        Request request = mock(Request.class);
        when(requestService.getCurrentRequest())
                .thenReturn(request);
        when(request.getAttribute("context"))
                .thenReturn(context);
        dSpaceImportService = new DSpaceImportService(itemService, requestService, metadataFieldMapping);
    }

    @Test
    public void itemImported() throws Exception {

        UUID uuid = UUID.randomUUID();
        Item item = mock(Item.class);

        when(itemService.find(context, uuid))
                .thenReturn(item);

        dSpaceImportService.getRecord(uuid.toString());

        verify(metadataFieldMapping).resultToDCValueMapping(item);
    }

    @Test(expected = MetadataSourceException.class)
    public void exceptionWhileLookingUp() throws Exception {
        UUID uuid = UUID.randomUUID();

        doThrow(new SQLException("sql exception"))
                .when(itemService).find(context, uuid);

        dSpaceImportService.getRecord(uuid.toString());
    }

    @Test(expected = MetadataSourceException.class)
    public void exceptionWhileTransforming() throws Exception {
        UUID uuid = UUID.randomUUID();
        Item item = mock(Item.class);

        when(itemService.find(context, uuid))
                .thenReturn(item);

        doThrow(new RuntimeException("something went wrong"))
                .when(metadataFieldMapping).resultToDCValueMapping(item);

        dSpaceImportService.getRecord(uuid.toString());
    }
}