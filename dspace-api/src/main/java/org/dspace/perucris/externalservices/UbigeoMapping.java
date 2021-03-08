/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;

import java.util.Map;
import java.util.Optional;

import org.dspace.content.vo.MetadataValueVO;
import org.dspace.util.SimpleMapConverter;

/**
 * Service that convert Ubigeo codes provided by some Peruvian services
 * into their standard INEI
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class UbigeoMapping {

    private final Map<String, SimpleMapConverter> converters;

    public UbigeoMapping(Map<String, SimpleMapConverter> converters) {
        this.converters = converters;
    }

    public MetadataValueVO convert(String service, String code) {
        return Optional.ofNullable(converters.get(service))
            .map(c -> c.getValue(code))
            .map(this::toMetadataVo)
            .orElseThrow(() -> new IllegalArgumentException("No converter provided for service " + service));
    }

    private MetadataValueVO toMetadataVo(String value) {
        String[] split = value.split("::");
        return new MetadataValueVO(split[1], split[0], 600);
    }
}
