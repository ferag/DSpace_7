/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.reniec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractDSpaceIntegrationTest;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.dspace.perucris.externalservices.reniec.ReniecRestConnector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReniecProviderTest {

    @InjectMocks
    private ReniecProvider reniecProvider;

    @Mock
    private ReniecRestConnector reniecRestConnector;

    protected static Properties testProps;

    @Test
    public void getFildsFromReniecMockitoTest() throws Exception {
        testProps = new Properties();
        URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader().getResource("test-config.properties");
        testProps.load(properties.openStream());
        String path = testProps.get("test.reniecExampleXML").toString();
        try (FileInputStream file = new FileInputStream(path)) {
            String reniecXML = IOUtils.toString(file, Charset.defaultCharset());
            InputStream inputStream = new StringInputStream(reniecXML);

            String DNI = "41918979";
            when(reniecRestConnector.get(DNI)).thenReturn(inputStream);

            ReniecDTO suneduObject = reniecProvider.getReniecObject(DNI);
            assertEquals("SAUCEDO", suneduObject.getFatherLastName());
            assertEquals("ASCONA", suneduObject.getMaternalLastName());
            assertNull(suneduObject.getLastNameMarried());
            assertEquals("EDWIN MANUEL", suneduObject.getNames());
            assertEquals("14-01-37", suneduObject.getHomeCode());
            assertEquals("LIMA", suneduObject.getRegionOfResidence());
            assertEquals("LIMA", suneduObject.getProvinceOfResidence());
            assertEquals("SAN JUAN DE LURIGANCHO", suneduObject.getDistrictOfResidence());
            assertEquals("MZ.7 LT.8 ASENT.H.19 DE ABRIL CANTO GRANDE", suneduObject.getHomeAddress());
            assertEquals(1, suneduObject.getIndexSex());
            assertEquals("14-01-37", suneduObject.getNacimientoCode());
            assertEquals("LIMA", suneduObject.getRegionOfBirth());
            assertEquals("LIMA", suneduObject.getProvinceOfBirth());
            assertEquals("SAN JUAN DE LURIGANCHO", suneduObject.getDistrictOfBirth());
            LocalDate birthDate = LocalDate.of(1983, 8, 14);
            assertEquals(birthDate, suneduObject.getBirthDate());
        }
    }

    @Test
    public void getFildsFromReniecWrongDNIMockitoTest() throws Exception {
        String DNI = "wrongDNI";
        ReniecDTO suneduObject = reniecProvider.getReniecObject(DNI);
        assertNull(suneduObject);
    }
}
