/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;
import org.dspace.app.rest.model.ItemSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.source.ItemSource;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the ItemSource in the DSpace API data model and
 * the REST data model
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class ItemSourceConverter implements DSpaceConverter<ItemSource, ItemSourceRest> {

    @Override
    public ItemSourceRest convert(ItemSource modelObject, Projection projection) {
        ItemSourceRest model = new ItemSourceRest();
        model.setProjection(projection);

        model.setId(modelObject.getItemUuid().toString());
        model.setSources(modelObject.getSources());

        return model;
    }

    @Override
    public Class<ItemSource> getModelClass() {
        return ItemSource.class;
    }

}