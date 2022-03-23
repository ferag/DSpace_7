/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Arrays;
import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Extension of {@link VirtualFieldAuthority}. It returns the actual id stored in authority for a controlled vocabulary driven
 * field, if present, without the id of the vocabulary which is stored as part of authority.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldControlledVocabularyAuthority extends VirtualFieldAuthority {
    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        return Arrays.stream(super.getMetadata(context, item, fieldName))
            .map(this::removePrefix)
            .toArray(String[]::new);
    }

    private String removePrefix(String value) {
        if (Objects.isNull(value)) {
            return "";
        }
        return value.contains(":") ? value.substring(value.indexOf(":") + 1) : value;
    }
}
