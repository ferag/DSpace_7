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
        ReniecDTO dto = new ReniecDTO();
        NodeList reniecInfo = doc.getElementsByTagName("return");
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
        String fatherLastName = reniecInfo.item(1).getTextContent();
        String maternalLastName = reniecInfo.item(2).getTextContent();
        String lastNameMarried = reniecInfo.item(3).getTextContent();
        if (fatherLastName != null && StringUtils.isNotBlank(fatherLastName)) {
            dto.setFatherLastName(fatherLastName);
        }
        if (maternalLastName != null && StringUtils.isNotBlank(maternalLastName)) {
            dto.setMaternalLastName(maternalLastName);
        }
        if (lastNameMarried != null && StringUtils.isNotBlank(lastNameMarried)) {
            dto.setLastNameMarried(lastNameMarried);
        }
    }

    private void setNames(ReniecDTO dto, NodeList reniecInfo) {
        String names = reniecInfo.item(4).getTextContent();
        if (names != null && StringUtils.isNotBlank(names)) {
            dto.setNames(names);
        }
    }

    private void setHomeCode(ReniecDTO dto, NodeList reniecInfo) {

        StringBuilder homeCode = new StringBuilder();
        String regionCode = reniecInfo.item(5).getTextContent();
        String provinceCode = reniecInfo.item(6).getTextContent();
        String districtCode = reniecInfo.item(7).getTextContent();

        if (StringUtils.isNotBlank(regionCode) &&
            StringUtils.isNotBlank(provinceCode) &&
            StringUtils.isNotBlank(districtCode)) {

            homeCode.append(regionCode).append(provinceCode).append(districtCode);
            dto.setHomeCode(homeCode.toString());
        } else {
            log.error("Some fields of HomeCode are empty or null ");
            log.error("Region code is : " + regionCode);
            log.error("Province code is : " + provinceCode);
            log.error("District code is : " + districtCode);
        }
    }

    private void setResidence(ReniecDTO dto, NodeList reniecInfo) {
        String regionOfResidence = reniecInfo.item(8).getTextContent();
        String provinceOfResidenc = reniecInfo.item(9).getTextContent();
        String districtOfResidence = reniecInfo.item(10).getTextContent();
        String homeAddress = reniecInfo.item(11).getTextContent();

        if (StringUtils.isNotBlank(regionOfResidence)) {
            dto.setRegionOfResidence(regionOfResidence);
        }
        if (StringUtils.isNotBlank(provinceOfResidenc)) {
            dto.setProvinceOfResidence(provinceOfResidenc);
        }
        if (StringUtils.isNotBlank(districtOfResidence)) {
            dto.setDistrictOfResidence(districtOfResidence);
        }
        if (StringUtils.isNotBlank(homeAddress)) {
            dto.setHomeAddress(homeAddress);
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
        String regionNacimentoCode = reniecInfo.item(14).getTextContent();
        String provinceNacimentoCode = reniecInfo.item(15).getTextContent();
        String districtNacimentoCode = reniecInfo.item(16).getTextContent();

        if (StringUtils.isNotBlank(regionNacimentoCode) &&
            StringUtils.isNotBlank(provinceNacimentoCode) &&
            StringUtils.isNotBlank(districtNacimentoCode)) {

            nacimientoCode.append(regionNacimentoCode).append(provinceNacimentoCode).append(districtNacimentoCode);
            dto.setNacimientoCode(nacimientoCode.toString());
        } else {
            log.error("Some fields of Nacimiento Code are empty or null ");
            log.error("Region code is : " + regionNacimentoCode);
            log.error("Province code is : " + provinceNacimentoCode);
            log.error("District code is : " + districtNacimentoCode);
        }
    }

    private void setDescriptionOfBirth(ReniecDTO dto, NodeList reniecInfo) {
        String regionOfBirth = reniecInfo.item(17).getTextContent();
        String provinceOfBirth = reniecInfo.item(18).getTextContent();
        String districtOfBirth = reniecInfo.item(19).getTextContent();

        if (StringUtils.isNotBlank(regionOfBirth)) {
            dto.setRegionOfBirth(regionOfBirth);
        }
        if (StringUtils.isNotBlank(provinceOfBirth)) {
            dto.setProvinceOfBirth(provinceOfBirth);
        }
        if (StringUtils.isNotBlank(districtOfBirth)) {
            dto.setDistrictOfBirth(districtOfBirth);
        }
    }

    private void setBirthDate(ReniecDTO dto, NodeList reniecInfo) {
        String stringDate = reniecInfo.item(20).getTextContent();
        if (stringDate == null | StringUtils.isNotBlank(stringDate) | stringDate.length() == 8) {
            LocalDate date = LocalDate.of(Integer.parseInt(stringDate.substring(0, 4)),
                    Integer.parseInt(stringDate.substring(4, 6)), Integer.parseInt(stringDate.substring(6)));
            dto.setBirthDate(date);
        } else {
            log.error("Wrong format date : " + stringDate);
        }
    }
}
