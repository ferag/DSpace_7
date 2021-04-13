/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static org.apache.commons.lang.StringUtils.replace;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.versioning.model.MetadataCorrection;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;

/**
 * Implementation of {@link Consumer} to flag the CV entity's metadata that
 * should not be overwritten by the changes made in the related directorio's
 * item.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntityFlagConsumer implements Consumer {

    private ItemService itemService;

    private MetadataFieldService metadataFieldService;

    private ItemCorrectionService itemCorrectionService;

    private ConcytecWorkflowService concytecWorkflowService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        concytecWorkflowService = new DSpace().getSingletonService(ConcytecWorkflowService.class);
        itemCorrectionService = new DSpace().getSingletonService(ItemCorrectionService.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        Item item = (Item) event.getSubject(context);

        if (itemsAlreadyProcessed.contains(item)) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        if (isNotArchivedCvEntity(item)) {
            return;
        }

        try {
            context.turnOffAuthorisationSystem();
            flagMetadata(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void flagMetadata(Context context, Item cvItem) throws SQLException, AuthorizeException {

        Item directorioItem = findDirectorioItem(context, cvItem);
        if (directorioItem == null) {
            flagAllMetadata(context, cvItem);
            return;
        }

        ItemCorrection corrections = itemCorrectionService.getAppliedCorrections(context, directorioItem, cvItem);
        for (MetadataCorrection correction : corrections.getMetadataCorrections()) {
            flagMetadata(context, cvItem, correction.getMetadataField());
        }

    }

    private Item findDirectorioItem(Context context, Item cvItem) throws SQLException {
        Item cvClone = concytecWorkflowService.findClone(context, cvItem);
        if (cvClone == null) {
            return null;
        }

        return concytecWorkflowService.findShadowItemCopy(context, cvClone);
    }

    private void flagAllMetadata(Context context, Item cvItem) throws SQLException {

        List<String> metadataFieldsToFlag = cvItem.getMetadata().stream()
            .map(metadataValue -> metadataValue.getMetadataField().toString('.'))
            .collect(Collectors.toList());

        for (String metadataField : metadataFieldsToFlag) {
            flagMetadata(context, cvItem, metadataField);
        }

    }

    private void flagMetadata(Context context, Item cvItem, String metadataFieldAsString) throws SQLException {

        String cvFlagMetadataField = "perucris.flagcv." + replace(metadataFieldAsString, ".", "");
        if (CollectionUtils.isNotEmpty(itemService.getMetadataByMetadataString(cvItem, cvFlagMetadataField))) {
            return;
        }

        MetadataField metadataField = metadataFieldService.findByString(context, cvFlagMetadataField, '.');
        if (metadataField != null) {
            itemService.addMetadata(context, cvItem, metadataField, null, "false");
        }

    }

    private boolean isNotArchivedCvEntity(Item item) {
        return item == null || !item.isArchived() || isNotCvEntity(item);
    }

    private boolean isNotCvEntity(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        return !(entityType != null && entityType.startsWith("Cv") && !entityType.endsWith("Clone"));
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }

}
