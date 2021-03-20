/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.ctivitae;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service that finds PGC relations between Items in CTIVitae Community and their linked entities into
 * Directorio and vice versa, if present.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CvRelatedEntitiesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CvRelatedEntitiesService.class);

    private final ConcytecWorkflowService concytecWorkflowService;
    private final ItemService itemService;
    private final RelationshipService relationshipService;
    private final RelationshipTypeService relationshipTypeService;

    private Map<String, List<String>> entityToMetadataMap = new HashMap<>();

    @Autowired
    public CvRelatedEntitiesService(ConcytecWorkflowService concytecWorkflowService,
                                    ItemService itemService,
                                    RelationshipService relationshipService,
                                    RelationshipTypeService relationshipTypeService) {
        this.concytecWorkflowService = concytecWorkflowService;
        this.itemService = itemService;
        this.relationshipService = relationshipService;
        this.relationshipTypeService = relationshipTypeService;
    }

    /**
     * given a DSpace Item finds, if present, its related counterpart into the Directorio community, if present.
     *
     * @param context DSpace application context
     * @param item DSpace item
     * @return
     */
    public Optional<Item> findDirectorioRelated(Context context, Item item) {
        try {
            Item clone = concytecWorkflowService.findClone(context, item);
            if (Objects.isNull(clone)) {
                return Optional.empty();
            }
            return Optional.ofNullable(concytecWorkflowService.findShadowItemCopy(context, clone));
        } catch (SQLException e) {
            LOGGER.warn("Error while finding directorio related entities for {}: {}", item.getID(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * given a DSpace Item finds its related counterparts into the CTIVitae community, if present
     *
     *
     * @param context DSpace application context
     * @param item DSpace item
     * @return
     */
    public Collection<Item> findCTIVitaeRelated(Context context, Item item) {
        try {
            List<Item> itemsToLookup = new LinkedList<>();
            itemsToLookup.add(item);
            itemsToLookup.addAll(concytecWorkflowService.findMergedInItems(context, item));
            Collection<Item> result = new LinkedList<>();
            for (Item i : itemsToLookup) {
                findCloned(context, i).map(result::add);
            }
            return result;

        } catch (SQLException e) {
            LOGGER.error("Error while finding CTIVItae related items of {}: {}", item.getID(),
                e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Given a DSpace item finds, using provided list of metadata fields, a list of CvPerson
     * to which this item can be related.
     *
     * @param context DSpace application context
     * @param item DSpace item
     * @return
     */
    public Collection<Item> findCtiVitaeRelatedProfiles(Context context, Item item) {

        List<Item> result = new LinkedList<>();
        try {
            for (String id : itemRelatedPersonIds(item)) {
                cvRelatedPerson(context, id).ifPresent(result::add);
            }
        } catch (SQLException e) {
            LOGGER.error("Error while finding CTIVItae related items of {}: {}", item.getID(),
                e.getMessage(), e);
        }

        return result;
    }

    public void setEntityToMetadataMap(Map<String, List<String>> entityToMetadataMap) {
        this.entityToMetadataMap = entityToMetadataMap;
    }

    private Optional<Item> cvRelatedPerson(Context context, String id) throws SQLException {
        Item person = itemService.find(context, UUIDUtils.fromString(id));

        return personOwner(context, person);
    }

    private Optional<Item> personOwner(Context context, Item person) throws SQLException {

        //FIXME: since relation is always the same, evaluate a way of caching it
        List<RelationshipType> relationshipType = relationshipTypeService.findByItemAndTypeNames(context,
            person, false, "isPersonOwner", "isOwnedByCvPerson");

        List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context,
            person, relationshipType.get(0), false);

        if (relationships.isEmpty()) {
            return Optional.empty();
        }

        if (relationships.size() > 1) {
            throw new RuntimeException("More than one owning profile for person " + person.getID());
        }

        return Optional.of(relationships.get(0).getLeftItem());

    }

    private Optional<Item> findCloned(Context context, Item item) throws SQLException {
        Item copiedItem = concytecWorkflowService.findCopiedItem(context, item);
        if (Objects.nonNull(copiedItem)) {
            Item clonedItem = concytecWorkflowService.findClonedItem(context, copiedItem);
            return Optional.ofNullable(clonedItem);
        }
        return Optional.empty();
    }

    private List<String> itemRelatedPersonIds(Item item) {
        String entityType = itemService.getMetadata(item, "relationship.type");
        List<String> me = entityToMetadataMap.get(entityType);
        return me.stream()
            .map(md -> itemService.getMetadataByMetadataString(item, md))
            .flatMap(Collection::stream)
            .map(MetadataValue::getAuthority)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }
}
