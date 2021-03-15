/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.vo.MetadataValueVO;
import org.dspace.util.SimpleMapConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class UbigeoMappingTest {

    private UbigeoMapping ubigeoMapping;
    private SimpleMapConverter serviceConverter = Mockito.mock(SimpleMapConverter.class);

    @Before
    public void setUp() throws Exception {
        Map<String, SimpleMapConverter> converter = new HashMap<>();
        converter.put("service", serviceConverter);
        ubigeoMapping = new UbigeoMapping(converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serviceNotFound() {
        ubigeoMapping.convert("fake", "code");
    }

    @Test
    public void valueConverted() {

        when(serviceConverter.getValue("key")).thenReturn("0011::keyConverted");

        MetadataValueVO convertedValue = ubigeoMapping.convert("service", "key");

        MetadataValueVO metadataValueVO = new MetadataValueVO("keyConverted", "0011", 600);
        assertThat(convertedValue.getValue(), is(metadataValueVO.getValue()));
        assertThat(convertedValue.getAuthority(), is(metadataValueVO.getAuthority()));
        assertThat(convertedValue.getConfidence(), is(metadataValueVO.getConfidence()));
    }
}