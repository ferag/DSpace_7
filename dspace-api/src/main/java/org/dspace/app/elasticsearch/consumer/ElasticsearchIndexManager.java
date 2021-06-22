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
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.template.generator.ElasticsearchIndexTemplateValueGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The scope of this class is to manage Elasticsearch indices.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchIndexManager {

    private Map<String, String> entityType2Index;

    private Map<String, ElasticsearchIndexTemplateValueGenerator> entityType2Generator;

    @Autowired
    private ItemService itemService;

    @PostConstruct
    private void setUp() {
        for (String entityType : this.entityType2Index.keySet()) {
            if (this.entityType2Generator.containsKey(entityType)) {
                String newIndex = entityType2Generator.get(entityType).generator(entityType2Index.get(entityType));
                if (StringUtils.isNotBlank(newIndex)) {
                    this.entityType2Index.put(entityType, newIndex);
                }
            }

        }
    }

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

    public Map<String, ElasticsearchIndexTemplateValueGenerator> getEntityType2Generator() {
        return entityType2Generator;
    }

    public void setEntityType2Generator(Map<String, ElasticsearchIndexTemplateValueGenerator> entityType2Generator) {
        this.entityType2Generator = entityType2Generator;
    }

}