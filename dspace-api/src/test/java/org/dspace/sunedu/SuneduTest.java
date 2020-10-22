/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sunedu;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractDSpaceIntegrationTest;
import org.dspace.external.model.SuneduDTO;
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
public class SuneduTest {

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
        String path = testProps.get("test.suneduExampleXML").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String suneduXML = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(suneduXML);

            String DNI = "41918979";
            when(suneduRestConnector.get(DNI)).thenReturn(inputStream);

            List<SuneduDTO> suneduObject = suneduProvider.getSundeduObject(DNI);
            assertEquals(1, suneduObject.size());
            assertEquals("PERU", suneduObject.get(0).getCountry());
            assertEquals("UNIVERSIDAD NACIONAL DE INGENIERÍA", suneduObject.get(0).getUniversity());
            assertEquals("Titulo profesional", suneduObject.get(0).getAbreviaturaTitulo());
            assertEquals("INGENIERO DE SISTEMAS", suneduObject.get(0).getProfessionalQualification());
        }
    }

    @Test
    public void getFildsFromSuneduWithTwoEducationDegreeMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());
        String path = testProps.get("test.suneduDoubleDegreeExampleXML").toString();
        try (FileInputStream file = new FileInputStream(path)) {

            String seneduExampleWithDoubleDegree = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(seneduExampleWithDoubleDegree);

            String DNI = "41918999";
            when(suneduRestConnector.get(DNI)).thenReturn(inputStream);

            List<SuneduDTO> suneduObjects = suneduProvider.getSundeduObject(DNI);
            assertEquals(2, suneduObjects.size());
            for (SuneduDTO dto : suneduObjects) {
                if (dto.getAbreviaturaTitulo().equals("Titulo profesional")) {
                    assertEquals("PERU", dto.getCountry());
                    assertEquals("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.", dto.getUniversity());
                    assertEquals("Titulo profesional", dto.getAbreviaturaTitulo());
                    assertEquals("TITULO PROFESIONAL DE LICENCIADO EN ADMINISTRACION",
                            dto.getProfessionalQualification());
                } else {
                    assertEquals("PERU", dto.getCountry());
                    assertEquals("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.", dto.getUniversity());
                    assertEquals("Bachiller", dto.getAbreviaturaTitulo());
                    assertEquals("BACHILLER EN ADMINISTRACION", dto.getProfessionalQualification());
                }
            }
        }
    }

}