/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.exception;

/**
 * This exception is thrown when the given search configuration
 * @author Alba Aliu
 */

public class InvalidPgcSearchRequestException extends RuntimeException {

    public InvalidPgcSearchRequestException(String message) {
        super(message);
    }

}
