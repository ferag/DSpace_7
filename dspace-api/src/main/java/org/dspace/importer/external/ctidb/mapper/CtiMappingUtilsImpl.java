/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.dspace.vocabulary.ControlledVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiMappingUtilsImpl implements CtiMappingUtils {

    private static final Logger log = LoggerFactory.getLogger(CtiMappingUtilsImpl.class);

    private static final String UBIGEO_VOCABULARY = "peru_ubigeo";

    private ControlledVocabulary ubigeoVocabulary = null;

    public CtiMappingUtilsImpl() {
        try {
            this.ubigeoVocabulary = ControlledVocabulary.loadVocabulary(UBIGEO_VOCABULARY);
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException e) {
            log.error("Cannot initialize the peru_ubigeo controlled vocabulary");
        }
    }

    public ControlledVocabulary getUbigeoVocabulary() {
        return ubigeoVocabulary;
    }

}
