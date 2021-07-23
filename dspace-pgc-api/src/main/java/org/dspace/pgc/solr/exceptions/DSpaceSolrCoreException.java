/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.solr.exceptions;

/**
 * @author Alba Aliu
 */

public class DSpaceSolrCoreException extends Exception {

    /**
     * Constructs an instance of <code>DSpaceSolrException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     * @param t the error.
     */
    public DSpaceSolrCoreException(String msg, Throwable t) {
        super(msg, t);
    }
}
