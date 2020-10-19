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
        NodeList tituloProfesionalRecord = doc.getElementsByTagName("tituloProfesional");
        NodeList universidadRecord = doc.getElementsByTagName("universidad");
        NodeList paisRecord = doc.getElementsByTagName("pais");

        String id = nroDocumento.item(0).getTextContent();
        String abreviaturaTitulo = abreviaturaTituloRecord.item(0).getTextContent();
        String tituloProfesional = tituloProfesionalRecord.item(0).getTextContent();
        String universidad = universidadRecord.item(0).getTextContent();
        String pais = paisRecord.item(0).getTextContent();

        externalDataObject.setId(id);
        if (!StringUtils.isBlank(abreviaturaTitulo)) {
            externalDataObject.addMetadata(
                              new MetadataValueDTO("crisrp", "qualificatio", null, null, abreviaturaTitulo));
        }
        if (!StringUtils.isBlank(tituloProfesional)) {
            externalDataObject.addMetadata(
                              new MetadataValueDTO("crisrp", "qualificatio", null, null, tituloProfesional));
        }
        if (!StringUtils.isBlank(universidad)) {
            externalDataObject.addMetadata(new MetadataValueDTO("crisrp", "equcation", "grantor", null, universidad));
        }
        if (!StringUtils.isBlank(pais)) {
            externalDataObject.addMetadata(new MetadataValueDTO("crisrp", "equcation", "country", null, pais));
        }
        return externalDataObject;
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