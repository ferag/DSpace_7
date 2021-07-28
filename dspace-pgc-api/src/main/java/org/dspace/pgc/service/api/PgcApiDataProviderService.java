/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.service.api;

import java.io.File;
import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.springframework.data.domain.Pageable;

/**
 * @author Alba Aliu
 */

public interface PgcApiDataProviderService {
    /**
     * Searches items of oai core based on the scope and compose the xml result
     *
     * @return File content of xml generated for oai items found
     */
    public File scopeData(String scope, Pageable pageable, String query,
                          Context context, HttpServletRequest request) throws Exception;

    /**
     * Searches the item on oai core based on the scope and id and compose the xml result
     *
     * @return File content of xml generated for the items found on oai core
     */
    public File scopeData(String scope, String id, Context context,
                          Pageable pageable, HttpServletRequest request) throws Exception;

    /**
     * Searches items of oai core based on the scope and compose the xml result based on inverse relation
     *
     * @return File content of xml generated for oai items found
     */
    public File scopeDataInverseRelation(String id, String scope, Context context,
                                         Pageable pageable, HttpServletRequest request) throws Exception;
}
