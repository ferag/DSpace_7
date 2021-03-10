/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.tools.ant.util.StringUtils.removePrefix;
import static org.dspace.content.Item.ANY;
import static org.dspace.util.UUIDUtils.fromString;
import static org.springframework.util.StringUtils.uncapitalize;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.ItemCorrectionProvider;
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

/**
 * Implementation of {@link Consumer} to request synchronize the Cv entities
 * with the directorio.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CvEntitySynchronizationConsumer implements Consumer {

    private ItemService itemService;

    private ItemCorrectionService itemCorrectionService;

    private ConcytecWorkflowService concytecWorkflowService;

    private XmlWorkflowService workflowService;

    private XmlWorkflowItemService workflowItemService;

    private WorkspaceItemService workspaceItemService;

    private ItemCorrectionProvider itemCorrectionProvider;

    private CollectionService collectionService;

    private ConfigurationService configurationService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private Map<String, List<String>> profileSynchronizationMap;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        DSpace dSpace = new DSpace();
        ServiceManager serviceManager = dSpace.getServiceManager();

        itemService = ContentServiceFactory.getInstance().getItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        concytecWorkflowService = dSpace.getSingletonService(ConcytecWorkflowService.class);
        itemCorrectionProvider = dSpace.getSingletonService(ItemCorrectionProvider.class);
        itemCorrectionService = dSpace.getSingletonService(ItemCorrectionService.class);
        workflowService = (XmlWorkflowService) WorkflowServiceFactory.getInstance().getWorkflowService();
        workflowItemService = (XmlWorkflowItemService) XmlWorkflowServiceFactory.getInstance().getWorkflowItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

        profileSynchronizationMap = ofNullable(serviceManager.getServiceByName("profileSynchronizationMap", Map.class))
            .orElseGet(HashMap::new);

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
            String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);

            if (isCvPerson(entityType)) {
                consumeCvPerson(context, item, entityType);
            } else {
                consumeCvEntity(context, item, entityType);
            }

        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void consumeCvEntity(Context context, Item item, String entityType) throws Exception {

        if (isSynchronizationDisabled(item, entityType)) {
            return;
        }

        Item clone = concytecWorkflowService.findClone(context, item);
        if (clone == null) {

            WorkspaceItem workspaceItemClone = createItemClone(context, item, entityType);
            workflowService.start(context, workspaceItemClone);

        } else if (!clone.isArchived()) {

            rejectItemRequest(context, clone);
            WorkspaceItem workspaceItemClone = createItemClone(context, item, entityType);
            workflowService.start(context, workspaceItemClone);

        } else {

            rejectPreviousCorrections(context, clone);

            ItemCorrection corrections = itemCorrectionService.getAppliedCorrections(context, clone, item);
            if (corrections.isNotEmpty()) {
                sendCorrectionRequest(context, item, clone, corrections);
            }
        }

    }

    private void consumeCvPerson(Context context, Item item, String entityType) throws Exception {
        Item cvPersonClone = concytecWorkflowService.findClone(context, item);

        if (cvPersonClone == null && noCvPersonSectionSynchronizationIsEnabled(item)) {
            return;
        }

        if (cvPersonClone == null) {

            WorkspaceItem workspaceItemClone = createCvPersonClone(context, item, entityType);
            workflowService.start(context, workspaceItemClone);

        } else if (!cvPersonClone.isArchived()) {

            ItemCorrection correctionToSynchronize = getProfileCorrectionToSynchronize(context, cvPersonClone, item);
            if (correctionToSynchronize.isNotEmpty()) {
                rejectItemRequest(context, cvPersonClone);
                WorkspaceItem workspaceItemClone = createCvPersonClone(context, item, entityType);
                workflowService.start(context, workspaceItemClone);
            }

        } else {

            rejectPreviousCorrections(context, cvPersonClone);

            ItemCorrection correctionToSynchronize = getProfileCorrectionToSynchronize(context, cvPersonClone, item);
            if (correctionToSynchronize.isNotEmpty()) {
                sendCorrectionRequest(context, item, cvPersonClone, correctionToSynchronize);
            }

        }
    }

    private WorkspaceItem createItemClone(Context ctx, Item item, String entityType) throws Exception {
        Collection collection = findCvCloneCollection(ctx, entityType)
            .orElseThrow(() -> new IllegalStateException("No collection found for clones of entity " + entityType));

        WorkspaceItem workspaceItem = itemCorrectionProvider.createNewItemAndAddItInWorkspace(ctx, collection, item);
        concytecWorkflowService.createCloneRelationship(ctx, workspaceItem.getItem(), item);

        return workspaceItem;
    }

    private WorkspaceItem createCvPersonClone(Context ctx, Item cvPerson, String entityType) throws Exception {
        Collection collection = findCvCloneCollection(ctx, entityType)
            .orElseThrow(() -> new IllegalStateException("No collection found for clones of entity " + entityType));

        WorkspaceItem cloneWorkspaceItem = workspaceItemService.create(ctx, collection, false);
        Item cvPersonClone = cloneWorkspaceItem.getItem();

        concytecWorkflowService.createCloneRelationship(ctx, cvPersonClone, cvPerson);
        ItemCorrection correctionToSynchronize = getProfileCorrectionToSynchronize(ctx, cvPersonClone, cvPerson);
        itemCorrectionService.applyCorrectionsOnItem(ctx, cvPersonClone, correctionToSynchronize);

        return cloneWorkspaceItem;
    }

    private void sendCorrectionRequest(Context context, Item item, Item clone, ItemCorrection correctionToSynchronize)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        WorkspaceItem correctionWorkspaceItem = itemCorrectionService.createCorrectionItem(context, clone.getID());
        Item correctionItem = correctionWorkspaceItem.getItem();
        itemCorrectionService.applyCorrectionsOnItem(context, correctionItem, correctionToSynchronize);
        workflowService.start(context, correctionWorkspaceItem);
    }

    private void rejectPreviousCorrections(Context context, Item clone) throws Exception {
        List<Item> cloneCorrections = itemCorrectionService.getCorrectionItems(context, clone);
        for (Item cloneCorrection : cloneCorrections) {
            rejectItemRequest(context, cloneCorrection);
        }
    }

    private void rejectItemRequest(Context context, Item item) throws Exception {
        EPerson currentUser = context.getCurrentUser();

        XmlWorkflowItem shadowWorkflowItemCopy = findWorkflowShadowItemCopy(context, item);
        if (shadowWorkflowItemCopy != null) {
            workflowService.deleteWorkflowByWorkflowItem(context, shadowWorkflowItemCopy, currentUser);
        }

        XmlWorkflowItem workflowItem = workflowItemService.findByItem(context, item);
        if (workflowItem != null) {
            workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, currentUser);
        }
    }

    private XmlWorkflowItem findWorkflowShadowItemCopy(Context context, Item item) throws SQLException {
        Item shadowItemCopy = concytecWorkflowService.findShadowItemCopy(context, item);
        return shadowItemCopy != null ? workflowItemService.findByItem(context, shadowItemCopy) : null;
    }

    private ItemCorrection getProfileCorrectionToSynchronize(Context context, Item cvPersonClone, Item item) {
        ItemCorrection corrections = itemCorrectionService.getAppliedCorrections(context, cvPersonClone, item);
        List<String> metadataFieldToSynchronize = calculateProfileMetadataToSynchronize(item);
        List<MetadataCorrection> correctionToSynchronize = corrections.getMetadataCorrections().stream()
            .filter(metadataCorrection -> metadataFieldToSynchronize.contains(metadataCorrection.getMetadataField()))
            .collect(Collectors.toList());
        return new ItemCorrection(correctionToSynchronize);
    }

    private List<String> calculateProfileMetadataToSynchronize(Item item) {
        return profileSynchronizationMap.entrySet().stream()
            .filter(entry -> isSynchronizationEnabled(item, entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
    }

    private Optional<Collection> findCvCloneCollection(Context context, String entityType) throws SQLException {
        String property = "cti-vitae.clone." + removePrefix(entityType, "Cv").toLowerCase() + "-collection-id";
        return ofNullable(collectionService.find(context, fromString(configurationService.getProperty(property))));
    }

    private boolean isSynchronizationEnabled(Item item, String synchronizationMetadata) {
        return toBoolean(itemService.getMetadataFirstValue(item, new MetadataFieldName(synchronizationMetadata), ANY));
    }

    private boolean noCvPersonSectionSynchronizationIsEnabled(Item item) {
        return profileSynchronizationMap.entrySet().stream()
            .noneMatch(entry -> isSynchronizationEnabled(item, entry.getKey()));
    }

    private boolean isSynchronizationDisabled(Item item, String entityType) {
        return !isSynchronizationEnabled(item, "perucris." + uncapitalize(entityType) + ".syncEnabled");
    }

    private boolean isNotArchivedCvEntity(Item item) {
        return item == null || !item.isArchived() || isNotCvEntity(item);
    }

    private boolean isNotCvEntity(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return !(entityType != null && entityType.startsWith("Cv") && !entityType.endsWith("Clone"));
    }

    private boolean isCvPerson(String entityType) {
        return "CvPerson".equals(entityType);
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }

}
