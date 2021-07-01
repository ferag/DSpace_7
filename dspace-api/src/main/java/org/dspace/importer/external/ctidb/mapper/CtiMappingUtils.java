/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import org.dspace.vocabulary.ControlledVocabulary;

/**
 * Cti Mapping Utils.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface CtiMappingUtils {

    /**
     * Provide the Ubigeo Controlled Vocabulary.
     * @return the controlled vocabulary containing Ubigeo mapping
     */
    ControlledVocabulary getUbigeoVocabulary();

}
