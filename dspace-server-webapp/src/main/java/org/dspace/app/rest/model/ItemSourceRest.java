/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;
import java.util.List;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.source.Source;

/**
 * The ItemSource REST Resource
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSourceRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestAddressableModel.CORE;
    public static final String NAME = "itemsource";

    private List<Source> sources;

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

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

}