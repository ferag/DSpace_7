/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.app.profile.service.ImportResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
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

    private List<ResearcherProfileProvider> importProfileProviders = new ArrayList<ResearcherProfileProvider>();

    public ImportResearcherProfileServiceImpl(ExternalDataService externalDataService,
                                              InstallItemService installItemService,
                                              RequestService requestService) {
        this.externalDataService = externalDataService;
        this.installItemService = installItemService;
        this.requestService = requestService;
    }

    @Override
    public Item importProfile(Context context, URI source, Collection collection)
        throws AuthorizeException, SQLException {

        requestService.getCurrentRequest().setAttribute("context", context);

        final List<URI> uriList = new ArrayList<URI>();
        uriList.add(source);

        List<ExternalDataObject> externalObjects = importProfileProviders.stream()
                .map(provider -> provider.configureProvider(context.getCurrentUser(), uriList))
                .filter(Optional::isPresent).map(Optional::get)
                .map(configuredProvider -> configuredProvider.getExternalDataObject())
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        if (externalObjects.isEmpty()) {
            throw new ResourceNotFoundException("resource for uri " + source + " not found");
        }

        ExternalDataObject externalDataObject = mergeExternalObjects(externalObjects);

        return createItem(context, collection, externalDataObject);
    }

    public void setAfterImportActionList(List<AfterImportAction> afterImportActionList) {
        this.afterImportActionList = afterImportActionList;
    }

    /**
     * <p>Merge all the valid researcher profile metadata present in every externalObjects.</p>
     * <p>
     * The ordered list of external objects is relevant:
     * conflicting metadata are resolved by giving priority to the first match.
     * </p>
     * <p>
     * Other fields of the returned merged object are taken from the first external objects of the list.
     * </p>
     * 
     * @param externalObjects the merged source external object
     * @return
     */
    private ExternalDataObject mergeExternalObjects(List<ExternalDataObject> externalObjects) {

        Set<MetadataValueDTO> metadataSet = new HashSet<MetadataValueDTO>();
        externalObjects.stream().forEach(object -> {
            object.getMetadata().stream().forEach(metadataValue -> {
                if (!metadataSet.contains(metadataValue)) {
                    metadataSet.add(metadataValue);
                }
            });
        });

        ExternalDataObject result = new ExternalDataObject();
        result.setId(externalObjects.get(0).getId());
        result.setSource(externalObjects.get(0).getSource());
        result.setDisplayValue(externalObjects.get(0).getDisplayValue());
        result.setValue(externalObjects.get(0).getValue());
        result.setMetadata(metadataSet.stream().collect(Collectors.toList()));
        return result;

    }

    private Item createItem(Context context, Collection collection, ExternalDataObject externalDataObject)
        throws AuthorizeException, SQLException {
        try {
            WorkspaceItem workspaceItem = externalDataService.createWorkspaceItemFromExternalDataObject(context,
                externalDataObject,
                collection);
            Item item = installItemService.installItem(context, workspaceItem);
            applyAfterImportActions(context, item, externalDataObject);
            return item;
        } catch (AuthorizeException | SQLException e) {
            log.error("Error while importing item into collection {}", e.getMessage(), e);
            throw e;
        }
    }

    private void applyAfterImportActions(Context context, Item item, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {
        if (Objects.nonNull(afterImportActionList)) {
            for (AfterImportAction action : afterImportActionList) {
                action.applyTo(context, item, externalDataObject);
            }
        }
    }

    public void setImportProfileProviders(List<ResearcherProfileProvider> importProfileProviders) {
        this.importProfileProviders = importProfileProviders;
    }
}
