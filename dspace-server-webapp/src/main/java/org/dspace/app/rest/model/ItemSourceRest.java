/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.source.ItemSource;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSourceRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestAddressableModel.CORE;
    public static final String NAME = "itemsource";

    private UUID itemUuid;

    private List<ItemSource> sources;

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public List<ItemSource> getSources() {
        return sources;
    }

    public void setSources(List<ItemSource> sources) {
        this.sources = sources;
    }

}