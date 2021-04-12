/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.record.Person;

public class OrcidV3AuthorDataProviderTest {

    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    private XMLtoBio converter = new XMLtoBio();

    private Person orcidPerson;

    /**
     * orcid-person-record.xml has been populated with the official samples taken from
     * https://info.orcid.org/documentation/integration-guide/orcid-record
     */
    @Before
    public void init() {
        this.orcidV3AuthorDataProvider = new OrcidV3AuthorDataProvider();
        this.orcidV3AuthorDataProvider.setOrcidUrl("http://orcid-url/");
        this.orcidPerson =
                converter.convertSinglePerson(getClass().getResourceAsStream("orcid-person-record.xml"));
    }

    @Test
    public void orcidMetadataProvider_shouldProvideAFixedNumberOfMetadata() {

        ExternalDataObject obj = this.orcidV3AuthorDataProvider.convertToExternalDataObject(this.orcidPerson);

        assertEquals(obj.getMetadata().size(), 6);
    }

    @Test
    public void orcidMetadataProvider_shouldProvidePersonNamesMetadata() {

        ExternalDataObject obj = this.orcidV3AuthorDataProvider.convertToExternalDataObject(this.orcidPerson);

        assertTrue(findMetadataValue(obj.getMetadata(), "person", "familyName", null, "Garcia").isPresent());
        assertTrue(findMetadataValue(obj.getMetadata(), "person", "givenName", null, "Sofia").isPresent());
    }

    @Test
    public void orcidMetadataProvider_shouldProvidePersonEmailMetadata() {

        ExternalDataObject obj = this.orcidV3AuthorDataProvider.convertToExternalDataObject(this.orcidPerson);

        assertTrue(findMetadataValue(obj.getMetadata(), "person", "email", null, "sofiag@gmail.com").isPresent());
    }

    @Test
    public void orcidMetadataProvider_shouldProvideDescriptionMetadata() {

        ExternalDataObject obj = this.orcidV3AuthorDataProvider.convertToExternalDataObject(this.orcidPerson);

        assertTrue(findMetadataValue(obj.getMetadata(), "dc", "description", null,
                "Sofia Maria Hernandez Garcia is the researcher that is used as an example ORCID record holder.")
                .isPresent());
    }

    @Test
    public void orcidMetadataProvider_shouldProvideIdentifierMetadata() {

        ExternalDataObject obj = this.orcidV3AuthorDataProvider.convertToExternalDataObject(this.orcidPerson);

        assertTrue(findMetadataValue(obj.getMetadata(),
                "person", "identifier", "orcid", "0000-0002-9029-1854").isPresent());

        assertTrue(findMetadataValue(obj.getMetadata(),
                "dc", "identifier", "uri", "http://orcid-url/0000-0002-9029-1854").isPresent());
    }


    private Optional<MetadataValueDTO> findMetadataValue(List<MetadataValueDTO> metadata,
            String schema, String element, String qualifier, String value) {
        return metadata.stream().filter(md -> {
            return md.getSchema().equals(schema)
                    && md.getElement().equals(element)
                    && (StringUtils.equals(md.getQualifier(), qualifier))
                    && md.getValue().equals(value);
        }).findFirst();
    }

}
