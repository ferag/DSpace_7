/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.profile.CvEntity;
import org.dspace.app.rest.model.CvEntityRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming an model that represent a
 * CvEntity to the REST representation of an CvEntity.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class CvEntityConverter implements DSpaceConverter<CvEntity, CvEntityRest> {

    @Override
    public CvEntityRest convert(CvEntity modelObject, Projection projection) {
        CvEntityRest rest = new CvEntityRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getId());
        return rest;
    }

    @Override
    public Class<CvEntity> getModelClass() {
        return CvEntity.class;
    }

}
