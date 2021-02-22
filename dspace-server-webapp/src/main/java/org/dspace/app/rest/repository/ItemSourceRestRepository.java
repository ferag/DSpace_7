/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ItemSourceRest;
import org.dspace.app.source.ItemSource;
import org.dspace.app.source.Source;
import org.dspace.app.source.service.ItemSourceService;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage ItemSource Rest object
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(ItemSourceRest.CATEGORY + "." + ItemSourceRest.NAME)
public class ItemSourceRestRepository extends DSpaceRestRepository<ItemSourceRest, UUID> {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ItemSourceService itemSourceService;

    @Override
    @PreAuthorize("permitAll()")
    public ItemSourceRest findOne(Context context, UUID uuid) {
        ItemSource itemSource = new ItemSource();
        Item item = null;
        try {
            item = itemService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            return null;
        }
        try {
            itemSource.setItemUuid(uuid);
            List<RelationshipType> relationshipTypes = new ArrayList<RelationshipType>();
            relationshipTypes.addAll(relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                                     ConcytecWorkflowService.IS_ORIGINATED_FROM_IN_RELATIONSHIP));
            relationshipTypes.addAll(relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                                     ConcytecWorkflowService.IS_SHADOW_COPY_RELATIONSHIP));
            for (RelationshipType relationshipType : relationshipTypes) {
                List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context, item,
                        relationshipType);
                for (Relationship relationship : relationships) {
                    Source source = new Source();
                    Item right = relationship.getRightItem();
                    source.setRelationshipType(relationshipType.getLeftwardType());
                    source.setSource(right.getOwningCollection().getCommunities().get(0).getName());
                    source.setItemUuid(right.getID());
                    List<String> metadata = itemSourceService.getMatchedMetadata(item, right);
                    source.setMetadata(metadata);
                    itemSource.addSource(source);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(itemSource, utils.obtainProjection());
    }



    @Override
    public Page<ItemSourceRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(ItemSourceRest.NAME, "findAll");
    }

    @Override
    public Class<ItemSourceRest> getDomainClass() {
        return ItemSourceRest.class;
    }

}