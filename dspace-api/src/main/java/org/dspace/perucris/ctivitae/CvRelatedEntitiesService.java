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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
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
    private final ResearcherProfileService researcherProfileService;

    private Map<String, List<String>> entityToMetadataMap = new HashMap<>();

    @Autowired
    public CvRelatedEntitiesService(ConcytecWorkflowService concytecWorkflowService,
                                    ItemService itemService,
                                    RelationshipService relationshipService,
                                    RelationshipTypeService relationshipTypeService,
                                    ResearcherProfileService researcherProfileService) {
        this.concytecWorkflowService = concytecWorkflowService;
        this.itemService = itemService;
        this.relationshipService = relationshipService;
        this.relationshipTypeService = relationshipTypeService;
        this.researcherProfileService = researcherProfileService;
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

    /**
     * Given a relationship type, returns true if this entity type can have references to
     * person metadata, false otherwise.
     * @param relationshipType
     * @return
     */
    public boolean entityWithCvReferences(String relationshipType) {
        return entityToMetadataMap.containsKey(relationshipType);
    }

    /**
     * Given a directorio item, find ids of CTIVitae Profiles that can be directly put in relation
     * with it, since they are not related to an item of CTIVitae community that is in relation with
     * this directorio item. In this case, in this second case, profile the relation to be considered is
     * the one existing between the CTIVitae profile and the copy of this directorio item.
     *
     * @param context DSpace application context
     * @param item a directorio item
     * @return
     */
    public List<String> findCtiVitaeRelationsForDirectorioItem(Context context, Item item)
        throws SQLException, AuthorizeException {
        // ids of profiles that are in relation with directorio item clone
        Set<UUID> relatedEntitiesProfiles = findCvProfileIdsOwningCloneOfItem(context, item);

        // Lookup of cti vitae profile ids that are in relation with directorio item and that are not
        // owners of directorio entity clone into their personal CTIVitae space.
        List<String> ctiVitaeProfilesRelatedToDirectorioItem = findCtiVitaeRelatedProfiles(context, item)
            .stream().map(DSpaceObject::getID)
                .filter(id -> !relatedEntitiesProfiles.contains(id))
                .map(UUIDUtils::toString)
                .collect(Collectors.toList());
        return ctiVitaeProfilesRelatedToDirectorioItem;
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

        if (Objects.isNull(relationshipType) || relationshipType.isEmpty()) {
            LOGGER.warn("Unable to find relationship type isPersonOwner for {}", person.getID().toString());
            return Optional.empty();
        }

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

    private Set<UUID> findCvProfileIdsOwningCloneOfItem(Context context, Item directorioItem)
        throws SQLException, AuthorizeException {

        Set<UUID> relatedEntitiesOwners = findCTIVitaeRelated(context, directorioItem)
                .stream().map(cvEntity -> itemService.getMetadataByMetadataString(cvEntity, "cris.owner"))
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0).getAuthority())
                .filter(org.apache.commons.lang.StringUtils::isNotBlank)
                .map(UUIDUtils::fromString)
                .collect(Collectors.toSet());

        Set<UUID> relatedEntitiesProfiles = new HashSet<>();
        for (UUID ownerId : relatedEntitiesOwners) {
            ResearcherProfile researcherProfile = researcherProfileService.findById(context, ownerId);
            if (Objects.isNull(researcherProfile)) {
                continue;
            }
            Optional.ofNullable(researcherProfile.getItem())
                .map(DSpaceObject::getID)
                .ifPresent(relatedEntitiesProfiles::add);

        }
        return relatedEntitiesProfiles;
    }
}
