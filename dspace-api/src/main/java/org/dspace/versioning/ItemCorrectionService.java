/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.model.CorrectionType;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.versioning.model.MetadataCorrection;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to handle submission of item correction.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemCorrectionService {

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected RelationshipTypeService relationshipTypeService;

    @Autowired
    protected ItemCorrectionProvider correctionItemProvider;

    @Autowired
    protected EntityTypeService entityTypeService;

    @Autowired
    protected MetadataFieldService metadataFieldService;

    private final String correctionRelationshipName;

    private final Set<String> ignoredMetadataFields;

    public ItemCorrectionService(String correctionRelationshipName, Set<String> ignoredMetadataFields) {
        this.correctionRelationshipName = correctionRelationshipName;
        this.ignoredMetadataFields = ignoredMetadataFields;
    }

    /**
     * Create a workspaceitem by an existing Item
     * 
     * @param context  the dspace context
     * @param request  the request containing the details about the workspace to
     *                 create
     * @param itemUUID the item UUID to use for creating the workspaceitem
     * @return the created workspaceitem
     * @throws SQLException       if a SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    public WorkspaceItem createWorkspaceItemByItem(Context context, UUID itemUUID)
        throws SQLException, AuthorizeException {

        WorkspaceItem wsi = null;

        Item item = itemService.find(context, itemUUID);

        if (item != null) {
            wsi = correctionItemProvider.createNewItemAndAddItInWorkspace(context, item.getOwningCollection(), item);
        } else {
            throw new IllegalArgumentException("Item " + itemUUID + " is not found");
        }

        return wsi;
    }

    /**
     * Create a workspaceitem by an existing Item
     * 
     * @param context  the dspace context
     * @param request  the request containing the details about the workspace to
     *                 create
     * @param itemUUID the item UUID to use for creating the workspaceitem
     * @return the created workspaceitem
     * @throws SQLException       if a SQL error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    public WorkspaceItem createWorkspaceItemAndRelationshipByItem(Context context, UUID itemUUID, String relationship)
        throws SQLException, AuthorizeException {

        if (StringUtils.isBlank(relationship)) {
            throw new IllegalArgumentException("Relationship cannot be undefined");
        }

        Item item = itemService.find(context, itemUUID);
        if (item == null) {
            throw new IllegalArgumentException("Cannot create a relationship without a given item");
        }

        RelationshipType relationshipType = findRelationshipType(context, item, relationship);
        if (relationshipType == null) {
            throw new IllegalArgumentException("No relationship type found for " + relationship);
        }

        WorkspaceItem workspaceItem = createWorkspaceItemByItem(context, itemUUID);
        relationshipService.create(context, workspaceItem.getItem(), item, relationshipType, false);

        return workspaceItem;
    }

    public WorkspaceItem createCorrectionItem(Context context, UUID itemUUID) throws SQLException, AuthorizeException {
        return createWorkspaceItemAndRelationshipByItem(context, itemUUID, getCorrectionRelationshipName());
    }

    public boolean checkIfIsCorrectionItem(Context context, Item item) throws SQLException {
        return checkIfIsCorrectionItem(context, item, getCorrectionRelationshipName());
    }

    public boolean checkIfIsCorrectionItem(Context context, Item item, String relationshipName) throws SQLException {
        RelationshipType relationshipType = findRelationshipType(context, item, relationshipName);
        if (relationshipType == null) {
            return false;
        }
        return isNotEmpty(relationshipService.findByItemAndRelationshipType(context, item, relationshipType, true));
    }

    public void replaceCorrectionItemWithNative(Context context, XmlWorkflowItem wfi) {

        try {
            Relationship relationship = getCorrectionItemRelation(context, wfi.getItem(), true);
            Item nativeItem = relationship.getRightItem();
            relationshipService.delete(context, relationship);
            correctionItemProvider.updateNativeItemWithCorrection(context, wfi, wfi.getItem(), nativeItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public List<Item> getCorrectionItems(Context context, Item item) throws SQLException {
        return getCorrectionItemRelations(context, item, false).stream()
            .map(relationship -> relationship.getLeftItem())
            .collect(Collectors.toList());
    }

    public Item getCorrectedItem(Context context, Item correctionItem) throws SQLException {
        Relationship relationship = getCorrectionItemRelation(context, correctionItem, true);
        return relationship != null ? relationship.getRightItem() : null;
    }

    public ItemCorrection getAppliedCorrections(Context context, Item originalItem, Item correctionItem) {
        return new ItemCorrection(checkMetadataCorrections(context, originalItem, correctionItem));
    }

    public void applyCorrectionsOnItem(Context context, Item item, ItemCorrection correction)
        throws SQLException, AuthorizeException {
        applyMetadataCorrectionsOnItem(context, item, correction.getMetadataCorrections());
        itemService.update(context, item);
    }

    private List<Relationship> getCorrectionItemRelations(Context ctx, Item item, boolean isLeft) throws SQLException {
        RelationshipType type = findRelationshipType(ctx, item, getCorrectionRelationshipName());
        if (type == null) {
            return Collections.emptyList();
        }
        return relationshipService.findByItemAndRelationshipType(ctx, item, type, isLeft);
    }

    private Relationship getCorrectionItemRelation(Context context, Item item, boolean isLeft) throws SQLException {
        List<Relationship> relations = getCorrectionItemRelations(context, item, isLeft);
        return isNotEmpty(relations) ? relations.get(0) : null;
    }

    private RelationshipType findRelationshipType(Context context, Item item, String relationship) throws SQLException {

        EntityType type = entityTypeService.findByItem(context, item);
        if (type == null) {
            return null;
        }

        return relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationship).stream()
            .filter(relationshipType -> type.equals(relationshipType.getLeftType()))
            .findFirst().orElse(null);
    }

    private List<MetadataCorrection> checkMetadataCorrections(Context context, Item originalItem, Item correctionItem) {

        List<MetadataCorrection> metadataCorrections = new ArrayList<>();

        Map<String, List<MetadataValue>> originalItemMetadata = getItemMetadata(originalItem);
        Map<String, List<MetadataValue>> correctionItemMetadata = getItemMetadata(correctionItem);

        for (String metadataField : originalItemMetadata.keySet()) {

            if (ignoredMetadataFields.contains(metadataField)) {
                continue;
            }

            if (!correctionItemMetadata.containsKey(metadataField)) {
                metadataCorrections.add(MetadataCorrection.metadataRemoval(metadataField));
                continue;
            }

            List<MetadataValue> newMetadataValues = correctionItemMetadata.get(metadataField);
            List<String> newValues = getValues(newMetadataValues);
            List<String> oldValues = getValues(originalItemMetadata.get(metadataField));

            if (!oldValues.equals(newValues)) {
                List<MetadataValueDTO> metadataValueDtos = getMetadataValueDtos(newMetadataValues);
                metadataCorrections.add(MetadataCorrection.metadataModification(metadataField, metadataValueDtos));
            }

        }

        for (String metadataField : correctionItemMetadata.keySet()) {

            if (ignoredMetadataFields.contains(metadataField)) {
                continue;
            }

            if (!originalItemMetadata.containsKey(metadataField)) {
                List<MetadataValue> metadataValues = correctionItemMetadata.get(metadataField);
                List<MetadataValueDTO> metadataValueDtos = getMetadataValueDtos(metadataValues);
                metadataCorrections.add(MetadataCorrection.metadataAddition(metadataField, metadataValueDtos));
            }
        }

        return metadataCorrections;
    }

    private Map<String, List<MetadataValue>> getItemMetadata(Item item) {
        return item.getMetadata().stream().collect(groupingBy(value -> value.getMetadataField().toString('.')));
    }

    private List<String> getValues(List<MetadataValue> metadataValues) {
        return metadataValues.stream().map(MetadataValue::getValue).collect(Collectors.toList());
    }

    private List<MetadataValueDTO> getMetadataValueDtos(List<MetadataValue> metadataValues) {
        return metadataValues.stream().map(value -> new MetadataValueDTO(value)).collect(Collectors.toList());
    }

    private void applyMetadataCorrectionsOnItem(Context context, Item item,
        List<MetadataCorrection> metadataCorrections) throws SQLException {

        for (MetadataCorrection correction : metadataCorrections) {

            String metadataField = correction.getMetadataField();

            CorrectionType correctionType = correction.getCorrectionType();
            switch (correctionType) {
                case ADD:
                    addMetadataValues(context, item, metadataField, correction.getNewValues());
                    break;
                case MODIFY:
                    removeMetadataValues(context, item, metadataField);
                    addMetadataValues(context, item, metadataField, correction.getNewValues());
                    break;
                case REMOVE:
                    removeMetadataValues(context, item, metadataField);
                    break;
                default:
                    throw new IllegalArgumentException("Unkown metadata correction type: " + correctionType);
            }

        }

    }

    private void addMetadataValues(Context context, Item item, String metadataFieldAsString,
        List<MetadataValueDTO> values) throws SQLException {

        MetadataField metadataField = metadataFieldService.findByString(context, metadataFieldAsString, '.');
        if (metadataField == null) {
            throw new IllegalArgumentException("Unknown metadata field: " + metadataFieldAsString);
        }

        for (MetadataValueDTO metadataValue : values) {
            String language = metadataValue.getLanguage();
            String value = metadataValue.getValue();
            String authority = metadataValue.getAuthority();
            int confidence = metadataValue.getConfidence();
            itemService.addMetadata(context, item, metadataField, language, value, authority, confidence);
        }
    }

    private void removeMetadataValues(Context context, Item item, String metadataField) throws SQLException {
        List<MetadataValue> valuesToRemove = itemService.getMetadataByMetadataString(item, metadataField);
        itemService.removeMetadataValues(context, item, valuesToRemove);
    }

    public String getCorrectionRelationshipName() {
        return correctionRelationshipName;
    }

}
