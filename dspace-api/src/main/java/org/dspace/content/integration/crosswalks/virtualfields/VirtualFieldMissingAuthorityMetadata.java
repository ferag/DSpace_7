/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} returning a metadata value
 * only if field it is referred to does not have an authority.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldMissingAuthorityMetadata implements VirtualField {

    @Autowired
    private ItemService itemService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String metadataField = fieldName.split("\\.")[2].replaceAll("-", ".");

        return itemService.getMetadataByMetadataString(item, metadataField)
            .stream()
            .filter(md -> StringUtils.isBlank(md.getAuthority()))
            .map(MetadataValue::getValue)
            .toArray(String[]::new);

    }
}
