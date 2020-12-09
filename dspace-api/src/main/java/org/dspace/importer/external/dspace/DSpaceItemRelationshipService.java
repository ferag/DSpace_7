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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service that handles relationship between two DSpace objects.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceItemRelationshipService implements InitializingBean {
    private final RelationshipTypeService relationshipTypeService;
    private final RelationshipService relationshipService;
    private final EntityTypeService entityTypeService;
    private final ItemService itemService;

    private final RelationshipCoordinates
        relationshipCoordinates = new RelationshipCoordinates();

    @Autowired
    public DSpaceItemRelationshipService(RelationshipTypeService relationshipTypeService,
                                         RelationshipService relationshipService,
                                         EntityTypeService entityTypeService,
                                         ItemService itemService) {
        this.relationshipTypeService = relationshipTypeService;
        this.relationshipService = relationshipService;
        this.entityTypeService = entityTypeService;
        this.itemService = itemService;
    }


    public void create(Context context, Item item, Item rightItem) throws SQLException, AuthorizeException {

        RelationshipType relationshipType = findRelationshipType(context);

        if (Objects.isNull(relationshipType)) {
            return;
        }
        Relationship relationship = relationshipService.create(context, item, rightItem,
            relationshipType, -1, -1);

        try {
            context.turnOffAuthorisationSystem();
            relationshipService.updateItem(context, relationship.getLeftItem());
            relationshipService.updateItem(context, relationship.getRightItem());
        } finally {
            context.restoreAuthSystemState();
        }
    }

    public void delete(Context context, Item item) throws SQLException, AuthorizeException {
        RelationshipType relationshipType = findRelationshipType(context);

        if (Objects.isNull(relationshipType)) {
            return;
        }
        List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context, item,
            relationshipType);

        if (Objects.isNull(relationships)) {
            return;
        }

        try {
            context.turnOffAuthorisationSystem();
            for (Relationship relationship : relationships) {
                relationshipService.delete(context, relationship, true);
            }
        } finally {
            context.restoreAuthSystemState();
        }
        List<MetadataValue> metadata = itemService.getMetadata(
            item, "relation", relationshipCoordinates.rightEntityType, null, Item.ANY);
        itemService.removeMetadataValues(context, item, metadata);
    }

    private RelationshipType findRelationshipType(Context context) throws SQLException {
        EntityType leftType =
            entityTypeService.findByEntityType(context, relationshipCoordinates.leftEntityType());
        EntityType rightType =
            entityTypeService.findByEntityType(context, relationshipCoordinates.rightEntityType());

        RelationshipType relationshipType = relationshipTypeService
            .findbyTypesAndTypeName(context, leftType, rightType,
                relationshipCoordinates.leftwardType(),
                relationshipCoordinates.rightwardType());
        return relationshipType;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> emptyCoordinates = relationshipCoordinates.emptyCoordinates();
        if (emptyCoordinates.isEmpty()) {
            return;
        }
        throw new RelationshipCoordinatesException(emptyCoordinates);
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


    private static class RelationshipCoordinates {


        private String leftEntityType;
        private String rightEntityType;
        private String leftwardType;
        private String rightwardType;

        RelationshipCoordinates withLeftEntityType(String leftEntityType) {
            this.leftEntityType = leftEntityType;
            return this;
        }

        RelationshipCoordinates withRightEntityType(String rightEntityType) {
            this.rightEntityType = rightEntityType;
            return this;
        }

        RelationshipCoordinates withLeftwardType(String leftwardType) {
            this.leftwardType = leftwardType;
            return this;
        }

        RelationshipCoordinates withRightwardType(String rightwardType) {
            this.rightwardType = rightwardType;
            return this;
        }

        List<String> emptyCoordinates() {
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
