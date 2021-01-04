/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.Objects;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.versioning.model.CorrectionType;
import org.dspace.versioning.model.MetadataCorrection;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a MetadataCorrection
 * by all its attributes.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class MetadataCorrectionMatcher extends TypeSafeMatcher<MetadataCorrection> {

    private final String metadataField;

    private final CorrectionType correctionType;

    private final List<MetadataValueDTO> newValues;

    private MetadataCorrectionMatcher(String metadataField, CorrectionType correctionType,
        List<MetadataValueDTO> newValues) {
        super();
        this.metadataField = metadataField;
        this.correctionType = correctionType;
        this.newValues = newValues;
    }

    public static MetadataCorrectionMatcher additionOf(String metadataField, List<MetadataValueDTO> newValues) {
        return new MetadataCorrectionMatcher(metadataField, CorrectionType.ADD, newValues);
    }

    public static MetadataCorrectionMatcher editOf(String metadataField, List<MetadataValueDTO> newValues) {
        return new MetadataCorrectionMatcher(metadataField, CorrectionType.MODIFY, newValues);
    }

    public static MetadataCorrectionMatcher removalOf(String metadataField) {
        return new MetadataCorrectionMatcher(metadataField, CorrectionType.REMOVE, new ArrayList<>());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("MetadataCorrection with the following attributes [metadataField=" + metadataField
            + ", correctionType=" + correctionType + ", newValues=" + newValues + "]");
    }

    @Override
    protected boolean matchesSafely(MetadataCorrection item) {
        return item.getCorrectionType() == correctionType && Objects.areEqual(item.getMetadataField(), metadataField)
            && Objects.areEqual(item.getNewValues(), newValues);
    }

}
