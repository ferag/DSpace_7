/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link VirtualField} allowing, given the source item having a list of
 * values for field passed in input, returning a sub-list of those values
 *
 * virtual.valuesSublist.positional_parameters.metadataFieldName
 *
 * positional parameters can be:
 * - "N-M" where N is the index (inclusive) of first value to
 *         be returned and M is the index (exclusive) of last position,
 *         aka he first index not to be returned;
 * - "N-" where N is the index (inclusive) of first metadata to be returned, full list starting from this index
 *        will be returned
 *
 *  i.e. virtual.valuesSublist.0-5.dc-title will return values from 0 to 5 (or less if list has less than 5 elements)
 *  of dc.title metadata.
 *
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldMetadataValueSublist implements VirtualField {

    @Autowired
    private ItemService itemService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] split = fieldName.split("\\.");
        String metadataString = split[3].replaceAll("-", ".");

        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataString);

        String[] positions = split[2].split("-");
        int from = Integer.parseInt(positions[0]);

        if (from >= metadataValues.size()) {
            return new String[0];
        }

        Optional<Integer> to = positions.length < 2 || StringUtils.isBlank(positions[1]) ? Optional.empty()
            : Optional.of(Integer.parseInt(positions[1]));

        return sublist(metadataValues, from,
            to);

    }

    private String[] sublist(List<MetadataValue> metadataValues, Integer from, Optional<Integer> to) {
        Integer upperLimit = Integer.min(metadataValues.size(),
            to.orElse(Integer.MAX_VALUE));

        return metadataValues.subList(from, upperLimit)
            .stream()
            .map(MetadataValue::getValue)
            .toArray(String[]::new);
    }
}
