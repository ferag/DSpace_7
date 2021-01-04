/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.annotation.Resource;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.util.SimpleMapConverter;
import org.junit.Test;

/**
 * Test that checks correct behaviour of configured {@link SimpleMapConverter} in file
 * "${dspace.dir}/config/crosswalks/mapConverter-aliciaCoarTypes.properties
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class AliciaImportMappingTest extends AbstractControllerIntegrationTest {

    @Resource(name = "aliciaTypeVersionMapConverter")
    private SimpleMapConverter aliciaTypeVersionMapConverter;

    @Test
    public void testConversion() {
        assertThat(converted("info:eu-repo/semantics/draft"), is("AO"));
        assertThat(converted("info:eu-repo/semantics/submittedVersion"), is("SMUR"));
        assertThat(converted("info:eu-repo/semantics/acceptedVersion"), is("AM"));
        assertThat(converted("info:eu-repo/semantics/publishedVersion"), is("VoR"));
        assertThat(converted("info:eu-repo/semantics/updatedVersion"), is("EVoR"));
        assertThat(converted("NOT-MAPPED"), is("NA"));
    }

    private String converted(String key) {
        return aliciaTypeVersionMapConverter.getValue(key);
    }
}
