/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sunedu;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.external.model.SuneduDTO;
import org.dspace.reniec.PeruExternalService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithInformationFromSuneduService implements PeruExternalService {

    private static Logger log = LogManager.getLogger(UpdateItemWithInformationFromSuneduService.class);

    @Autowired
    private SuneduProvider suneduProvider;

    @Autowired(required = true)
    protected SearchService searchService;

    @Autowired
    private ItemService itemService;

    @Override
    public void updateItem(Context context, Item item) {
        String dni = itemService.getMetadataFirstValue(item, "perucris", "identifier", "dni", Item.ANY);
        List<SuneduDTO> suneduInformations = suneduProvider.getSundeduObject(dni);
        updateWithSuneduInformations(context, item, suneduInformations);
    }

    private void updateWithSuneduInformations(Context context, Item currentItem,List<SuneduDTO> suneduInformations) {
        List<MetadataValue> roles =  itemService.getMetadata(currentItem, "crisrp", "education", "role", null);
        List<MetadataValue> professional =  itemService.getMetadata(currentItem, "crisrp", "education", null, null);
        List<MetadataValue> countries = itemService.getMetadata(currentItem, "perucris", "education", "country", null);
        List<MetadataValue> university = itemService.getMetadata(currentItem, "perucris", "education", "grantor", null);

        if (roles == null || roles.isEmpty()
            || !sameMetadata(roles, countries, university, professional, suneduInformations)) {

            cleanMetadata(context, currentItem);
            addMetadata(context, currentItem, suneduInformations);
        }
    }

    private boolean sameMetadata(List<MetadataValue> roles,
                                 List<MetadataValue> countries,
                                 List<MetadataValue> universities,
                                 List<MetadataValue> tituloProfesional,
                                 List<SuneduDTO> suneduInformations) {

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
        return true;
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

}
