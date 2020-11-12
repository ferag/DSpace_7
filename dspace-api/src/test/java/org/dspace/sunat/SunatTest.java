/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sunat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractDSpaceIntegrationTest;
import org.dspace.perucris.externalservices.sunat.SunatDTO;
import org.dspace.perucris.externalservices.sunat.SunatProvider;
import org.dspace.perucris.externalservices.sunat.SunatRestConnector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SunatTest {

    @InjectMocks
    private SunatProvider sunatProvider;

    @Mock
    private SunatRestConnector sunatRestConnector;

    protected static Properties testProps;

    @Test
    public void getFildsFromSunatMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());
        String path = testProps.get("test.sunatExampleXML").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String sunatXML = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(sunatXML);

            String RUC = "20172627421";
            when(sunatRestConnector.get(RUC)).thenReturn(inputStream);

            SunatDTO sunatDTO = sunatProvider.getSunatObject(RUC);
            assertEquals("20172627421", sunatDTO.getRuc());
            assertEquals("UNIVERSIDAD DE PIURA", sunatDTO.getLegalName());
            assertEquals("80309", sunatDTO.getCiiu());
            assertEquals("RAMON MUGICA,SAN EDUARDO,131,-,SECTOR EL CHIPE,PIURA,PIURA,PIURA",
                         sunatDTO.getAddressLocality());

        }
    }

    @Test
    public void getFildsFromSunatEmptyXMLMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());
        String path = testProps.get("test.sunatEmptyExampleXML").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String sunatEmptyXML = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(sunatEmptyXML);

            when(sunatRestConnector.get(Mockito.anyString())).thenReturn(inputStream);

            SunatDTO sunatDTO = sunatProvider.getSunatObject("88888888888");
            assertNull(sunatDTO.getCiiu());
            assertNull(sunatDTO.getLegalName());
            assertNull(sunatDTO.getRuc());
            assertNull(sunatDTO.getUbigeoSunat());
            assertEquals("-,-,-,-,-,-,-,-", sunatDTO.getAddressLocality());
        }
    }

    @Test
    public void getFildsFromSunatWrongIdMockitoTest() throws Exception {
        String DNI = "wrongDNI";
        SunatDTO sunatDTO = sunatProvider.getSunatObject(DNI);
        assertNull(sunatDTO);
    }
}