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

/**
 * Model the difference between an item and its correction.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemCorrection {

    private final List<BitstreamCorrection> bitstreamCorrections;
    private final List<MetadataCorrection> metadataCorrections;

    public ItemCorrection(List<BitstreamCorrection> bitstream, List<MetadataCorrection> metadata) {
        this.bitstreamCorrections = bitstream;
        this.metadataCorrections = metadata;
    }

    public ItemCorrection() {
        bitstreamCorrections = new ArrayList<>();
        metadataCorrections = new ArrayList<>();
    }

    public void addBitStreamCorrection(BitstreamCorrection correction) {
        bitstreamCorrections.add(correction);
    }

    public void addMetadataCorrection(MetadataCorrection correction) {
        metadataCorrections.add(correction);
    }

    public List<BitstreamCorrection> getBitstreamCorrections() {
        return bitstreamCorrections;
    }

    public List<MetadataCorrection> getMetadataCorrections() {
        return metadataCorrections;
    }

}
