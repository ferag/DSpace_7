/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.dspace.content.Item.ANY;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.versioning.model.MetadataCorrection;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Consumer} to request an Profile correction to
 * CONCYTEC.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ProfileEditConsumer implements Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEditConsumer.class);

    private ItemService itemService;

    private ItemCorrectionService itemCorrectionService;

    private ConcytecWorkflowService concytecWorkflowService;

    private XmlWorkflowService workflowService;

    private XmlWorkflowItemService workflowItemService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private Map<String, List<String>> profileSynchronizationMap;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        DSpace dSpace = new DSpace();
        itemService = ContentServiceFactory.getInstance().getItemService();
        concytecWorkflowService = dSpace.getSingletonService(ConcytecWorkflowService.class);
        itemCorrectionService = dSpace.getSingletonService(ItemCorrectionService.class);
        workflowService = (XmlWorkflowService) WorkflowServiceFactory.getInstance().getWorkflowService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        profileSynchronizationMap = dSpace.getServiceManager().getServiceByName("profileSynchronizationMap", Map.class);
        profileSynchronizationMap = profileSynchronizationMap != null ? profileSynchronizationMap : new HashMap<>();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        Item item = (Item) event.getSubject(context);
        if (isNotArchivedCvPerson(item) || itemsAlreadyProcessed.contains(item)) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        Item cvPersonClone = concytecWorkflowService.findClone(context, item);
        if (cvPersonClone == null) {
            LOGGER.warn("The Profile with id {} does not have the clone", item.getID());
            return;
        }

        ItemCorrection correctionToSynchronize = calculateCorrectionToSynchronize(context, cvPersonClone, item);
        if (correctionToSynchronize.isEmpty()) {
            return;
        }

        try {
            context.turnOffAuthorisationSystem();
            sendCorrectionRequest(context, item, cvPersonClone, correctionToSynchronize);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void sendCorrectionRequest(Context context, Item item, Item clone, ItemCorrection correctionToSynchronize)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        List<Item> cloneCorrections = itemCorrectionService.getCorrectionItems(context, clone);
        for (Item cloneCorrection : cloneCorrections) {
            deleleProfileCorrections(context, cloneCorrection);
        }

        WorkspaceItem correctionWorkspaceItem = itemCorrectionService.createCorrectionItem(context, clone.getID());
        Item correctionItem = correctionWorkspaceItem.getItem();
        itemCorrectionService.applyCorrectionsOnItem(context, correctionItem, correctionToSynchronize);
        workflowService.start(context, correctionWorkspaceItem);
    }

    private void deleleProfileCorrections(Context context, Item cloneCorrection)
        throws SQLException, AuthorizeException, IOException {

        EPerson currentUser = context.getCurrentUser();

        XmlWorkflowItem cloneCorrectionShadowWorkflowItemCopy = findWorkflowShadowItemCopy(context, cloneCorrection);
        if (cloneCorrectionShadowWorkflowItemCopy != null) {
            workflowService.deleteWorkflowByWorkflowItem(context, cloneCorrectionShadowWorkflowItemCopy, currentUser);
        }

        XmlWorkflowItem cloneCorrectionWorkflowItem = workflowItemService.findByItem(context, cloneCorrection);
        if (cloneCorrectionWorkflowItem != null) {
            workflowService.deleteWorkflowByWorkflowItem(context, cloneCorrectionWorkflowItem, currentUser);
        }

    }

    private XmlWorkflowItem findWorkflowShadowItemCopy(Context context, Item item) throws SQLException {
        Item shadowItemCopy = concytecWorkflowService.findShadowItemCopy(context, item);
        return shadowItemCopy != null ? workflowItemService.findByItem(context, shadowItemCopy) : null;
    }

    private ItemCorrection calculateCorrectionToSynchronize(Context context, Item cvPersonClone, Item item) {
        ItemCorrection corrections = itemCorrectionService.getAppliedCorrections(context, cvPersonClone, item);
        List<String> metadataFieldToSynchronize = calculateMetadataToSynchronize(item);
        List<MetadataCorrection> correctionToSynchronize = corrections.getMetadataCorrections().stream()
            .filter(metadataCorrection -> metadataFieldToSynchronize.contains(metadataCorrection.getMetadataField()))
            .collect(Collectors.toList());
        return new ItemCorrection(correctionToSynchronize);
    }

    private List<String> calculateMetadataToSynchronize(Item item) {
        return profileSynchronizationMap.entrySet().stream()
            .filter(entry -> isSynchronizationEnabled(item, entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
    }

    private boolean isSynchronizationEnabled(Item item, String synchronizationMetadata) {
        return toBoolean(itemService.getMetadataFirstValue(item, new MetadataFieldName(synchronizationMetadata), ANY));
    }

    private boolean isNotArchivedCvPerson(Item item) {
        return item == null || !item.isArchived() || isNotCvPerson(item);
    }

    private boolean isNotCvPerson(Item item) {
        return !"CvPerson".equals(itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY));
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }

}
