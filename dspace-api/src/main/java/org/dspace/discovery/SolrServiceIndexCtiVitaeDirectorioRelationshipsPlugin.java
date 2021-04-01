/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.util.stream.Collectors.toMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.perucris.ctivitae.CvRelatedEntitiesService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom PGC implementation of {@link SolrServiceIndexPlugin} that add relations reference
 * between a Directorio entity and its owning CvPerson entity, if present.
 * <p>
 * For example: in case of a publication (Publication) it adds references to the CvPerson
 * owner of the profile that is author or editor of such publication, if present.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin.class);

    private final CvRelatedEntitiesService cvRelatedEntitiesService;
    private final ItemService itemService;
    private final CollectionService collectionService;

    @Autowired
    public SolrServiceIndexCtiVitaeDirectorioRelationshipsPlugin(
        CvRelatedEntitiesService cvRelatedEntitiesService, ItemService itemService,
        CollectionService collectionService) {

        this.cvRelatedEntitiesService = cvRelatedEntitiesService;
        this.itemService = itemService;
        this.collectionService = collectionService;
    }

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {

        Optional<Item> item = Optional.empty();

        try {
            item = item(context, indexableObject);
        } catch (SQLException e) {
            LOGGER.error("An error occurred during ctivitae item related indexing", e);
        }

        item.ifPresent(i -> {
            try {
                tryToUpdateDocument(context, document, i);
            } catch (SQLException | AuthorizeException e) {
                LOGGER.error("An error occurred during ctivitae item related indexing", e);
            }
        });
    }

    private Optional<Item> item(Context context, IndexableObject indexableObject) throws SQLException {
        if (!(indexableObject instanceof IndexableItem)) {
            return Optional.empty();
        }
        Item indexedObject = ((IndexableItem) indexableObject).getIndexedObject();
        if (Objects.isNull(indexedObject)) {
            return Optional.empty();
        }
        if (notADirectorioItem(context, indexedObject)) {
            return Optional.empty();
        }
        return Optional.of(indexedObject);
    }

    private void tryToUpdateDocument(Context context, SolrInputDocument document, Item item)
        throws SQLException, AuthorizeException {
        {
            Map<String, String> profilesMap =
                cvRelatedEntitiesService.
                    findCtiVitaeRelationsForDirectorioItem(context, item)
                    .stream()
                    .collect(toMap(dso -> UUIDUtils.toString(dso.getID()),
                        dso -> Optional.ofNullable(dso.getName()).orElse("")));

            if (!profilesMap.isEmpty()) {
                document.addField("perucris.ctivitae.owner", new ArrayList<>(profilesMap.values()));
                document.addField("perucris.ctivitae.owner_authority", new ArrayList<>(profilesMap.keySet()));
            }
        }
    }

    private boolean notADirectorioItem(Context context, Item item) throws SQLException {

        if (!cvRelatedEntitiesService.entityWithCvReferences(itemService.getMetadata(item,
            "relationship.type"))) {
            return true;
        }
        boolean directorioCollection = collectionService.isDirectorioCollection(context, item.getOwningCollection());
        return !directorioCollection;
    }
}
