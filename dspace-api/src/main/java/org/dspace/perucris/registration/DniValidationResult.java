/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.registration;

import org.dspace.perucris.externalservices.reniec.ReniecDTO;

public class DniValidationResult {

    private boolean isError;
    private int statusCode;
    private ReniecDTO reniecDto;

    public DniValidationResult(boolean isError, int statusCode, ReniecDTO reniecDto) {
        this.setError(isError);
        this.setStatusCode(statusCode);
        this.setReniecDto(reniecDto);
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public ReniecDTO getReniecDto() {
        return reniecDto;
    }

    public void setReniecDto(ReniecDTO reniecDto) {
        this.reniecDto = reniecDto;
    }

}