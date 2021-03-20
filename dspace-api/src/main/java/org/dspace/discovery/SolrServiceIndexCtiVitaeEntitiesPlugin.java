/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.perucris.ctivitae.CvRelatedEntitiesService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom PGC implementation of {@link SolrServiceIndexPlugin} that add relations reference
 * between a CTIVitae community entity and its owning CvPerson entity, ensuring that in case
 * an equal entity is present in other collections and related to same CvProfile is unreferenced.
 * <p>
 * For example: in case of personal publications (CvPublication) it adds references to the CvPerson
 * owner of such publication. In case an equivalent publication is present in other collections (i.e. Directorio)
 * and it has a reference with the same CvPerson, this reference is removed.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SolrServiceIndexCtiVitaeEntitiesPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceIndexCtiVitaeEntitiesPlugin.class);
    private static final String CTIVITAE_OWNER_FIELD = "ctivitae.owner";

    private final ItemService itemService;
    private final ResearcherProfileService researcherProfileService;
    private final CvRelatedEntitiesService cvRelatedEntitiesService;
    private final IndexingService indexingService;
    private final ConfigurationService configurationService;

    @Autowired
    public SolrServiceIndexCtiVitaeEntitiesPlugin(ItemService itemService,
                                                  ResearcherProfileService researcherProfileService,
                                                  CvRelatedEntitiesService cvRelatedEntitiesService,
                                                  IndexingService indexingService,
                                                  ConfigurationService configurationService) {

        this.itemService = itemService;
        this.researcherProfileService = researcherProfileService;
        this.cvRelatedEntitiesService = cvRelatedEntitiesService;
        this.indexingService = indexingService;
        this.configurationService = configurationService;
    }

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {

        Optional<Item> item = item(indexableObject);

        item.ifPresent(i -> {
            try {
                tryToUpdateDocument(context, document, i);
            } catch (SQLException | AuthorizeException e) {
                LOGGER.error("An error occurred during ctivitae item related indexing", e);
            }
        });
    }

    private void tryToUpdateDocument(Context context, SolrInputDocument document, Item item)
        throws SQLException, AuthorizeException {

        if (isADirectorioItem(item)) {
            updateDirectorioItemReferences(context, item);
            return;
        }


        Optional<UUID> owner = owner(item);
        if (owner.isPresent()) {
            UUID ownerProfile = researcherProfileService.findById(context, owner.get()).getItem().getID();
            document.addField(CTIVITAE_OWNER_FIELD, UUIDUtils.toString(ownerProfile));
            Optional<Item> directorioRelatedItem = cvRelatedEntitiesService.findDirectorioRelated(context, item);
            if (directorioRelatedItem.isPresent()) {
                updateDirectorioItemReferences(context, directorioRelatedItem.get());
            }
        }
    }

    private void updateDirectorioItemReferences(Context context, Item directorioItem)
        throws SQLException, AuthorizeException {

        Set<UUID> relatedEntitiesProfiles = findCvProfileIdsOwningCloneOfItem(context, directorioItem);

        List<String> ctiVitaeProfilesRelatedToDirectorioItem =
            cvRelatedEntitiesService.findCtiVitaeRelatedProfiles(context, directorioItem)
                .stream().map(DSpaceObject::getID)
                .filter(id -> !relatedEntitiesProfiles.contains(id))
                .map(UUIDUtils::toString)
                .collect(Collectors.toList());


        if (!ctiVitaeProfilesRelatedToDirectorioItem.isEmpty()) {
            indexingService.updateCtiVitaeReferences(context, directorioItem.getID(),
                ctiVitaeProfilesRelatedToDirectorioItem);
        }
    }

    private Set<UUID> findCvProfileIdsOwningCloneOfItem(Context context, Item directorioItem)
        throws SQLException, AuthorizeException {

        Set<UUID> relatedEntitiesOwners =
            cvRelatedEntitiesService.findCTIVitaeRelated(context, directorioItem)
                .stream().map(cvEntity -> itemService.getMetadataByMetadataString(cvEntity, "cris.owner"))
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0).getAuthority())
                .filter(StringUtils::isNotBlank)
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

    private Optional<UUID> owner(Item item) {
        if (notACvEntity(item)) {
            return Optional.empty();
        }
        List<MetadataValue> ownerMetadata = itemService.getMetadataByMetadataString(item, "cris.owner");
        return Optional.ofNullable(ownerMetadata)
            .filter(list -> !list.isEmpty())
            .map(values -> values.get(0).getAuthority())
            .map(UUIDUtils::fromString);
    }

    private boolean isADirectorioItem(Item item) throws SQLException {
        String communityId = configurationService.getProperty("directorios.community-id");
        return item.getOwningCollection().getCommunities()
            .stream()
            .anyMatch(c -> UUIDUtils.toString(c.getID()).equals(communityId));
    }


    private boolean notACvEntity(Item item) {
        String entityType = itemService.getMetadata(item, "relationship.type");
        if (StringUtils.isBlank(entityType)) {
            return true;
        }
        return "CvPerson".equals(entityType) || (!entityType.startsWith("Cv") || entityType.endsWith("Clone"));
    }

    private Optional<Item> item(IndexableObject indexableObject) {
        if (!(indexableObject instanceof IndexableItem)) {
            return Optional.empty();
        }
        return Optional.ofNullable(((IndexableItem) indexableObject).getIndexedObject());
    }
}
