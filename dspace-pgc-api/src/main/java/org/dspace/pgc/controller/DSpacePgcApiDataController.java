/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.pgc.controller;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.File;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.pgc.service.api.PgcApiDataProviderService;
import org.dspace.pgc.service.api.contexts.PgcContextService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alba Aliu
 * Serves as entry point for all requests with prefix pgc-api
 */

@RestController
// Use the configured "pgc-api.path" for all requests, or "/pgc-api" by default
@RequestMapping("/${pgc-api.path:pgc-api}")
// Only enable this controller if "pgc-api.enabled=true"
@ConditionalOnProperty("pgc-api.enabled")
public class DSpacePgcApiDataController {
    private static final Logger log = getLogger(DSpacePgcApiDataController.class);
    @Autowired
    private PgcContextService contextServicePgc;
    @Autowired
    private PgcApiDataProviderService pgcApiDataProviderService;
    @Autowired
    private ConfigurationService configurationService;
    /*** returns the content of file xml for document found on oai core
     **
     * @param scope is the scope that will be translated into uuid
     * @param pageable will contain offset and pageSize
     * @param query will query to filter on search core
     */
    @RequestMapping(method = RequestMethod.GET, value = "{scope}", produces = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<FileSystemResource> getSearchObjects(@PathVariable String scope,
                                                               @RequestParam(name = "query") String query,
                                                               @PageableDefault(page = 0, size = 10) Pageable pageable,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) throws Exception {
        if (invalidScope(scope)) {
            return ResponseEntity.notFound().build();
        }
        try {
            Context context = contextServicePgc.getContext();
            response.setContentType("application/xml");
            File tempFileXml = pgcApiDataProviderService.scopeData(scope, pageable, query, context, request);
            tempFileXml.deleteOnExit();
            final FileSystemResource fileSystemResource = new FileSystemResource(tempFileXml.getPath());
            return ResponseEntity.ok(fileSystemResource);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * returns the content of file xml for document found on oai core
     *
     * @param scope is the scope that will be translated into uuid
     * @param id represents the uuid of item to search on oai
     * @param pageable will contain offset and pageSize
     * @param query is used to support requests ctivitae/search
     */

    @RequestMapping(method = RequestMethod.GET, value = "{scope}/{id}", produces = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<FileSystemResource> getSearchObjectsById(@PathVariable String scope,
                                                   @PathVariable String id,
                                                   @RequestParam(name = "query", required = false) String query,
                                                   @PageableDefault(page = 0, size = 10) Pageable pageable,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {

        if (invalidScope(scope)) {
            return ResponseEntity.notFound().build();
        }
        if (invalidUUID(id)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Context context = contextServicePgc.getContext();
            response.setContentType("application/xml");
            File tempFileXml;
            if (id.equals("search")) {
                tempFileXml = pgcApiDataProviderService.scopeData(scope, pageable, query, context, request);
            } else {
                tempFileXml = pgcApiDataProviderService.scopeData(scope, id, context, pageable, request);
            }
            tempFileXml.deleteOnExit();
            final FileSystemResource fileSystemResource = new FileSystemResource(tempFileXml.getPath());
            return ResponseEntity.ok(fileSystemResource);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * returns the content of file xml for document found on oai core
     *
     * @param scope is the scope that will be translated into uuid
     * @param id represents the uuid of the collection
     * @param pageable will contain offset and pageSize
     */
    @RequestMapping(method = RequestMethod.GET, value = "ctivitae/{id}/{scope}",
        produces = { MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<FileSystemResource> getSearchCtiVitaeObjects(@PathVariable String id,
                                                       @PathVariable String scope,
                                                       @PageableDefault(page = 0, size = 10) Pageable pageable,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) throws Exception {
        if (invalidCtiVitaeEntity(scope)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Context context = contextServicePgc.getContext();
            response.setContentType("application/xml");
            File tempFileXml = pgcApiDataProviderService.scopeDataInverseRelation(id, scope,
                                                                                  context, pageable, request);
            tempFileXml.deleteOnExit();
            final FileSystemResource fileSystemResource = new FileSystemResource(tempFileXml.getPath());
            return ResponseEntity.ok(fileSystemResource);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private boolean invalidCtiVitaeEntity(final String scope) {
        return Objects.isNull(configurationService.getPropertyValue("pgc-api." + scope + ".inverseRelation"));
    }

    private boolean invalidUUID(final String id) {
        return Objects.isNull(UUIDUtils.fromString(id));
    }

    private boolean invalidScope(final String scope) {
        return Objects.isNull(configurationService.getPropertyValue("pgc-api." + scope + ".id"));
    }
}
