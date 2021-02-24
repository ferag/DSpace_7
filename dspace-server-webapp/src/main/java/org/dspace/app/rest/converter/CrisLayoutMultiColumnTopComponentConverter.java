/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *  http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest.Column;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisLayoutMultiColumnTopComponent;
import org.dspace.layout.CrisLayoutSectionComponent;

/**
 * extension of {@link CrisLayoutTopComponentConverter} to convert
 * {@link org.dspace.layout.CrisLayoutMultiColumnTopComponent} to rest.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */

public class CrisLayoutMultiColumnTopComponentConverter extends CrisLayoutTopComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutMultiColumnTopComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest convert(CrisLayoutSectionComponent component) {

        CrisLayoutMultiColumnTopComponent topComponent = (CrisLayoutMultiColumnTopComponent) component;
        CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest result =
            (CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest) super.convert(topComponent);

        List<Column> columnList = new ArrayList<>();

        topComponent.getColumns().forEach(c -> columnList.add(Column.from(c)));
        result.setColumnList(columnList);

        return result;
    }
}
