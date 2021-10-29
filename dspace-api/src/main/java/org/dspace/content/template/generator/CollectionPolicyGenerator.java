/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
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
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        try {
            return
                Collections.singletonList(
                    findValue(context,
                        templateItem.getTemplateItemOf(), extraParams.split("\\.")));
        } catch (Exception e) {
            log.error("Error while evaluating resource policies for collection {}: {}",
                templateItem.getTemplateItemOf().getID(), e.getMessage(), e);
            return Collections.singletonList(new MetadataValueVO(""));
        }
    }

    private MetadataValueVO findValue(Context context, Collection owningCollection,
                                      String[] params) throws SQLException {
        int action = Constants.getActionID(params[0].toUpperCase());
        Function<ResourcePolicy, MetadataValueVO> mapper = mapper(params[1]);
        return owningCollection.getResourcePolicies()
            .stream()
            .filter(rp -> rp.getAction() == action)
            .findFirst()
            .map(mapper)
            .orElse(new MetadataValueVO(""));
    }

    private Function<ResourcePolicy, MetadataValueVO> mapper(String param) {
        switch (param.toUpperCase()) {
            case "EPERSON":
                return resourcePolicy -> new MetadataValueVO(
                    resourcePolicy.getEPerson().getName(),
                    UUIDUtils.toString(resourcePolicy.getEPerson().getID()));
            case "EPERSONGROUP":
                return resourcePolicy -> new MetadataValueVO(
                    resourcePolicy.getGroup().getName(),
                    UUIDUtils.toString(resourcePolicy.getGroup().getID()));
            default:
                throw new IllegalArgumentException("Unable to find mapper for : " + param);
        }
    }
}
