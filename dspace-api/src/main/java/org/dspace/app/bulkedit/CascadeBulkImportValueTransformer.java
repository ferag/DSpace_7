/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 *
 * Implementation of {@link BulkImportValueTransformer} which attempts to apply many
 * transformations to the original value.
 *
 * Transformations are executed in sequence, the value returned is the one returned by the first
 * transformer which succeeds.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CascadeBulkImportValueTransformer implements BulkImportValueTransformer {

    private final List<BulkImportValueTransformer> transformerList;

    public CascadeBulkImportValueTransformer(
        List<BulkImportValueTransformer> transformerList) {
        this.transformerList = transformerList;
    }

    @Override
    public MetadataValueVO transform(Context context, MetadataValueVO metadataValue) {
        for (BulkImportValueTransformer transformer : transformerList) {
            MetadataValueVO result = transformer.transform(context, metadataValue);
            if (!StringUtils.equals(result.getValue(), metadataValue.getValue())) {
                return result;
            }
        }
        return metadataValue;
    }
}
