/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.sunat;

import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.perucris.externalservices.PeruExternalService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The purpose of this class is to: given an "OrgUnit" type item and given a SunatDTO object,
 * make a comparison between some metadata, and in case the metadata does not match-make an update.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithInformationFromSunatService implements PeruExternalService {

    private static Logger log = LogManager.getLogger(UpdateItemWithInformationFromSunatService.class);

    @Autowired
    private SunatProvider sunatProvider;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean updateItem(Context context, Item item) {
        String ruc = itemService.getMetadataFirstValue(item, "organization", "identifier", "ruc", Item.ANY);
        SunatDTO sunatInformations = sunatProvider.getSunatObject(ruc);
        if (Objects.isNull(sunatInformations)) {
            return false;
        }
        return updateWithSunatInformations(context, item, sunatInformations);
    }

    private boolean updateWithSunatInformations(Context context, Item currentItem, SunatDTO infoFromSunat) {
        boolean updated = false;
        if (checkMetadataAndUpdateIfNotMatch(context, currentItem, infoFromSunat.getCiiu(),
            "perucris", "type", "ciiu")) {
            updated = true;
        }
        if (checkMetadataAndUpdateIfNotMatch(context, currentItem, infoFromSunat.getLegalName(),
            "organization", "legalName", null)) {
            updated = true;
        }
        if (checkMetadataAndUpdateIfNotMatch(context, currentItem, infoFromSunat.getUbigeoSunat(),
            "perucris", "ubigeoSunat", null)) {
            updated = true;
        }
        if (checkMetadataAndUpdateIfNotMatch(context, currentItem, infoFromSunat.getAddressLocality(),
            "organization", "address", "addressLocality")) {
            updated = true;
        }
        return updated;
    }

    private boolean checkMetadataAndUpdateIfNotMatch(Context context, Item currentItem, String infoFromSunat,
            String schema, String element, String qualifier) {
        if (StringUtils.isNotBlank(infoFromSunat)) {
            if (!checkCurrentItemWithInformationFromSunat(currentItem, schema, element, qualifier, infoFromSunat)) {
                removeOldMetadata(context, currentItem, schema, element, qualifier);
                addNewMetadata(context, currentItem, schema, element, qualifier, infoFromSunat);
                return true;
            }
        }
        return false;
    }

    private boolean checkCurrentItemWithInformationFromSunat(Item currentItem,
            String schema, String element, String qualifier, String infoFromSunat) {
        String value = itemService.getMetadataFirstValue(currentItem, schema, element, qualifier, Item.ANY);
        if (StringUtils.equals(value, infoFromSunat)) {
            return true;
        }
        return false;
    }

    private void removeOldMetadata(Context context, Item currentItem, String schema, String element, String qualifier) {
        try {
            itemService.clearMetadata(context, currentItem, schema, element, qualifier, Item.ANY);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addNewMetadata(Context c, Item currentItem, String schema, String element, String qualifier,
            String infoFromSunat) {
        try {
            itemService.addMetadata(c, currentItem, schema, element, qualifier, null, infoFromSunat);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}