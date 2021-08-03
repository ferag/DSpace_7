/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.service.api.contexts;
import org.dspace.core.Context;
/**
 * @author Alba Aliu
 */

public interface PgcContextService {
    Context getContext() throws Exception;
}
