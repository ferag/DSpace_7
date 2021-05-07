/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.dspace.content.dao.RelationshipDAO;
import org.dspace.content.service.RelationshipPlacesIndexingService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class RelationshipPlacesIndexingServiceImpl implements RelationshipPlacesIndexingService {

    @Autowired
    private IndexingService indexingService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private RelationshipDAO relationshipDAO;

    @Override
    public void updateRelationReferences(final Context context, final Relationship relationship) throws SQLException {

        Item rightItem = relationship.getRightItem();
        final List<Relationship> relations =
            relationshipDAO.findByItemAndRelationshipType(context, rightItem,
                                                          relationship.getRelationshipType(), -1, -1);

        List<String> leftItemsIdsToAdd = new LinkedList<>();

        for (final Relationship relation : relations) {
            addRightItemsReferences(context, relationship, relation);

            int times = 1;
            if (singleDirectionRelationship("left", relationship.getRelationshipType())) {
                times = relation.getRightPlace() - relation.getLeftPlace();
            }
            leftItemsIdsToAdd.addAll(Collections.nCopies(times, relation.getLeftItem().getID().toString()));
        }
        if (!leftItemsIdsToAdd.isEmpty()) {

            indexingService.updateRelationForItem(rightItem.getID().toString(),
                                                  relationship.getRelationshipType().getRightwardType(),
                                                  leftItemsIdsToAdd);
        }
    }

    private void addRightItemsReferences(final Context context, final Relationship relationship,
                                         final Relationship relation) throws SQLException {
        final Item leftItem = relation.getLeftItem();
        final List<Relationship> leftItemRelationships =
            relationshipDAO.findByItemAndRelationshipType(context, leftItem, relationship.getRelationshipType(),
                                                          -1, -1);
        List<String> rightItemsToAdd = new LinkedList<>();
        for (final Relationship leftItemRelation : leftItemRelationships) {
            int times = 1;
            if (singleDirectionRelationship("right", relationship.getRelationshipType())) {
                times = leftItemRelation.getLeftPlace() - leftItemRelation.getRightPlace();
            }
            rightItemsToAdd.addAll(Collections.nCopies(times, leftItemRelation.getRightItem().getID().toString()));
        }
        if (!rightItemsToAdd.isEmpty())  {
            indexingService.updateRelationForItem(leftItem.getID().toString(),
                                                  relation.getRelationshipType().getLeftwardType(),
                                                  rightItemsToAdd);
        }
    }


    private boolean singleDirectionRelationship(final String direction, final RelationshipType relationshipType) {
        final String[] placesSettings = configurationService
                                            .getArrayProperty("relationship.places.only" + direction);
        if (placesSettings == null) {
            return false;
        }
        final String leftTypeLabel = Optional.ofNullable(relationshipType.getLeftType())
                                             .map(EntityType::getLabel).orElse("null");
        final String rightTypeLabel = Optional.ofNullable(relationshipType.getRightType())
                                              .map(EntityType::getLabel).orElse("null");

        return Arrays.stream(placesSettings)
                     .anyMatch(v -> v.equals(String.join("::",
                                                         leftTypeLabel,
                                                         rightTypeLabel,
                                                         relationshipType.getLeftwardType(),
                                                         relationshipType.getRightwardType())));
    }
}
