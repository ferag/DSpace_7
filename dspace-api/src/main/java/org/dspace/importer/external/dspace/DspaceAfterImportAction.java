/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DspaceAfterImportAction implements AfterImportAction, InitializingBean {

    private final RelationshipTypeService relationshipTypeService;
    private final RelationshipService relationshipService;
    private final ItemService itemService;
    private final EntityTypeService entityTypeService;

    private final RelationshipCoordinates relationshipCoordinates = new RelationshipCoordinates();

    public DspaceAfterImportAction(RelationshipTypeService relationshipTypeService,
                                   RelationshipService relationshipService,
                                   ItemService itemService,
                                   EntityTypeService entityTypeService) {
        this.relationshipTypeService = relationshipTypeService;
        this.relationshipService = relationshipService;
        this.itemService = itemService;
        this.entityTypeService = entityTypeService;
    }

    @Override
    public void applyTo(Context context, Item item, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {
        EntityType leftType =
            entityTypeService.findByEntityType(context, relationshipCoordinates.leftEntityType());
        EntityType rightType =
            entityTypeService.findByEntityType(context, relationshipCoordinates.rightEntityType());

        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, leftType, rightType,
                relationshipCoordinates.leftwardType(),
                relationshipCoordinates.rightwardType());

        if (Objects.isNull(relationshipType)) {
            return;
        }

        Item rightItem = itemService.find(context, UUID.fromString(externalDataObject.getId()));
        Relationship relationship = relationshipService.create(context, item, rightItem,
            relationshipType, -1, -1);

        context.turnOffAuthorisationSystem();
        relationshipService.updateItem(context, relationship.getLeftItem());
        relationshipService.updateItem(context, relationship.getRightItem());
        context.restoreAuthSystemState();
    }

    public void setLeftEntityType(String leftEntityType) {
        relationshipCoordinates.withLeftEntityType(leftEntityType);
    }

    public void setRightEntityType(String rightEntityType) {
        relationshipCoordinates.withRightEntityType(rightEntityType);
    }

    public void setLeftwardType(String leftwardType) {
        relationshipCoordinates.withLeftwardType(leftwardType);
    }

    public void setRightwardType(String rightwardType) {
        relationshipCoordinates.withRightwardType(rightwardType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> emptyCoordinates = relationshipCoordinates.emptyCoordinates();
        if (emptyCoordinates.isEmpty()) {
            return;
        }
        throw new RelationshipCoordinatesException(emptyCoordinates);
    }

    private static class RelationshipCoordinates {


        private String leftEntityType;
        private String rightEntityType;
        private String leftwardType;
        private String rightwardType;

        public RelationshipCoordinates withLeftEntityType(String leftEntityType) {
            this.leftEntityType = leftEntityType;
            return this;
        }

        public RelationshipCoordinates withRightEntityType(String rightEntityType) {
            this.rightEntityType = rightEntityType;
            return this;
        }

        public RelationshipCoordinates withLeftwardType(String leftwardType) {
            this.leftwardType = leftwardType;
            return this;
        }

        public RelationshipCoordinates withRightwardType(String rightwardType) {
            this.rightwardType = rightwardType;
            return this;
        }

        public List<String> emptyCoordinates() {
            List<String> emptyCoordinates = new ArrayList<>(4);
            checkCoordinate(leftEntityType, "leftEntityType").ifPresent(emptyCoordinates::add);
            checkCoordinate(rightEntityType, "rightEntityType").ifPresent(emptyCoordinates::add);
            checkCoordinate(leftwardType, "leftwardType").ifPresent(emptyCoordinates::add);
            checkCoordinate(rightwardType, "rightwardType").ifPresent(emptyCoordinates::add);
            return emptyCoordinates;
        }

        private Optional<String> checkCoordinate(String coordinate, String label) {
            return StringUtils.isBlank(coordinate) ? Optional.of(label) : Optional.empty();
        }

        public String leftEntityType() {
            return leftEntityType;
        }

        public String rightEntityType() {
            return rightEntityType;
        }

        public String leftwardType() {
            return leftwardType;
        }

        public String rightwardType() {
            return rightwardType;
        }
    }

    public static class RelationshipCoordinatesException extends RuntimeException {
        public RelationshipCoordinatesException(
            List<String> emptyCoordinates) {
            super("Following relationship coordinates are missing: " +
                emptyCoordinates.stream().collect(Collectors.joining(",")));
        }
    }
}
