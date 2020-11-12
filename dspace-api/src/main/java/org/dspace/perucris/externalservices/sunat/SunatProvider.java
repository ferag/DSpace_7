/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.sunat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.perucris.externalservices.sunedu.SuneduProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SunatProvider {

    private static Logger log = LogManager.getLogger(SuneduProvider.class);

    public static final String SUNAT_ID_SYNTAX = "\\d{11}";

    @Autowired
    private SunatRestConnector sunatRestConnector;

    public SunatDTO getSunatObject(String id) {
        InputStream inputStream = getRecords(id);
        if (inputStream != null) {
            return convertToSunatDTO(inputStream);
        } else {
            log.error("The RUC NUMBER : " + id + " is wrong!");
            return null;
        }
    }

    private InputStream getRecords(String id) {
        if (!isValid(id)) {
            return null;
        }
        return sunatRestConnector.get(id);
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(SUNAT_ID_SYNTAX);
    }

    private SunatDTO convertToSunatDTO(InputStream inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        SunatDTO sunatDTO = new SunatDTO();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }
        if (doc == null) {
            return null;
        }

        Node ciiu = doc.getElementsByTagName("ddp_ciiu").item(0);
        if (!Objects.isNull(ciiu)) {
            sunatDTO.setCiiu(ciiu.getTextContent());
        }
        Node legalName = doc.getElementsByTagName("ddp_nombre").item(0);
        if (!Objects.isNull(legalName)) {
            sunatDTO.setLegalName(legalName.getTextContent().strip());
        }
        Node ddp_numruc = doc.getElementsByTagName("ddp_numruc").item(0);
        if (!Objects.isNull(ddp_numruc)) {
            sunatDTO.setRuc(ddp_numruc.getTextContent());
        }
        Node ddp_ubigeo = doc.getElementsByTagName("ddp_ubigeo").item(0);
        if (!Objects.isNull(ddp_numruc)) {
            sunatDTO.setUbigeoSunat(ddp_ubigeo.getTextContent());
        }

        buildAddressLocality(doc, sunatDTO);

        return sunatDTO;
    }

    private void buildAddressLocality(Document doc, SunatDTO sunatDTO) {
        StringBuilder addressLocality = new StringBuilder();

        addressLocality.append(checkValue(doc.getElementsByTagName("ddp_nomvia").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("ddp_nomzon").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("ddp_numer1").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("ddp_inter1").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("ddp_refer1").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("desc_dep").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("desc_prov").item(0))).append(",")
                       .append(checkValue(doc.getElementsByTagName("desc_dist").item(0)));

        sunatDTO.setAddressLocality(addressLocality.toString().strip());
    }

    private String checkValue(Node node) {
        if (!Objects.isNull(node)) {
            return node.getTextContent().strip();
        }
        return "-";
    }
}