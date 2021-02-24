/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;
import org.dspace.app.rest.model.ItemSourceRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * ItemSource Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@RelNameDSpaceResource(ItemSourceRest.NAME)
public class ItemSourceResource extends DSpaceResource<ItemSourceRest> {

    public ItemSourceResource(ItemSourceRest data, Utils utils) {
        super(data, utils);
    }

}