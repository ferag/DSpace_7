/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.util.Arrays;
import java.util.Collections;

import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CascadeBulkImportValueTransformer}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */

public class CascadeBulkImportValueTransformerTest {

    private Context context = Mockito.mock(Context.class);

    @Test
    public void noTransformersOriginalValueIsReturned() {

        CascadeBulkImportValueTransformer cascadeBulkImportValueTransformer =
            new CascadeBulkImportValueTransformer(Collections.emptyList());

        MetadataValueVO metadataValue = metadataValue("originalValue");

        MetadataValueVO transformed = cascadeBulkImportValueTransformer.transform(context, metadataValue);

        Assert.assertEquals(transformed, metadataValue);
    }

    @Test
    public void firstTransformerReturnsValue() {

        MetadataValueVO firstTransformerResult = metadataValue("firstResult");
        MetadataValueVO secondTransformerResult = metadataValue("secondResult");

        BulkImportValueTransformer firstTransformer = (context, metadataValue) -> firstTransformerResult;
        BulkImportValueTransformer secondTransformer = (context, metadataValue) -> secondTransformerResult;

        CascadeBulkImportValueTransformer cascadeBulkImportValueTransformer =
            new CascadeBulkImportValueTransformer(
                Arrays.asList(
                    firstTransformer,
                    secondTransformer
                )
            );

        MetadataValueVO metadataValue = metadataValue("originalValue");

        MetadataValueVO transformed = cascadeBulkImportValueTransformer.transform(context, metadataValue);

        Assert.assertEquals(transformed, firstTransformerResult);

    }

    @Test
    public void firstTransformerReturnsOriginalValue() {

        MetadataValueVO originalValue = metadataValue("originalValue");
        MetadataValueVO secondTransformerResult = metadataValue("secondResult");

        BulkImportValueTransformer firstTransformer = (context, mv) -> originalValue;
        BulkImportValueTransformer secondTransformer = (context, mv) -> secondTransformerResult;

        CascadeBulkImportValueTransformer cascadeBulkImportValueTransformer =
            new CascadeBulkImportValueTransformer(
                Arrays.asList(
                    firstTransformer,
                    secondTransformer
                )
            );


        MetadataValueVO transformed = cascadeBulkImportValueTransformer.transform(context, originalValue);

        Assert.assertEquals(transformed, secondTransformerResult);

    }

    @Test
    public void allTransformersReturnsOriginalValue() {

        MetadataValueVO originalValue = metadataValue("originalValue");

        BulkImportValueTransformer firstTransformer = (context, mv) -> originalValue;
        BulkImportValueTransformer secondTransformer = (context, mv) -> originalValue;

        CascadeBulkImportValueTransformer cascadeBulkImportValueTransformer =
            new CascadeBulkImportValueTransformer(
                Arrays.asList(
                    firstTransformer,
                    secondTransformer
                )
            );


        MetadataValueVO transformed = cascadeBulkImportValueTransformer.transform(context, originalValue);

        Assert.assertEquals(transformed, originalValue);

    }

    private MetadataValueVO metadataValue(String value) {
        return new MetadataValueVO(value);
    }
}