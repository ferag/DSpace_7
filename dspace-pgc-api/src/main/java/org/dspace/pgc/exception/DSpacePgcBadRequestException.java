/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * When a request is malformed, we use this exception to indicate this to the client
 *
 * @author Alba Aliu
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad Request")
public class DSpacePgcBadRequestException extends RuntimeException {

    public DSpacePgcBadRequestException(String message) {
        this(message, null);
    }

    public DSpacePgcBadRequestException(String message, Exception e) {
        super(message, e);
    }
}
