/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.integration.crosswalks.ReferCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexConverter {

    private static final Logger log = LogManager.getLogger(ElasticsearchIndexConverter.class);

    private final Map<String,ReferCrosswalk> entity2ReferCrosswalk;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ElasticsearchIndexQueueService elasticsearchIndexService;

    public ElasticsearchIndexConverter(Map<String,ReferCrosswalk> entity2ReferCrosswalk) {
        this.entity2ReferCrosswalk = entity2ReferCrosswalk;
    }

    public String convert(Context context, ElasticsearchIndexQueue record) throws SQLException {
        Item item = itemService.find(context, record.getId());
        if (Objects.nonNull(item)) {
            if (elasticsearchIndexService.isSupportedEntityType(item)) {
                String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
                ReferCrosswalk referCrosswalk = entity2ReferCrosswalk.get(entityType);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    referCrosswalk.disseminate(context, item, out);
                    return out.toString();
                } catch (CrosswalkException | IOException | AuthorizeException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return StringUtils.EMPTY;
    }

}