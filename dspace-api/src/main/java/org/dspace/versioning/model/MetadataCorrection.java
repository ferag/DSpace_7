/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.dto.MetadataValueDTO;

/**
 * Model an item metadata value correction.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class MetadataCorrection {

    private final String metadataField;

    private final CorrectionType correctionType;

    private final List<MetadataValueDTO> newValues;

    public static MetadataCorrection metadataAddition(String metadataField, List<MetadataValueDTO> newValues) {
        return new MetadataCorrection(metadataField, CorrectionType.ADD, newValues);
    }

    public static MetadataCorrection metadataModification(String metadataField, List<MetadataValueDTO> newValues) {
        return new MetadataCorrection(metadataField, CorrectionType.MODIFY, newValues);
    }

    public static MetadataCorrection metadataRemoval(String metadataField) {
        return new MetadataCorrection(metadataField, CorrectionType.REMOVE, new ArrayList<MetadataValueDTO>());
    }

    public MetadataCorrection(String metadataField, CorrectionType correctionType, List<MetadataValueDTO> newValues) {
        super();
        this.metadataField = metadataField;
        this.correctionType = correctionType;
        this.newValues = newValues;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public CorrectionType getCorrectionType() {
        return correctionType;
    }

    public List<MetadataValueDTO> getNewValues() {
        return newValues;
    }

}
