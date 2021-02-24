/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ItemSourceRest;
import org.dspace.app.source.ItemSource;
import org.dspace.app.source.service.ItemSourceService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
    private ItemSourceService itemSourceService;

    @Override
    @PreAuthorize("permitAll()")
    public ItemSourceRest findOne(Context context, UUID uuid) {
        Item item = null;
        try {
            item = itemService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            return null;
        }
        ItemSource itemSource =  itemSourceService.getItemSource(context, item);
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