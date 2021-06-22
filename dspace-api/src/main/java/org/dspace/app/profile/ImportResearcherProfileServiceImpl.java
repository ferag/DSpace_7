/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.app.profile.service.ImportResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
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
    public Item importProfile(Context context, EPerson eperson, URI source, Collection collection)
        throws AuthorizeException, SQLException {

        requestService.getCurrentRequest().setAttribute("context", context);

        final List<URI> uriList = new ArrayList<URI>();
        if (source != null) {
            uriList.add(source);
        }

        List<ExternalDataObject> externalObjects =
                getConfiguredProfileProvider(eperson, uriList).stream()
                .map(configuredProvider -> configuredProvider.getExternalDataObject())
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        if (externalObjects.isEmpty()) {
            throw new IllegalArgumentException("No external profile metadata found for the eperson " + eperson.getID());
        }

        ExternalDataObject externalDataObject = mergeExternalObjects(externalObjects);

        return createItem(context, collection, externalDataObject);
    }

    public void setAfterImportActionList(List<AfterImportAction> afterImportActionList) {
        this.afterImportActionList = afterImportActionList;
    }

    /**
     * Return the list of configured researcher profile metadata provider for the eperson and the given uriList.
     * @param eperson
     * @param uriList
     * @return
     */
    public List<ConfiguredResearcherProfileProvider> getConfiguredProfileProvider(EPerson eperson, List<URI> uriList) {
        return importProfileProviders.stream()
                .map(provider -> provider.configureProvider(eperson, uriList))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * <p>Merge all the valid researcher profile metadata present in every externalObjects.</p>
     * <p>
     * The ordered list of external objects is relevant:
     * conflicting metadata are resolved by giving priority to the first match.
     * </p>
     * <p>
     * Other fields of the returned merged object are not relevant.
     * </p>
     * 
     * @param externalObjects the merged source external object
     * @return
     */
    private ExternalDataObject mergeExternalObjects(final List<ExternalDataObject> externalObjects) {

        log.debug("Merging " + externalObjects.size() + " external objects");

        if (externalObjects.size() == 1) {
            return externalObjects.get(0);
        }

        ExternalDataObject result = new ExternalDataObject();
        result.setId("merged--" + fromSources(externalObjects, ExternalDataObject::getId));
        result.setSource("merged--" + fromSources(externalObjects, ExternalDataObject::getSource));
        result.setDisplayValue("N/A");
        result.setValue("N/A");

        for (ExternalDataObject otherObject: externalObjects) {
            appendMetadataFromOtherObject(result, otherObject);
        }

        return result;

    }

    private ExternalDataObject appendMetadataFromOtherObject(
            ExternalDataObject object,
            ExternalDataObject otherObject) {

        Set<String> existentMetadataKeys = object
                .getMetadata().stream().map(m -> metadataKey(m)).collect(Collectors.toSet());

        for (MetadataValueDTO metadata : otherObject.getMetadata()) {
            if (!existentMetadataKeys.contains(metadataKey(metadata))) {
                object.addMetadata(metadata);
            }
        }

        return object;
    }

    private String metadataKey(MetadataValueDTO metadata) {
        return Stream.of(metadata.getSchema(), metadata.getQualifier(), metadata.getElement())
                .filter(s -> s != null)
                .collect(Collectors.joining("."));
    }

    private String fromSources(List<ExternalDataObject> externalObjects,
                               Function<ExternalDataObject, String> originalData) {
        return externalObjects.stream().map(originalData)
            .collect(joining("+"));
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
