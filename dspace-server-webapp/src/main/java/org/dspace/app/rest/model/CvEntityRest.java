/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import org.dspace.app.rest.RestResourceController;

/**
 * The Researcher Profile related entity REST resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@LinksRest(links = {
    @LinkRest(name = CvEntityRest.ITEM, method = "getItem"),
})
public class CvEntityRest extends BaseObjectRest<UUID> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.CRIS;
    public static final String NAME = "cventity";

    public static final String ITEM = "item";

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

}
