/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.dspace.utils.DSpace;
/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ItemAuthorityUtils {

    private static DSpace dspace = new DSpace();
    private ItemAuthorityUtils() {}

    public static Map<String, String> buildExtra(String authorityName, SolrDocument item) {
        Map<String, String> extras = new HashMap<String, String>();
        List<ItemAuthorityExtraMetadataGenerator> generators = dspace.getServiceManager()
                .getServicesByType(ItemAuthorityExtraMetadataGenerator.class);
        if (generators != null) {
            for (ItemAuthorityExtraMetadataGenerator gg : generators) {
                Map<String, String> extrasTmp = gg.build(authorityName, item);
                extras.putAll(extrasTmp);
            }
        }
        return extras;
    }

    public static List<Choice> buildAggregateByExtra(String authorityName, SolrDocument item) {
        Map<String, Choice> choiceMap = new HashMap<>();
        List<ItemAuthorityExtraMetadataGenerator> generators = dspace.getServiceManager()
                .getServicesByType(ItemAuthorityExtraMetadataGenerator.class);
        if (generators != null) {
            for (ItemAuthorityExtraMetadataGenerator gg : generators) {
                List<Choice> choices = gg.buildAggregate(authorityName, item);
                choices.forEach(c -> {
                    choiceMap.putIfAbsent(key(c), c);
                });
            }
        }
        return new ArrayList<>(choiceMap.values());
    }

    private static String key(Choice choice) {
        String extras = choice.extras.entrySet().stream().sorted()
            .map(e -> e.getKey() + "|" + e.getValue())
            .collect(Collectors.joining("-"));
        return String.join("::", Arrays.asList(choice.authority,
            choice.label, choice.value,
            extras));
    }

}