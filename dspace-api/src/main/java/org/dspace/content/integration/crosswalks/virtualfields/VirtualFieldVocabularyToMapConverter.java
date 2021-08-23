/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.MapConverters;
import org.dspace.util.SimpleMapConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends of {@link VirtualFieldVocabulary} that elaborate a controlled-vocabulary
 * originated metadata to take a specific section of its value and convert using specific dictionary.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldVocabularyToMapConverter extends VirtualFieldVocabulary {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldVocabularyToMapConverter.class);

    private final MapConverters mapConverters;

    public VirtualFieldVocabularyToMapConverter(ItemService itemService, MapConverters mapConverters) {
        super(itemService);
        this.mapConverters = mapConverters;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\_");

        if (virtualFieldName.length != 2) {
            LOGGER.warn("Invalid vocabulary virtual field: " + fieldName);
            return new String[] {};
        }

        String[] values = super.getMetadata(context, item, virtualFieldName[0]);
        if (values.length == 0) {
            return values;
        }
        SimpleMapConverter mapConverter = mapConverters.getConverter(virtualFieldName[1])
                .orElseThrow(() -> new IllegalArgumentException("No MapConverter found for field name: " + fieldName));
        return new String[] {mapConverter.getValue(values[0])};
    }

}
