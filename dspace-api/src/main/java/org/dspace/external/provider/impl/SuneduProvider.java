/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.SuneduRestConnector;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is the implementation of the ExternalDataProvider interface that
 * will deal with the SUNEDU External Data lookup
 * 
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SuneduProvider implements ExternalDataProvider {

    private static Logger log = LogManager.getLogger(OrcidV2AuthorDataProvider.class);

    public static final String SUNEDU_ID_SYNTAX = "\\d{8}";

    @Autowired
    private SuneduRestConnector suneduRestConnector;

    private String sourceIdentifier;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        InputStream xmlSunedu = getRecords(id);
        ExternalDataObject externalDataObject = convertToExternalDataObject(xmlSunedu);
        return Optional.of(externalDataObject);
    }

    public InputStream getRecords(String id) {
        log.debug("getBio called with ID=" + id);
        if (!isValid(id)) {
            return null;
        }
        InputStream bioDocument = suneduRestConnector.get(id);
        return bioDocument;
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(SUNEDU_ID_SYNTAX);
    }

    protected ExternalDataObject convertToExternalDataObject(InputStream inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        NodeList nroDocumento = doc.getElementsByTagName("nroDocumento");
        NodeList abreviaturaTituloRecord = doc.getElementsByTagName("abreviaturaTitulo");
        NodeList professionalQualificationRecord = doc.getElementsByTagName("tituloProfesional");
        NodeList universityRecord = doc.getElementsByTagName("universidad");
        NodeList countryRecord = doc.getElementsByTagName("pais");

        externalDataObject = fillExternalDataObject(externalDataObject, nroDocumento,
                             abreviaturaTituloRecord, professionalQualificationRecord, universityRecord, countryRecord);
        return externalDataObject;
    }

    private ExternalDataObject fillExternalDataObject(ExternalDataObject externalDataObject, NodeList nroDocumento,
            NodeList abreviaturaTituloRecord, NodeList professionalQualificationRecord, NodeList universityRecord,
            NodeList countryRecord) {
        ExternalDataObject edo = externalDataObject;
        String id = nroDocumento.item(0).getTextContent();
        String university = universityRecord.item(0).getTextContent();
        String country = countryRecord.item(0).getTextContent();

        externalDataObject.setId(id);
        for (int i = 0; i < abreviaturaTituloRecord.getLength(); i++) {
            String abreviaturaTitulo = educationDegree(abreviaturaTituloRecord.item(i).getTextContent());
            String  professionalQualification = professionalQualificationRecord.item(i).getTextContent();
            if (!StringUtils.isBlank(abreviaturaTitulo)) {
                externalDataObject.addMetadata(
                                  new MetadataValueDTO("crisrp", "qualificatio", null, null, abreviaturaTitulo));
            }
            if (!StringUtils.isBlank(professionalQualification)) {
                externalDataObject.addMetadata(
                          new MetadataValueDTO("crisrp", "qualificatio", null, null, professionalQualification));
            }
        }

        if (!StringUtils.isBlank(university)) {
            externalDataObject.addMetadata(new MetadataValueDTO("crisrp", "equcation", "grantor", null, university));
        }
        if (!StringUtils.isBlank(country)) {
            externalDataObject.addMetadata(new MetadataValueDTO("crisrp", "equcation", "country", null, country));
        }
        return edo;
    }

    private String educationDegree(String educationDegree) {
        switch (educationDegree) {
            case "B":
                return "Bachiller";
            case "T":
                return "Titulo profesional";
            case "S":
                return "Titulo de segunda especialidad";
            case "M":
                return "Maestro";
            case "D":
                return "Doctor";
            default:
                return null;
        }
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        return Collections.emptyList();
    }

    @Override
    public int getNumberOfResults(String query) {
        return 0;
    }

}