/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sunedu;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.model.SuneduDTO;
import org.dspace.external.provider.impl.OrcidV2AuthorDataProvider;
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
public class SuneduProvider {

    private static Logger log = LogManager.getLogger(OrcidV2AuthorDataProvider.class);

    public static final String SUNEDU_ID_SYNTAX = "\\d{8}";

    @Autowired
    private SuneduRestConnector suneduRestConnector;

    public SuneduDTO getSundeduObject(String id) {
        InputStream xmlSunedu = getRecords(id);
        SuneduDTO result = convertToSuneduDTO(xmlSunedu);
        return result;
    }

    private InputStream getRecords(String id) {
        if (!isValid(id)) {
            return null;
        }
        InputStream bioDocument = suneduRestConnector.get(id);
        return bioDocument;
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(SUNEDU_ID_SYNTAX);
    }

    private SuneduDTO convertToSuneduDTO(InputStream inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        SuneduDTO suneduObject = new SuneduDTO();
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

        suneduObject = fillSuneduDTO(suneduObject, nroDocumento, abreviaturaTituloRecord,
                                     professionalQualificationRecord, universityRecord, countryRecord);
        return suneduObject;
    }

    private SuneduDTO fillSuneduDTO(SuneduDTO suneduObject, NodeList nroDocumento, NodeList abreviaturaTituloRecord,
                  NodeList professionalQualificationRecord, NodeList universityRecord, NodeList countryRecord) {

        SuneduDTO dto = suneduObject;
        String dni = nroDocumento.item(0).getTextContent();
        String university = universityRecord.item(0).getTextContent();
        String country = countryRecord.item(0).getTextContent();

        dto.setId(dni);
        Map<String, List<String>> titulo2Qualification = dto.getEducationDegree();
        for (int i = 0; i < abreviaturaTituloRecord.getLength(); i++) {
            String abreviaturaTitulo = educationDegree(abreviaturaTituloRecord.item(i).getTextContent());
            String professionalQualification = professionalQualificationRecord.item(i).getTextContent();
            if (!StringUtils.isBlank(abreviaturaTitulo) && !StringUtils.isBlank(professionalQualification)) {
                List<String> qualificationsByTitulo = titulo2Qualification.get(abreviaturaTitulo);
                if (qualificationsByTitulo == null) {
                    List<String> qualifications = new ArrayList<String>();
                    qualifications.add(professionalQualification);
                    titulo2Qualification.put(abreviaturaTitulo, qualifications);
                }
            }
        }

        if (!StringUtils.isBlank(university)) {
            dto.setUniversity(university);
        }
        if (!StringUtils.isBlank(country)) {
            dto.setCountry(country);
        }
        return dto;
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

}