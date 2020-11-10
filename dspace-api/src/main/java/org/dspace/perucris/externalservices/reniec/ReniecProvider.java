/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.reniec;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is the implementation of the ExternalDataProvider that
 * will deal with the RENIEC External Data lookup
 * 
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ReniecProvider {

    private static Logger log = LogManager.getLogger(ReniecProvider.class);

    public static final String RENIEC_ID_SYNTAX = "\\d{8}";

    @Autowired
    private ReniecRestConnector reniecRestConnector;

    public ReniecDTO getReniecObject(String id) {
        InputStream is = getRecords(id);
        if (is != null) {
            return convertToReniecDTO(is);
        } else {
            log.error("The dni : " + id + " is wrong!");
            return null;
        }
    }

    private InputStream getRecords(String id) {
        if (!isValid(id)) {
            return null;
        }
        return reniecRestConnector.get(id);
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(RENIEC_ID_SYNTAX);
    }

    private ReniecDTO convertToReniecDTO(InputStream inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }
        if (doc == null || doc.getElementsByTagName("return").item(22) == null) {
            return null;
        }
        NodeList reniecInfo = doc.getElementsByTagName("return");
        ReniecDTO dto = new ReniecDTO();
        setLastName(dto, reniecInfo);
        setNames(dto, reniecInfo);
        setHomeCode(dto, reniecInfo);
        setResidence(dto, reniecInfo);
        setSex(dto, reniecInfo);
        setNacimento(dto, reniecInfo);
        setDescriptionOfBirth(dto, reniecInfo);
        setBirthDate(dto, reniecInfo);
        dto.setIdentifierDni(reniecInfo.item(22).getTextContent());
        return dto;
    }

    private void setLastName(ReniecDTO dto, NodeList reniecInfo) {
        if (StringUtils.isNotBlank(reniecInfo.item(1).getTextContent())) {
            dto.setFatherLastName(reniecInfo.item(1).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(2).getTextContent())) {
            dto.setMaternalLastName(reniecInfo.item(2).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(3).getTextContent())) {
            dto.setLastNameMarried(reniecInfo.item(3).getTextContent());
        }
    }

    private void setNames(ReniecDTO dto, NodeList reniecInfo) {
        if (StringUtils.isNotBlank(reniecInfo.item(4).getTextContent())) {
            dto.setNames(reniecInfo.item(4).getTextContent());
        }
    }

    private void setHomeCode(ReniecDTO dto, NodeList reniecInfo) {
        StringBuilder homeCode = new StringBuilder();
        if (StringUtils.isNotBlank(reniecInfo.item(5).getTextContent()) &&
            StringUtils.isNotBlank(reniecInfo.item(6).getTextContent()) &&
            StringUtils.isNotBlank(reniecInfo.item(7).getTextContent())) {

            homeCode.append(reniecInfo.item(5).getTextContent())
                    .append(reniecInfo.item(6).getTextContent())
                    .append(reniecInfo.item(7).getTextContent());
            dto.setHomeCode(homeCode.toString());
        } else {
            log.error("Some fields of HomeCode are empty or null!");
        }
    }

    private void setResidence(ReniecDTO dto, NodeList reniecInfo) {
        if (StringUtils.isNotBlank(reniecInfo.item(8).getTextContent())) {
            dto.setRegionOfResidence(reniecInfo.item(8).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(9).getTextContent())) {
            dto.setProvinceOfResidence(reniecInfo.item(9).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(10).getTextContent())) {
            dto.setDistrictOfResidence(reniecInfo.item(10).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(11).getTextContent())) {
            dto.setHomeAddress(reniecInfo.item(11).getTextContent());
        }
    }

    private void setSex(ReniecDTO dto, NodeList reniecInfo) {
        int indexSex = Integer.parseInt(reniecInfo.item(13).getTextContent());
        if (indexSex == 1 | indexSex == 2) {
            dto.setIndexSex(indexSex);
        } else {
            log.error("Sex index field has wrong code, it must to be 1 or 2");
            log.error("Sex code is : " + indexSex);
        }
    }

    private void setNacimento(ReniecDTO dto, NodeList reniecInfo) {
        StringBuilder nacimientoCode = new StringBuilder();
        if (StringUtils.isNotBlank(reniecInfo.item(14).getTextContent()) &&
            StringUtils.isNotBlank(reniecInfo.item(15).getTextContent()) &&
            StringUtils.isNotBlank(reniecInfo.item(16).getTextContent())) {

            nacimientoCode.append(reniecInfo.item(14).getTextContent())
                          .append(reniecInfo.item(15).getTextContent())
                          .append(reniecInfo.item(16).getTextContent());
            dto.setNacimientoCode(nacimientoCode.toString());
        } else {
            log.error("Some fields of Nacimiento Code are empty or null!");
        }
    }

    private void setDescriptionOfBirth(ReniecDTO dto, NodeList reniecInfo) {
        if (StringUtils.isNotBlank(reniecInfo.item(17).getTextContent())) {
            dto.setRegionOfBirth(reniecInfo.item(17).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(18).getTextContent())) {
            dto.setProvinceOfBirth(reniecInfo.item(18).getTextContent());
        }
        if (StringUtils.isNotBlank(reniecInfo.item(19).getTextContent())) {
            dto.setDistrictOfBirth(reniecInfo.item(19).getTextContent());
        }
    }

    private void setBirthDate(ReniecDTO dto, NodeList reniecInfo) {
        if (StringUtils.isNotBlank(reniecInfo.item(20).getTextContent()) &&
            reniecInfo.item(20).getTextContent().length() == 8) {
            LocalDate date = LocalDate.of(Integer.parseInt(reniecInfo.item(20).getTextContent().substring(0, 4)),
                                          Integer.parseInt(reniecInfo.item(20).getTextContent().substring(4, 6)),
                                          Integer.parseInt(reniecInfo.item(20).getTextContent().substring(6)));
            dto.setBirthDate(date);
        } else if (StringUtils.isNotBlank(reniecInfo.item(20).getTextContent())) {
            log.error("Wrong format date : " + reniecInfo.item(20).getTextContent());
        } else {
            log.error("Date is blank: " + reniecInfo.item(20).getTextContent());
        }
    }
}
