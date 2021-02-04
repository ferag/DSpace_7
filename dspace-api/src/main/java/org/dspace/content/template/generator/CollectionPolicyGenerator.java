/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.sql.SQLException;
import java.util.function.Function;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link TemplateValueGenerator} that generates a value starting from
 * what defined in target owning collection resource policy.
 * <p>
 * Syntax is: ###RESPOLICY.[:action].[eperson|epersongroup]###, so for example
 * ###RESPOLICY.default_item_read.epersongroup### will set metadata with value of epersongroup
 * having default_item_read grants on target item owning collection (if any).
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CollectionPolicyGenerator implements TemplateValueGenerator {

    private static final Logger log = LoggerFactory.getLogger(CollectionPolicyGenerator.class);

    public CollectionPolicyGenerator() {
    }


    @Override
    public String generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        try {
            return findValue(context, templateItem.getTemplateItemOf(), extraParams.split("\\."));
        } catch (Exception e) {
            log.error("Error while evaluating resource policies for collection {}: {}",
                templateItem.getTemplateItemOf().getID(), e.getMessage(), e);
            return "";
        }
    }

    private String findValue(Context context, Collection owningCollection,
                             String[] params) throws SQLException {
        int action = Constants.getActionID(params[0].toUpperCase());
        Function<ResourcePolicy, String> mapper = mapper(params[1]);
        return owningCollection.getResourcePolicies()
            .stream()
            .filter(rp -> rp.getAction() == action)
            .findFirst()
            .map(mapper)
            .orElse("");
    }

    private Function<ResourcePolicy, String> mapper(String param) {
        switch (param.toUpperCase()) {
            case "EPERSON":
                return resourcePolicy -> resourcePolicy.getEPerson().getName();
            case "EPERSONGROUP":
                return resourcePolicy -> resourcePolicy.getGroup().getName();
            default:
                throw new IllegalArgumentException("Unable to find mapper for : " + param);
        }
    }
}
