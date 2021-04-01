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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CvEntityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "item" subresource of an individual cv entity.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(CvEntityRest.CATEGORY + "." + CvEntityRest.NAME + "." + CvEntityRest.ITEM)
public class CvEntityItemLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ItemService itemService;

    /**
     * Returns the item related to the Cv entity with the given UUID.
     *
     * @param request    the http servlet request
     * @param id         the profile UUID
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the item rest representation
     */
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public ItemRest getItem(@Nullable HttpServletRequest request, UUID id,
        @Nullable Pageable pageable, Projection projection) {

        try {

            Context context = obtainContext();

            Item item = itemService.find(context, id);
            if (item == null) {
                throw new ResourceNotFoundException("No such item related to a cv entity with UUID: " + id);
            }

            return converter.toRest(item, projection);

        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }
}
