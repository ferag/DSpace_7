/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.exception;

/**
 * Exception for errors that occurs during the sending of requests to Elasticsearch.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchException extends RuntimeException {

    private static final long serialVersionUID = 6388905528105549202L;

    /**
     * Constructor with error message and cause.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public ElasticsearchException (String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error message.
     *
     * @param message the error message
     */
    public ElasticsearchException (String message) {
        super(message);
    }

    /**
     * Constructor with error cause.
     *
     * @param cause the error cause
     */
    public ElasticsearchException (Throwable cause) {
        super(cause);
    }
}