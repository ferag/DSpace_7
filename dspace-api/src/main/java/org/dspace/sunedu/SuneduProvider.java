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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.model.SuneduDTO;
import org.dspace.util.SimpleMapConverterCountry;
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

    private static Logger log = LogManager.getLogger(SuneduProvider.class);

    public static final String SUNEDU_ID_SYNTAX = "\\d{8}";

    @Autowired
    private SuneduRestConnector suneduRestConnector;

    @Autowired
    private SimpleMapConverterCountry simpleMapConverterCountry;

    public List<SuneduDTO> getSundeduObject(String id) {
        InputStream is = getRecords(id);
        if (is != null) {
            return convertToSuneduDTO(is);
        } else {
            log.error("The dni : " + id + " is wrong!");
            return null;
        }
    }

    private InputStream getRecords(String id) {
        if (!isValid(id)) {
            return null;
        }
        return suneduRestConnector.get(id);
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(SUNEDU_ID_SYNTAX);
    }

    private List<SuneduDTO> convertToSuneduDTO(InputStream inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        List<SuneduDTO> suneduObjects = new ArrayList<SuneduDTO>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }

        NodeList abreviaturaTituloRecord = doc.getElementsByTagName("abreviaturaTitulo");
        NodeList professionalQualificationRecord = doc.getElementsByTagName("tituloProfesional");
        NodeList universityRecord = doc.getElementsByTagName("universidad");
        NodeList countryRecord = doc.getElementsByTagName("pais");

        List<SuneduDTO> objects = suneduObjects;
        for (int i = 0; i < abreviaturaTituloRecord.getLength(); i++) {
            SuneduDTO dto = new SuneduDTO();
            dto.setCountry(getCountryCode(countryRecord.item(i).getTextContent()));
            dto.setAbreviaturaTitulo(educationDegree(abreviaturaTituloRecord.item(i).getTextContent()));
            dto.setProfessionalQualification(professionalQualificationRecord.item(i).getTextContent());
            dto.setUniversity(universityRecord.item(i).getTextContent());
            objects.add(dto);
        }
        return objects;
    }

    private String getCountryCode(String countryName) {
        return simpleMapConverterCountry.getValue(countryName);
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
                return "N/A";
        }
    }

}