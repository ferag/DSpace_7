/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.service.impl.contexts;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.pgc.service.api.contexts.PgcContextService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
/**
 * @author Alba Aliu
 */

public class DSpaceContextServicePgc implements PgcContextService {
    private static final String PGC_API_CONTEXT = "PGC_API_CONTEXT";

    @Override
    public Context getContext() throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Object value = request.getAttribute(PGC_API_CONTEXT);
        if (value == null || !(value instanceof Context)) {
            request.setAttribute(PGC_API_CONTEXT, new Context());
        }
        return (Context) request.getAttribute(PGC_API_CONTEXT);
    }
}
