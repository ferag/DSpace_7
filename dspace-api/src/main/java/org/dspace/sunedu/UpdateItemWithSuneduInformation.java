/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sunedu;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.external.model.SuneduDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithSuneduInformation {

    private static Logger log = LogManager.getLogger(UpdateItemWithSuneduInformation.class);

    private UUID collectonUuid;

    public static int countItemUpdated = 0;

    @Autowired
    private SuneduProvider suneduProvider;

    @Autowired(required = true)
    protected SearchService searchService;

    @Autowired
    private ItemService itemService;

    public void updateInformation() {
        try (Context context = new Context()) {
            int count = 0;
            Iterator<Item> itemIterator = findItems(context);
            log.info("Sunedu update start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                updateItems(context, item);
                count++;
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            log.info("Sunedu update end");
            log.info("Item updated " + countItemUpdated);
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void updateItems(Context context, Item item) {
        String dni = itemService.getMetadataFirstValue(item, "perucris", "identifier", "dni", Item.ANY);
        List<SuneduDTO> suneduInformations = suneduProvider.getSundeduObject(dni);
        if (updateWithSuneduInformations(context, item, suneduInformations)) {
            countItemUpdated++;
        }
    }

    private boolean updateWithSuneduInformations(Context context, Item currentItem,List<SuneduDTO> suneduInformations) {
        boolean updated = true;
        List<MetadataValue> roles =  itemService.getMetadata(currentItem, "crisrp", "education", "role", null);
        List<MetadataValue> professional =  itemService.getMetadata(currentItem, "crisrp", "education", null, null);
        List<MetadataValue> countries = itemService.getMetadata(currentItem, "perucris", "education", "country", null);
        List<MetadataValue> university = itemService.getMetadata(currentItem, "perucris", "education", "grantor", null);

        if (roles.isEmpty() || roles == null) {
            cleanMetadata(context, currentItem);
            addMetadata(context, currentItem, suneduInformations);
            return updated;
        } else if (checkMetadata(roles, countries, university, professional, suneduInformations)) {
            return false;
        } else {
            cleanMetadata(context, currentItem);
            addMetadata(context, currentItem, suneduInformations);
            return updated;
        }
    }

    private boolean checkMetadata(List<MetadataValue> roles,
                                  List<MetadataValue> countries,
                                  List<MetadataValue> universities,
                                  List<MetadataValue> tituloProfesional,
                                  List<SuneduDTO> suneduInformations) {

        boolean ok = true;
        if (suneduInformations.size() != roles.size()) {
            return false;
        } else {
            for (SuneduDTO dto : suneduInformations) {
                if (!checkWithInformationsFromSunedu(dto.getAbreviaturaTitulo(), roles)) {
                    return false;
                }
                if (!checkWithInformationsFromSunedu(dto.getCountry(),countries)) {
                    return false;
                }
                if (!checkWithInformationsFromSunedu(dto.getUniversity(), universities)) {
                    return false;
                }
                if (!checkWithInformationsFromSunedu(dto.getProfessionalQualification(), tituloProfesional)) {
                    return false;
                }
            }
        }
        return ok;
    }

    private boolean checkWithInformationsFromSunedu(String suneduInfo, List<MetadataValue> currentItemMetadata) {
        for (MetadataValue mv : currentItemMetadata) {
            if (mv.getValue().equals(suneduInfo)) {
                return true;
            }
        }
        return false;
    }

    private void cleanMetadata(Context context, Item currentItem) {
        try {
            itemService.clearMetadata(context, currentItem, "crisrp", "education", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "crisrp", "education", "role", Item.ANY);
            itemService.clearMetadata(context, currentItem, "crisrp", "qualification", "end", Item.ANY);
            itemService.clearMetadata(context, currentItem, "crisrp", "qualification", "start", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "education", "country", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "education", "grantor", Item.ANY);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addMetadata(Context context, Item currentItem, List<SuneduDTO> suneduInformations) {
        for (SuneduDTO dto : suneduInformations) {
            try {
                itemService.addMetadata(context, currentItem, "crisrp", "education", "role", null,
                        dto.getAbreviaturaTitulo());
                itemService.addMetadata(context, currentItem, "crisrp", "education", null, null,
                        dto.getProfessionalQualification());
                itemService.addMetadata(context, currentItem, "perucris", "education", "country", null,
                        dto.getCountry());
                itemService.addMetadata(context, currentItem, "perucris", "education", "grantor", null,
                        dto.getUniversity());
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private Iterator<Item> findItems(Context context)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries("relationship.type:Person");
        discoverQuery.addFilterQueries("perucris.identifier.dni:*");
        discoverQuery.addFilterQueries("location.coll:" + this.collectonUuid.toString());
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    public UUID getCollectonUuid() {
        return collectonUuid;
    }

    public void setCollectonUuid(UUID collectonUuid) {
        this.collectonUuid = collectonUuid;
    }
}
