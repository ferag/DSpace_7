/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Locale;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} returning metadata value by looking
 * for its value in the controlled list specified as part of field name.
 *
 * field name syntax is: virtual.valuepair.list_name.metadata_field, i.e.
 * virtual.valuepair.common_iso_countries.crisrp-country
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */

public class VirtualFieldValuePair implements VirtualField {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;
    @Autowired
    private ItemService itemService;

    private String defaultLanguage = Locale.ENGLISH.getLanguage();

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] split = fieldName.split("\\.");

        String listName = split[2];
        String metadataString = split[3].replaceAll("-", ".");

        ChoiceAuthority choiceAuthority =
            choiceAuthorityService.getChoiceAuthorityByAuthorityName(listName);

        String language = Optional.ofNullable(this.defaultLanguage)
            .orElseGet(() -> context.getCurrentLocale().getLanguage());
        return itemService.getMetadataByMetadataString(item,
            metadataString)
            .stream()
            .map(md -> {
                return bestMatch(choiceAuthority, language, md);
            })
            .toArray(String[]::new);
    }

    private String bestMatch(ChoiceAuthority choiceAuthority, String language, org.dspace.content.MetadataValue md) {
        Choices bestMatch = choiceAuthority.getBestMatch(
            md.getValue(),
            language);
        return bestMatch.values.length > 0 ? bestMatch.values[0].label : md.getValue();
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
