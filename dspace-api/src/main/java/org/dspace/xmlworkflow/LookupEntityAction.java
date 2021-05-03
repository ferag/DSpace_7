/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.xmlworkflow.service.PostShadowCopyCreationAction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link PostShadowCopyCreationAction} that lookup entities
 * starting from a shadow copy item's metadata.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class LookupEntityAction implements PostShadowCopyCreationAction {

    private final String shadowCopyMetadata;

    private final String metadataToSearch;

    private final String metadataToLink;

    private final String entityTypeToSearch;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SearchService searchService;

    public LookupEntityAction(String shadowCopyMetadata, String metadataToSearch, String metadataToLink,
        String entityTypeToSearch) {
        this.shadowCopyMetadata = shadowCopyMetadata;
        this.metadataToSearch = metadataToSearch;
        this.metadataToLink = metadataToLink;
        this.entityTypeToSearch = entityTypeToSearch;
    }

    @Override
    public void process(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {

        Item item = workspaceItem.getItem();

        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, shadowCopyMetadata);
        List<MetadataValue> metadataValuesToLink = itemService.getMetadataByMetadataString(item, metadataToLink);

        if (isEmpty(metadataValues) || isEmpty(metadataValuesToLink)) {
            return;
        }

        for (MetadataValue metadataValue : metadataValues) {
            lookupEntity(context, metadataValue, metadataValuesToLink);
        }
    }

    private void lookupEntity(Context ctx, MetadataValue metadataValue, List<MetadataValue> metadataValuesToLink) {
        try {

            findItemToLink(ctx, metadataValue.getValue())
                .ifPresent(itemToLink -> linkItem(itemToLink.getID(), metadataValue.getPlace(), metadataValuesToLink));

        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void linkItem(UUID itemToLinkId, int place, List<MetadataValue> metadataValuesToLink) {
        findMetadataValueByPlace(metadataValuesToLink, place).ifPresent(metadataValue -> {
            metadataValue.setAuthority(itemToLinkId.toString());
            metadataValue.setConfidence(600);
        });
    }

    @SuppressWarnings("rawtypes")
    private Optional<Item> findItemToLink(Context context, String metadataValue) throws SearchServiceException {

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries(metadataToSearch + ":" + metadataValue);
        discoverQuery.addFilterQueries("search.entitytype:" + entityTypeToSearch);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return Optional.empty();
        }

        return Optional.of((Item) indexableObjects.get(0).getIndexedObject());
    }

    private Optional<MetadataValue> findMetadataValueByPlace(List<MetadataValue> metadataValues, int place) {
        return metadataValues.stream()
            .filter(metadataValue -> metadataValue.getPlace() == place)
            .findFirst();
    }

    public String getEntityTypeToSearch() {
        return entityTypeToSearch;
    }

    public String getMetadataToSearch() {
        return metadataToSearch;
    }

    public String getMetadataToLink() {
        return metadataToLink;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

}
