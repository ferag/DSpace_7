/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ItemConverter
        extends DSpaceObjectConverter<Item, ItemRest>
        implements IndexableObjectConverter<Item, ItemRest> {

    @Autowired
    private ItemService itemService;

    @Resource(name = "securityLevelsMap")
    private final Map<String, MetadataSecurityEvaluation> securityLevelsMap = new HashMap<>();

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    GroupService groupService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DCInputsReader dcInputsReader;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemConverter.class);

    @Override
    public ItemRest convert(Item obj, Projection projection) {
        ItemRest item = super.convert(obj, projection);
        item.setInArchive(obj.isArchived());
        item.setDiscoverable(obj.isDiscoverable());
        item.setWithdrawn(obj.isWithdrawn());
        item.setLastModified(obj.getLastModified());

        List<MetadataValue> entityTypes =
            itemService.getMetadata(obj, "dspace", "entity", "type", Item.ANY, false);
        if (CollectionUtils.isNotEmpty(entityTypes) && StringUtils.isNotBlank(entityTypes.get(0).getValue())) {
            item.setEntityType(entityTypes.get(0).getValue());
        }

        return item;
    }

    /**
     * Retrieves the metadata list filtered according to the hidden metadata configuration
     * When the context is null, it will return the metadatalist as for an anonymous user
     * Overrides the parent method to include virtual metadata
     * @param context The context
     * @param obj     The object of which the filtered metadata will be retrieved
     * @return A list of object metadata (including virtual metadata) filtered based on the the hidden metadata
     * configuration
     */
    @Override
    public MetadataValueList getPermissionFilteredMetadata(Context context, Item obj, Projection projection) {

        List<MetadataValue> fullList = itemService.getMetadata(obj, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);

        List<MetadataValue> returnList = new LinkedList<>();
        String entityType = itemService.getMetadataFirstValue(obj, "dspace", "entity", "type", Item.ANY);

        try {

            if (obj.isWithdrawn() && (Objects.isNull(context) ||
                Objects.isNull(context.getCurrentUser()) || !authorizeService.isAdmin(context))) {
                return new MetadataValueList(new ArrayList<MetadataValue>());
            }

            List<CrisLayoutBox> boxes;
            if (context != null && !preventSecurityCheck(projection)) {
                boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
            } else {
                // the context could be null if the converter is used to prepare test data or in a batch script
                boxes = new ArrayList<CrisLayoutBox>();
            }

            Optional<List<DCInputSet>> submissionDefinitionInputs = submissionDefinitionInputs();
            if (submissionDefinitionInputs.isPresent()) {
                return fromSubmissionDefinition(context, boxes, obj, submissionDefinitionInputs.get(), fullList);
            }

            for (MetadataValue metadataValue : fullList) {
                MetadataField metadataField = metadataValue.getMetadataField();
                if (checkMetadataFieldVisibility(context, boxes, obj, metadataField, projection)) {
                    if (metadataValue.getSecurityLevel() != null) {
                        MetadataSecurityEvaluation metadataSecurityEvaluation =
                            mapBetweenSecurityLevelAndClassSecurityLevel( metadataValue.getSecurityLevel());
                        if (metadataSecurityEvaluation.allowMetadataFieldReturn(context, obj ,metadataField)) {
                            returnList.add(metadataValue);
                        }
                    } else {
                        returnList.add(metadataValue);
                    }
                }
            }
        } catch (SQLException e ) {
            log.error("Error filtering item metadata based on permissions", e);
        }
        return new MetadataValueList(returnList);
    }

//    @Override
    public boolean checkMetadataFieldVisibility(Context context, Item item,
                                                MetadataField metadataField) throws SQLException {
        return checkMetadataFieldVisibility(context, item, metadataField, null);
    }

    public boolean checkMetadataFieldVisibility(Context context, Item item,
            MetadataField metadataField, Projection projection) throws SQLException {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        List<CrisLayoutBox> boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
        return checkMetadataFieldVisibility(context, boxes, item, metadataField, projection);
    }

    private Optional<List<DCInputSet>> submissionDefinitionInputs() {
        return Optional.ofNullable(requestService.getCurrentRequest())
                .map(rq -> (String )rq.getAttribute("submission-name"))
                .map(this::dcInputsSet);
    }

    // private method to catch checked exception that might occur during a lambda call
    private List<DCInputSet> dcInputsSet(final String sd) {
        try {
            return dcInputsReader.getInputsBySubmissionName(sd);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private MetadataValueList fromSubmissionDefinition(Context context, List<CrisLayoutBox> boxes, Item item,
            final List<DCInputSet> dcInputSets, final List<MetadataValue> fullList) {

        Predicate<MetadataValue> inDcInputs = mv -> dcInputSets.stream()
                .anyMatch((dc) -> {
                    try {
                        return dc.isFieldPresent(mv.getMetadataField().toString('.')) ||
                                checkMetadataFieldVisibilityByBoxes(context, boxes, item, mv.getMetadataField(), null);
                    } catch (SQLException e) {
                        return false;
                    }
                });

        List<MetadataValue> metadataFields = fullList.stream()
                                                     .filter(inDcInputs)
                                                     .collect(Collectors.toList());

        return new MetadataValueList(metadataFields);
    }

    private boolean checkMetadataFieldVisibility(Context context, List<CrisLayoutBox> boxes, Item item,
            MetadataField metadataField, Projection projection) throws SQLException {
        if (boxes.size() == 0 && !preventSecurityCheck(projection)) {
            if (context != null && authorizeService.isAdmin(context)) {
                return true;
            } else {
                if (!metadataExposureService
                        .isHidden(context, metadataField.getMetadataSchema().getName(),
                                  metadataField.getElement(),
                                  metadataField.getQualifier())) {
                    return true;
                }
            }
        } else {
            return checkMetadataFieldVisibilityByBoxes(context, boxes, item, metadataField, projection);
        }
        return false;
    }

    private boolean checkMetadataFieldVisibilityByBoxes(Context context, List<CrisLayoutBox> boxes, Item item,
                                                        MetadataField metadataField,
                                                        Projection projection) throws SQLException {
        boolean performSecurityCheck = !preventSecurityCheck(projection);
        List<String> allPublicMetadata = performSecurityCheck ? getPublicMetadata(boxes) : publicMetadataFromConfig();
        if (isPublicMetadataField(metadataField, allPublicMetadata)) {
            return true;
        } else {
            if (performSecurityCheck) {

                EPerson currentUser = context.getCurrentUser();
                List<CrisLayoutBox> boxesWithMetadataFieldExcludedPublic = getBoxesWithMetadataFieldExcludedPublic(
                    metadataField, boxes);

                for (CrisLayoutBox box : boxesWithMetadataFieldExcludedPublic) {
                    if (crisLayoutBoxAccessService.hasAccess(context, currentUser, box, item)) {
                        return true;
                    }
                }
                // the metadata is not included in any box so use the default dspace security
                if (boxesWithMetadataFieldExcludedPublic.size() == 0) {
                    if (!metadataExposureService
                        .isHidden(context, metadataField.getMetadataSchema().getName(),
                            metadataField.getElement(),
                            metadataField.getQualifier())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<String> publicMetadataFromConfig() {
        return Arrays.stream(configurationService.getArrayProperty("metadata.publicField"))
            .collect(Collectors.toList());
    }

    private boolean preventSecurityCheck(Projection projection) {
        return Optional.ofNullable(projection)
            .map(Projection::preventMetadataLevelSecurity)
            .orElse(false);
    }

    private List<CrisLayoutBox> getBoxesWithMetadataFieldExcludedPublic(MetadataField metadataField,
            List<CrisLayoutBox> boxes) {
        List<CrisLayoutBox> boxesWithMetadataField = new LinkedList<CrisLayoutBox>();
        for (CrisLayoutBox box : boxes) {
            List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
            for (CrisLayoutField field : crisLayoutFields) {
                if (field instanceof CrisLayoutFieldMetadata) {
                    checkField(metadataField, boxesWithMetadataField, box, field.getMetadataField());
                    for (CrisMetadataGroup metadataGroup : field.getCrisMetadataGroupList()) {
                        checkField(metadataField, boxesWithMetadataField, box, metadataGroup.getMetadataField());
                    }
                }
            }
        }
        return boxesWithMetadataField;
    }

    private void checkField(MetadataField metadataField, List<CrisLayoutBox> boxesWithMetadataField, CrisLayoutBox box,
            MetadataField field) {
        if (field.equals(metadataField) && box.getSecurity() != LayoutSecurity.PUBLIC.getValue()) {
            boxesWithMetadataField.add(box);
        }
    }

    private boolean isPublicMetadataField(MetadataField metadataField, List<String> allPublicMetadata) {
        for (String publicField : allPublicMetadata) {
            if (publicField.equals(metadataField.toString('.'))) {
                return true;
            }
        }
        return false;
    }

    private List<String> getPublicMetadata(List<CrisLayoutBox> boxes) {
        List<String> publicMetadata = new ArrayList<String>();
        for (CrisLayoutBox box : boxes) {
            if (box.getSecurity() == LayoutSecurity.PUBLIC.getValue()) {
                List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
                for (CrisLayoutField field : crisLayoutFields) {
                    if (field instanceof CrisLayoutFieldMetadata) {
                        publicMetadata.add(field.getMetadataField().toString('.'));
                    }
                }
            }
        }
        return publicMetadata;
    }

    @Override
    protected ItemRest newInstance() {
        return new ItemRest();
    }

    @Override
    public Class<Item> getModelClass() {
        return Item.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Item;
    }

    public MetadataSecurityEvaluation mapBetweenSecurityLevelAndClassSecurityLevel(int securityValue) {
        return securityLevelsMap.get(securityValue + "");
    }
}
