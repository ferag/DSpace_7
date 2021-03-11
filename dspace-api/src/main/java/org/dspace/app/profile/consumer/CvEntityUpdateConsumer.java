/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.versioning.model.MetadataCorrection;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;

/**
 * Implementation of {@link Consumer} to update all the not flagged metadata of
 * an CV entity when the related item into directorio is updated.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityUpdateConsumer implements Consumer {

    private ItemService itemService;

    private CollectionService collectionService;

    private ItemCorrectionService itemCorrectionService;

    private MetadataFieldService metadataFieldService;

    private ConcytecWorkflowService concytecWorkflowService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        concytecWorkflowService = new DSpace().getSingletonService(ConcytecWorkflowService.class);
        itemCorrectionService = new DSpace().getSingletonService(ItemCorrectionService.class);
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);

        if (itemsAlreadyProcessed.contains(item) || context.getCurrentUser() == null) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        if (isNotArchivedDirectorioItem(context, item)) {
            return;
        }

        try {

            context.turnOffAuthorisationSystem();

            Item cvEntityClone = findRelatedCvEntityClone(context, item);
            if (cvEntityClone != null) {
                updateRelatedCvEntity(context, item, cvEntityClone);
            }

        } finally {
            context.restoreAuthSystemState();
        }

    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    private boolean isNotArchivedDirectorioItem(Context context, Item item) throws SQLException {
        return !(item != null && item.isArchived()
            && collectionService.isDirectorioCollection(context, item.getOwningCollection()));
    }

    private Item findRelatedCvEntityClone(Context context, Item item) throws SQLException {
        Item copiedItem = concytecWorkflowService.findCopiedItem(context, item);
        if (copiedItem != null && isCvEntityClone(copiedItem)) {
            return copiedItem;
        }

        return concytecWorkflowService.findOriginatedFromItems(context, item).stream()
            .filter(this::isCvEntityClone)
            .findFirst().orElse(null);
    }

    private void updateRelatedCvEntity(Context context, Item item, Item cvEntityClone) throws Exception {

        Item cvEntity = concytecWorkflowService.findClonedItem(context, cvEntityClone);
        if (cvEntity == null) {
            throw new IllegalStateException("Cannot find cloned item related to the item " + cvEntityClone.getID());
        }

        ItemCorrection itemCorrection = itemCorrectionService.getAppliedCorrections(context, cvEntity, item);
        List<MetadataCorrection> correctionsToApply = itemCorrection.getMetadataCorrections().stream()
            .filter(metadataCorrection -> isSynchronizationEnabled(context, cvEntity, metadataCorrection))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(correctionsToApply)) {
            ItemCorrection itemCorrectionToApply = new ItemCorrection(correctionsToApply);
            itemCorrectionService.applyCorrectionsOnItem(context, cvEntityClone, itemCorrectionToApply);
            itemCorrectionService.applyCorrectionsOnItem(context, cvEntity, itemCorrectionToApply);
        }

    }

    private boolean isSynchronizationEnabled(Context context, Item item, MetadataCorrection metadataCorrection) {
        try {
            String cvFlagMetadataField = "perucris.flagcv." + replace(metadataCorrection.getMetadataField(), ".", "");
            MetadataField metadataField = metadataFieldService.findByString(context, cvFlagMetadataField, '.');
            return metadataField != null && isEmpty(itemService.getMetadataByMetadataString(item, cvFlagMetadataField));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isCvEntityClone(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return isNotBlank(entityType) && entityType.startsWith("Cv") && entityType.endsWith("Clone");
    }


}
