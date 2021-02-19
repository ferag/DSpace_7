/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.source.service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSourceServiceImpl implements ItemSourceService {

    @Autowired
    private ItemService itemSrvice;

    @Override
    public List<String> getMatchedMetadata(Item item1, Item item2) {
        Set<MetadataField> fields = new HashSet<>();
        item1.getMetadata().stream().forEach(metadata -> {
            fields.add(metadata.getMetadataField());
        });

        item2.getMetadata().stream().forEach(metadata -> {
            fields.add(metadata.getMetadataField());
        });

        List<String> results = new ArrayList<String>();

        fields.stream().forEach(field -> {
            List<String> fieldResult = computeSourceMetatada(field,
                    itemSrvice.getMetadata(item1, field.getMetadataSchema().getName(), field.getElement(),
                            field.getQualifier(), null),
                    itemSrvice.getMetadata(item2, field.getMetadataSchema().getName(), field.getElement(),
                            field.getQualifier(), null));
            results.addAll(fieldResult);
        });
        return results;
    }

    private List<String> computeSourceMetatada(MetadataField field, List<MetadataValue> metadataValues1,
            List<MetadataValue> metadataValues2) {
        List<String> results = new ArrayList<String>();
        if (!metadataValues1.isEmpty() && !metadataValues2.isEmpty()) {
            for (int i = 0; i < metadataValues1.size(); i++) {
                for (MetadataValue mv2 : metadataValues2) {
                    if (StringUtils.equals(metadataValues1.get(i).getValue(), mv2.getValue())) {
                        results.add(field.toString() + "/" + i);
                    }
                }
            }
            if ((metadataValues1.size() == metadataValues2.size()) && (metadataValues1.size() == results.size())) {
                return Collections.singletonList(field.toString());
            }
        }
        return results;
    }

}