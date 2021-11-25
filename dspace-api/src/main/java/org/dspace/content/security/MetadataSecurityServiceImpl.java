/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
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

/**
 * Implementation of {@link MetadataSecurityService}.
 *
 * @author Mykhaylo Boychuk (4science.it)
 * @author Luca Giamminonni (4science.it)
 */
public class MetadataSecurityServiceImpl implements MetadataSecurityService {

    @Resource(name = "securityLevelsMap")
    private final Map<String, MetadataSecurityEvaluation> securityLevelsMap = new HashMap<>();

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private MetadataExposureService metadataExposureService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ConfigurationService configurationService;

    private DCInputsReader dcInputsReader;

    @PostConstruct
    private void setup() throws DCInputsReaderException {
        this.dcInputsReader = new DCInputsReader();
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item,
        boolean preventBoxSecurityCheck) {
        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        return getPermissionFilteredMetadata(context, item, values, preventBoxSecurityCheck);
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item, String metadataField,
        boolean preventBoxSecurityCheck) {
        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);
        return getPermissionFilteredMetadata(context, item, metadataValues, preventBoxSecurityCheck);
    }

    @Override
    public boolean checkMetadataFieldVisibility(Context context, Item item, MetadataField metadataField,
        boolean preventBoxSecurityCheck) {
        List<CrisLayoutBox> boxes = findBoxes(context, item, preventBoxSecurityCheck);
        return isMetadataVisible(context, boxes, item, metadataField, preventBoxSecurityCheck);
    }

    private List<MetadataValue> getPermissionFilteredMetadata(Context context, Item item,
        List<MetadataValue> metadataValues, boolean preventBoxSecurityCheck) {

        if (item.isWithdrawn() && !isCurrentUserAdmin(context)) {
            return new ArrayList<MetadataValue>();
        }

        List<CrisLayoutBox> boxes = findBoxes(context, item, preventBoxSecurityCheck);

        Optional<List<DCInputSet>> submissionDefinitionInputs = submissionDefinitionInputs();
        if (submissionDefinitionInputs.isPresent()) {
            return fromSubmissionDefinition(context, boxes, item, submissionDefinitionInputs.get(), metadataValues);
        }

        return metadataValues.stream()
            .filter(value -> isMetadataVisible(context, boxes, item, value.getMetadataField(), preventBoxSecurityCheck))
            .filter(value -> isMetadataFieldReturnAllowed(context, item, value))
            .collect(Collectors.toList());

    }

    private boolean isMetadataFieldReturnAllowed(Context context, Item item, MetadataValue metadataValue) {
        Integer securityLevel = metadataValue.getSecurityLevel();
        if (securityLevel == null) {
            return true;
        }

        MetadataSecurityEvaluation metadataSecurityEvaluation = getMetadataSecurityEvaluator(securityLevel);
        try {
            return metadataSecurityEvaluation.allowMetadataFieldReturn(context, item, metadataValue.getMetadataField());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<CrisLayoutBox> findBoxes(Context context, Item item, boolean preventBoxSecurityCheck) {
        if (context == null || preventBoxSecurityCheck) {
            // the context could be null if the converter is used to prepare test data or in a batch script
            return new ArrayList<CrisLayoutBox>();
        }

        String entityType = itemService.getEntityType(item);
        try {
            return crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isMetadataVisible(Context context, List<CrisLayoutBox> boxes, Item item,
        MetadataField metadataField, boolean preventBoxSecurityCheck) {
        if (CollectionUtils.isNotEmpty(boxes)) {
            return checkMetadataFieldVisibilityByBoxes(context, boxes, item, metadataField, preventBoxSecurityCheck);
        }
        return isCurrentUserAdmin(context) ? true : isNotHidden(context, metadataField);
    }

    private boolean checkMetadataFieldVisibilityByBoxes(Context context, List<CrisLayoutBox> boxes, Item item,
        MetadataField metadataField, boolean preventBoxSecurityCheck) {

        if (isPublicMetadataField(metadataField, boxes, preventBoxSecurityCheck)) {
            return true;
        }

        if (preventBoxSecurityCheck) {
            return false;
        }

        EPerson currentUser = context.getCurrentUser();
        List<CrisLayoutBox> notPublicBoxes = getNotPublicBoxes(metadataField, boxes);

        if (Objects.nonNull(currentUser)) {

            for (CrisLayoutBox box : notPublicBoxes) {
                if (hasAccess(context, item, currentUser, box)) {
                    return true;
                }
            }
        }

        // the metadata is not included in any box so use the default dspace security
        if (notPublicBoxes.isEmpty() && isNotHidden(context, metadataField)) {
            return true;
        }

        return false;
    }

    private boolean hasAccess(Context context, Item item, EPerson currentUser, CrisLayoutBox box) {
        try {
            return crisLayoutBoxAccessService.hasAccess(context, currentUser, box, item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<String> getPublicMetadataFromConfig() {
        return List.of(configurationService.getArrayProperty("metadata.publicField"));
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

    private List<CrisLayoutBox> getNotPublicBoxes(MetadataField metadataField, List<CrisLayoutBox> boxes) {
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

    private boolean isPublicMetadataField(MetadataField metadataField, List<CrisLayoutBox> boxes,
        boolean preventBoxSecurityCheck) {

        List<String> publicFields = preventBoxSecurityCheck ? getPublicMetadataFromConfig() : getPublicMetadata(boxes);

        for (String publicField : publicFields) {
            if (publicField.equals(metadataField.toString('.'))) {
                return true;
            }
        }

        return false;
    }

    private Optional<List<DCInputSet>> submissionDefinitionInputs() {
        return Optional.ofNullable(requestService.getCurrentRequest())
            .map(rq -> (String) rq.getAttribute("submission-name"))
            .map(this::dcInputsSet);
    }

    private List<DCInputSet> dcInputsSet(final String sd) {
        try {
            return dcInputsReader.getInputsBySubmissionName(sd);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isCurrentUserAdmin(Context context) {
        try {
            return context != null && authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> fromSubmissionDefinition(Context context, List<CrisLayoutBox> boxes, Item item,
        final List<DCInputSet> dcInputSets, final List<MetadataValue> metadataValues) {
        Predicate<MetadataValue> inDcInputs = mv -> dcInputSets.stream().anyMatch((dc) -> {
            return dc.isFieldPresent(mv.getMetadataField().toString('.'))
                || checkMetadataFieldVisibilityByBoxes(context, boxes, item, mv.getMetadataField(), false);
        });
        return metadataValues.stream()
            .filter(inDcInputs)
            .collect(Collectors.toList());
    }

    private boolean isNotHidden(Context context, MetadataField metadataField) {
        try {
            return !metadataExposureService.isHidden(context, metadataField.getMetadataSchema().getName(),
                metadataField.getElement(), metadataField.getQualifier());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public MetadataSecurityEvaluation getMetadataSecurityEvaluator(int securityValue) {
        return securityLevelsMap.get(securityValue + "");
    }

}