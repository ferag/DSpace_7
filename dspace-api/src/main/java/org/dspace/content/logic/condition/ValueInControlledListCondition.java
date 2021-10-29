/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Check whether or not list is in a valid controlled list
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ValueInControlledListCondition extends AbstractCondition {


    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;


    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        String field = (String)getParameters().get("field");
        if (field == null) {
            return false;
        }

        String authorityName = (String) getParameters().get("authorityName");
        if (StringUtils.isBlank(authorityName)) {
            return false;
        }

        List<MetadataValue> values = getMetadataValues(item, field);
        if (values.isEmpty()) {
            return false;
        }
        ChoiceAuthority choiceAuthority =
            choiceAuthorityService.getChoiceAuthorityByAuthorityName(authorityName);
        return values.stream()
            .map(MetadataValue::getValue)
            .allMatch(v -> Optional.ofNullable(choiceAuthority.getBestMatch(v, StringUtils.EMPTY))
            .filter(c -> c.values.length > 0).isPresent());
    }

    private List<MetadataValue> getMetadataValues(Item item, String field) {
        String[] fieldParts = field.split("\\.");
        String schema = (fieldParts.length > 0 ? fieldParts[0] : null);
        String element = (fieldParts.length > 1 ? fieldParts[1] : null);
        String qualifier = (fieldParts.length > 2 ? fieldParts[2] : null);

        List<MetadataValue> values = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
        return values;
    }

}
