/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.renacyt;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.perucris.externalservices.PeruExternalService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithInformationFromRenacytService implements PeruExternalService {
    private static Logger log = LogManager.getLogger(UpdateItemWithInformationFromRenacytService.class);

    @Autowired
    private RenacytProvider renacytProvider;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean updateItem(Context context, Item item) {
        String dni = itemService.getMetadataFirstValue(item, "perucris", "identifier", "dni", Item.ANY);
        RenacytDTO informationsFromRenacyt = renacytProvider.getRenacytObject(dni);
        return updateCurrentItemWithInformationsFromRenacyt(context, item, informationsFromRenacyt);
    }

    private boolean updateCurrentItemWithInformationsFromRenacyt(Context context, Item currentItem,
                                                                 RenacytDTO informationsFromRenacyt) {
        if (informationsFromRenacyt.isEmpty()) {
            return false;
        }
        boolean updated = false;
        if (tryToAddMetadataIfNotExistYet(context, currentItem, informationsFromRenacyt.getLevel(), "crisrp",
                "qualification", null)) {
            updated = true;
        }
        if (tryToAddMetadataIfNotExistYet(context, currentItem, informationsFromRenacyt.getGroup(), "crisrp",
                "qualification", "group")) {
            updated = true;
        }
        if (informationsFromRenacyt.getStartDate() != null) {
            String startDate = informationsFromRenacyt.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (tryToAddMetadataIfNotExistYet(context, currentItem, startDate, "crisrp", "qualification", "start")) {
                updated = true;
            }
        }
        if (informationsFromRenacyt.getEndDate() != null) {
            String endDate = informationsFromRenacyt.getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (tryToAddMetadataIfNotExistYet(context, currentItem, endDate, "crisrp", "qualification", "end")) {
                updated = true;
            }
        }
        return updated;
    }

    private boolean tryToAddMetadataIfNotExistYet(Context context, Item currentItem, String infoFromRenacyt,
            String schema, String element, String qualifier) {
        if (infoFromRenacyt != null) {
            if (!checkCurrentItemWithInformationFromReniec(currentItem, schema, element, qualifier, infoFromRenacyt)) {
                addMetadata(context, currentItem, schema, element, qualifier, infoFromRenacyt);
                return true;
            }
        }
        return false;
    }

    private boolean checkCurrentItemWithInformationFromReniec(
            Item currentItem, String schema, String element, String qualifier, String infoFromRenacyt) {

        if (!checkMetadata(itemService.getMetadata(currentItem, schema, element, qualifier, Item.ANY),
             infoFromRenacyt)) {
            return false;
        }
        return true;
    }

    private boolean checkMetadata(List<MetadataValue> values, String infoFromReniec) {
        for (MetadataValue value : values) {
            if (StringUtils.equals(value.getValue(), infoFromReniec)) {
                return true;
            }
        }
        return false;
    }

    private void addMetadata(Context c, Item currentItem, String schema, String element, String qualifier,
            String infoFromRenacyt) {
        try {
            itemService.addMetadata(c, currentItem, schema, element, qualifier, null, infoFromRenacyt);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
}
