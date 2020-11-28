/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile.service;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.dspace.services.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ImportResearcherProfileServiceImpl implements ImportResearcherProfileService {

    private static final Logger log = LoggerFactory.getLogger(ImportResearcherProfileServiceImpl.class);

    private final ExternalDataService externalDataService;

    private final InstallItemService installItemService;

    private final RequestService requestService;

    private List<AfterImportAction> afterImportActionList;


    public ImportResearcherProfileServiceImpl(ExternalDataService externalDataService,
                                              InstallItemService installItemService, RequestService requestService) {
        this.externalDataService = externalDataService;
        this.installItemService = installItemService;
        this.requestService = requestService;
    }

    @Override
    public Item importProfile(Context context, URI source, Collection collection)
            throws AuthorizeException, SQLException {

        Optional<ExternalDataObject> externalDataObject = objectLookup(source);


        if (externalDataObject.isEmpty()) {
            throw new ResourceNotFoundException("resource for uri " + source + " not found");
        }
        return createItem(context, collection, externalDataObject.get());
    }

    private Item createItem(Context context, Collection collection, ExternalDataObject externalDataObject)
            throws AuthorizeException, SQLException {
        try {
            requestService.getCurrentRequest().setAttribute("context", context);
            WorkspaceItem workspaceItem = externalDataService.createWorkspaceItemFromExternalDataObject(context,
                    externalDataObject,
                    collection);
            Item item = installItemService.installItem(context, workspaceItem);
            Optional.ofNullable(afterImportActionList)
                    .ifPresent(l -> l.forEach(action -> action.applyTo(item)));
            return item;
        } catch (AuthorizeException | SQLException e) {
            log.error("Error while importing item into collection {}", e.getMessage(), e);
            throw e;
        }
    }

    private Optional<ExternalDataObject> objectLookup(URI source) {
        ResearcherProfileSource researcherProfileSource = new ResearcherProfileSource(source);
        Optional<ExternalDataObject> externalDataObject = externalDataService
                .getExternalDataObject(researcherProfileSource.source(), researcherProfileSource.id());
        return externalDataObject;
    }

    public void setAfterImportActionList(List<AfterImportAction> afterImportActionList) {
        this.afterImportActionList = afterImportActionList;
    }

    private static class ResearcherProfileSource {

        private final String[] path;

        ResearcherProfileSource(URI source) {
            this.path = source.getPath().split("/");
        }

        String id() {
            return path[path.length - 1];
        }

        String source() {
            return path[path.length - 3];
        }
    }
}
