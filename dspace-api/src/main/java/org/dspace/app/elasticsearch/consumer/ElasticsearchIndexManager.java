/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.consumer;
import java.util.Map;
import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The scope of this class is to manage Elasticsearch indices.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexManager {

    private Map<String, String> entityType2Index;

    @Autowired
    private ItemService itemService;

    public ElasticsearchIndexManager(Map<String, String> map) {
        this.entityType2Index = map;
    }

    public boolean isSupportedEntityType(Item item) {
        if (Objects.isNull(item)) {
            return false;
        }
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        return isSupportedEntityType(entityType);
    }

    public boolean isSupportedEntityType(String entityType) {
        return entityType2Index.containsKey(entityType);
    }

    public Map<String, String> getEntityType2Index() {
        return entityType2Index;
    }

    public void setEntityType2Index(Map<String, String> entityType2Index) {
        this.entityType2Index = entityType2Index;
    }

}