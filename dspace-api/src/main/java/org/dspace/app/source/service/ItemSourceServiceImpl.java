/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.source.service;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.source.ItemSource;
import org.dspace.app.source.Source;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSourceServiceImpl implements ItemSourceService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public ItemSource getItemSource(Context context, Item item) {
        ItemSource itemSource = new ItemSource();
        try {
            itemSource.setItemUuid(item.getID());
            List<RelationshipType> relationshipTypes = new ArrayList<RelationshipType>();

            relationshipTypes.addAll(relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                    ConcytecWorkflowRelation.ORIGINATED.getLeftType()));

            relationshipTypes.addAll(relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                    ConcytecWorkflowRelation.SHADOW_COPY.getLeftType()));
            Set<MetadataField> denyFields = getDenyFields(context);
            for (RelationshipType relationshipType : relationshipTypes) {
                List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context, item,
                        relationshipType);
                for (Relationship relationship : relationships) {
                    Source source = new Source();
                    boolean leftwardRelation = item.getID().equals(relationship.getLeftItem().getID());
                    Item relatedItem = leftwardRelation ? relationship.getRightItem() : relationship.getLeftItem();
                    String type = leftwardRelation ? relationshipType.getLeftwardType() :
                        relationshipType.getRightwardType();

                    source.setRelationshipType(type);
                    source.setSource(relatedItem.getOwningCollection().getCommunities().get(0).getName());
                    source.setItemUuid(relatedItem.getID());
                    List<String> metadata = getMatchingMetadata(item, relatedItem, denyFields);
                    source.setMetadata(metadata);
                    itemSource.addSource(source);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return itemSource;
    }

    private List<String> getMatchingMetadata(Item item1, Item item2, Set<MetadataField> denyFields)
            throws SQLException {
        Set<MetadataField> fields = new HashSet<>();
        item1.getMetadata().stream().forEach(metadata -> {
            fields.add(metadata.getMetadataField());
        });

        item2.getMetadata().stream().forEach(metadata -> {
            fields.add(metadata.getMetadataField());
        });

        fields.removeAll(denyFields);
        List<String> results = new ArrayList<String>();

        fields.stream().forEach(field -> {
            List<String> fieldResult = computeSourceMetatada(field, getMetadataValue(item1, field),
                                                                    getMetadataValue(item2, field));
            results.addAll(fieldResult);
        });
        return results;
    }

    private List<String> computeSourceMetatada(MetadataField field, List<MetadataValue> metadataValues1,
            List<MetadataValue> metadataValues2) {
        List<String> results = new ArrayList<String>();
        if (metadataValues1.isEmpty() || metadataValues2.isEmpty()) {
            return results;
        }
        for (int i = 0; i < metadataValues1.size(); i++) {
            for (MetadataValue mv2 : metadataValues2) {
                if (StringUtils.equals(metadataValues1.get(i).getValue(), mv2.getValue())) {
                    results.add(field.toString('.') + "/" + i);
                }
            }
        }
        if ((metadataValues1.size() == metadataValues2.size()) && (metadataValues1.size() == results.size())) {
            return Collections.singletonList(field.toString('.'));
        }
        return results;
    }

    private List<MetadataValue> getMetadataValue (Item item, MetadataField field) {
        return itemService.getMetadata(item, field.getMetadataSchema().getName(), field.getElement(),
                field.getQualifier(), null);
    }

    public Set<MetadataField> getDenyFields(Context context) throws SQLException {
        Set<MetadataField> denyFields = new HashSet<MetadataField>();
        denyFields.add(metadataFieldService.findByString(context, "dc.date.accessioned", '.'));
        denyFields.add(metadataFieldService.findByString(context, "dc.date.available", '.'));
        return denyFields;
    }

}