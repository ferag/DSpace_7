/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractDSpaceIntegrationTest;
import org.dspace.external.model.SuneduDTO;
import org.dspace.sunedu.SuneduProvider;
import org.dspace.sunedu.SuneduRestConnector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SuneduExternalSourcesIT {

    @InjectMocks
    private SuneduProvider suneduProvider;

    @Mock
    private SuneduRestConnector suneduRestConnector;

    protected static Properties testProps;

    @Test
    public void getFildsFromSuneduSingleEducationDegreeMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());

        try (FileInputStream file = new FileInputStream(testProps.get("test.suneduExampleXML").toString())) {
            String suneduXML = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(suneduXML);

            String DNI = "41918979";
            when(suneduRestConnector.get(DNI)).thenReturn(inputStream);

            SuneduDTO suneduObject =  suneduProvider.getSundeduObject(DNI);
            assertEquals(DNI, suneduObject.getId());
            assertEquals("PERU", suneduObject.getCountry());
            assertEquals("UNIVERSIDAD NACIONAL DE INGENIERÍA", suneduObject.getUniversity());

            Set<String> keys = suneduObject.getEducationDegree().keySet();
            assertEquals(1, keys.size());
            assertTrue(keys.contains("Titulo profesional"));
            List<String> x = suneduObject.getEducationDegree().get("Titulo profesional");
            assertEquals(1, x.size());
            assertEquals("INGENIERO DE SISTEMAS", x.get(0));
        }
    }

    @Test
    public void getFildsFromSuneduWithTwoEducationDegreeMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());
        try (FileInputStream file = new FileInputStream(
             testProps.get("test.suneduDoubleDegreeExampleXML").toString())) {

            String seneduExampleWithDoubleDegree = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(seneduExampleWithDoubleDegree);

            String DNI = "41918999";
            when(suneduRestConnector.get(DNI)).thenReturn(inputStream);

            SuneduDTO suneduObject =  suneduProvider.getSundeduObject(DNI);
            assertEquals(DNI, suneduObject.getId());
            assertEquals("PERU", suneduObject.getCountry());
            assertEquals("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.", suneduObject.getUniversity());

            Set<String> keys = suneduObject.getEducationDegree().keySet();
            assertEquals(2, keys.size());
            assertTrue(keys.contains("Titulo profesional"));
            assertTrue(keys.contains("Bachiller"));
            List<String> firstEduDegree = suneduObject.getEducationDegree().get("Titulo profesional");
            assertEquals(1, firstEduDegree.size());
            assertEquals("TITULO PROFESIONAL DE LICENCIADO EN ADMINISTRACION", firstEduDegree.get(0));
            List<String> secondEduDegree = suneduObject.getEducationDegree().get("Bachiller");
            assertEquals(1, secondEduDegree.size());
            assertEquals("BACHILLER EN ADMINISTRACION", secondEduDegree.get(0));
        }
    }

}