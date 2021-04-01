/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.registration;

import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.eclipse.jetty.http.HttpStatus;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class DniValidationResult {

    private final int statusCode;
    private final ReniecDTO reniecDto;

    public DniValidationResult(int statusCode, ReniecDTO reniecDto) {
        this.statusCode = statusCode;
        this.reniecDto = reniecDto;
    }

    public boolean isError() {
        return statusCode != HttpStatus.NO_CONTENT_204;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ReniecDTO getReniecDto() {
        return reniecDto;
    }

}
