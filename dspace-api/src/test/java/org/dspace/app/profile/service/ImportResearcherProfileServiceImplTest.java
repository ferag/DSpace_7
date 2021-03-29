/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.service;


import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.profile.ImportResearcherProfileServiceImpl;
import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ImportResearcherProfileServiceImplTest {

    private ImportResearcherProfileServiceImpl importResearcherProfileService;

    private ExternalDataService externalDataService = mock(ExternalDataService.class);
    private InstallItemService installItemService = mock(InstallItemService.class);

    private Context context = mock(Context.class);
    private Request currentRequest = mock(Request.class);
    private ResearcherProfileProvider importProfileProvider = mock(ResearcherProfileProvider.class);


    @Before
    public void setUp() throws Exception {
        RequestService requestService = mock(RequestService.class);
        when(requestService.getCurrentRequest()).thenReturn(currentRequest);
        importResearcherProfileService = new ImportResearcherProfileServiceImpl(externalDataService,
            installItemService, requestService);
        importResearcherProfileService.setImportProfileProviders(singletonList(
            importProfileProvider
        ));
    }

    @Test
    public void itemInstalled() throws AuthorizeException, SQLException {

        URI source = URI.create("http://localhost:8080/path_to_external/serviceId/entry/1234");
        Collection collection = mock(Collection.class);

        ExternalDataObject externalDataObject = createExternalDataObject("1234");

        ConfiguredResearcherProfileProvider configuredResearcherProfileProvider =
            mock(ConfiguredResearcherProfileProvider.class);
        when(configuredResearcherProfileProvider.getExternalDataObject()).thenReturn(Optional.of(externalDataObject));

        when(importProfileProvider.configureProvider(any(), any()))
            .thenReturn(Optional.of(configuredResearcherProfileProvider));

        WorkspaceItem workspaceItem = workspaceItem(1234);

        when(externalDataService.createWorkspaceItemFromExternalDataObject(context, externalDataObject, collection))
            .thenReturn(workspaceItem);

        importResearcherProfileService.importProfile(context, source,
            collection);

        verify(installItemService).installItem(context, workspaceItem);
        verify(currentRequest).setAttribute("context", context);

    }

    @Test
    public void dspaceItemInstalled() throws AuthorizeException, SQLException {

        URI source = URI.create("http://localhost:8080/server/api/core/items/4ede600c-12e4-4934-9d93-ac56cc63f150");
        Collection collection = mock(Collection.class);

        ExternalDataObject externalDataObject = createExternalDataObject("4ede600c-12e4-4934-9d93-ac56cc63f150");

        ConfiguredResearcherProfileProvider configuredResearcherProfileProvider =
            mock(ConfiguredResearcherProfileProvider.class);
        when(configuredResearcherProfileProvider.getExternalDataObject()).thenReturn(Optional.of(externalDataObject));

        when(importProfileProvider.configureProvider(any(), any()))
            .thenReturn(Optional.of(configuredResearcherProfileProvider));

        WorkspaceItem workspaceItem = workspaceItem(8888);

        when(externalDataService.createWorkspaceItemFromExternalDataObject(context, externalDataObject, collection))
            .thenReturn(workspaceItem);

        importResearcherProfileService.importProfile(context, source,
            collection);

        verify(installItemService).installItem(context, workspaceItem);
        verify(currentRequest).setAttribute("context", context);

    }

    @Test(expected = IllegalArgumentException.class)
    public void resourceNotFoundThrowsException() throws AuthorizeException, SQLException {

        URI source = URI.create("http://localhost:8080/path_to_external/serviceId/entry/5678");
        Collection collection = mock(Collection.class);

        when(externalDataService.getExternalDataObject("serviceId", "5678"))
            .thenReturn(Optional.empty());


        importResearcherProfileService.importProfile(context, source,
            collection);

        verifyNoInteractions(installItemService);

    }

    @Test(expected = AuthorizeException.class)
    public void exceptionWhileCreating() throws AuthorizeException, SQLException {

        URI source = URI.create("http://localhost:8080/path_to_external/serviceId/entry/9999");
        Collection collection = mock(Collection.class);

        ExternalDataObject externalDataObject = createExternalDataObject("9999");

        ConfiguredResearcherProfileProvider configuredResearcherProfileProvider =
            mock(ConfiguredResearcherProfileProvider.class);
        when(configuredResearcherProfileProvider.getExternalDataObject()).thenReturn(Optional.of(externalDataObject));

        when(importProfileProvider.configureProvider(any(), any()))
            .thenReturn(Optional.of(configuredResearcherProfileProvider));

        doThrow(new AuthorizeException("not authorized"))
            .when(externalDataService)
            .createWorkspaceItemFromExternalDataObject(context, externalDataObject, collection);

        importResearcherProfileService.importProfile(context, source,
            collection);

        verifyNoInteractions(installItemService);

    }

    @Test
    public void postActionCalled() throws AuthorizeException, SQLException {

        AfterImportAction afterImportAction = mock(AfterImportAction.class);
        importResearcherProfileService.setAfterImportActionList(singletonList(afterImportAction));

        URI source = URI.create("http://localhost:8080/path_to_external/serviceId/entry/1234");
        Collection collection = mock(Collection.class);

        ExternalDataObject externalDataObject = createExternalDataObject("1234");

        ConfiguredResearcherProfileProvider configuredResearcherProfileProvider =
            mock(ConfiguredResearcherProfileProvider.class);
        when(configuredResearcherProfileProvider.getExternalDataObject()).thenReturn(Optional.of(externalDataObject));

        when(importProfileProvider.configureProvider(any(), any()))
            .thenReturn(Optional.of(configuredResearcherProfileProvider));

        WorkspaceItem workspaceItem = workspaceItem(1234);

        when(externalDataService.createWorkspaceItemFromExternalDataObject(context, externalDataObject, collection))
            .thenReturn(workspaceItem);

        Item item = item();
        when(installItemService.installItem(context, workspaceItem))
            .thenReturn(item);

        importResearcherProfileService.importProfile(context, source,
            collection);

        verify(afterImportAction).applyTo(context, item, externalDataObject);
    }

    @Test(expected = SQLException.class)
    public void exceptionDuringAfterImport() throws Exception {

        AfterImportAction afterImportAction = mock(AfterImportAction.class);

        importResearcherProfileService.setAfterImportActionList(singletonList(afterImportAction));

        URI source = URI.create("http://localhost:8080/path_to_external/serviceId/entry/1234");
        Collection collection = mock(Collection.class);

        ExternalDataObject externalDataObject = createExternalDataObject("1234");

        ConfiguredResearcherProfileProvider configuredResearcherProfileProvider =
            mock(ConfiguredResearcherProfileProvider.class);
        when(configuredResearcherProfileProvider.getExternalDataObject()).thenReturn(Optional.of(externalDataObject));

        when(importProfileProvider.configureProvider(any(), any()))
            .thenReturn(Optional.of(configuredResearcherProfileProvider));

        WorkspaceItem workspaceItem = workspaceItem(1234);

        when(externalDataService.createWorkspaceItemFromExternalDataObject(context, externalDataObject, collection))
            .thenReturn(workspaceItem);

        Item item = item();
        when(installItemService.installItem(context, workspaceItem))
            .thenReturn(item);

        doThrow(new SQLException("SqlException")).when(afterImportAction).applyTo(context, item, externalDataObject);

        importResearcherProfileService.importProfile(context, source,
            collection);
    }

    private ExternalDataObject createExternalDataObject(String s) {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setId(s);
        return externalDataObject;
    }

    private WorkspaceItem workspaceItem(int i) {
        WorkspaceItem workspaceItem = mock(WorkspaceItem.class);
        when(workspaceItem.getID()).thenReturn(i);
        return workspaceItem;
    }

    private Item item() {
        Item item = mock(Item.class);
        when(item.getID()).thenReturn(UUID.randomUUID());
        return item;
    }
}