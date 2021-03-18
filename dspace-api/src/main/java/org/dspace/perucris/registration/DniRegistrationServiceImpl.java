/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.registration;


import java.sql.SQLException;
import java.time.LocalDate;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DniRegistrationServiceImpl implements DniRegistrationService {

    protected static final Logger log = LoggerFactory.getLogger(DniRegistrationServiceImpl.class);

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ReniecProvider reniecProvider;

    @Override
    public DniValidationResult validateDni(Context context, String dni, LocalDate date) throws SQLException {

        if (dni == null || date == null) {
            return new DniValidationResult(true, HttpStatus.BAD_REQUEST_400, null);
        }

        // dni already exists
        EPerson ePerson = ePersonService.findByNetid(context, dni);
        if (ePerson != null) {
            return new DniValidationResult(true, HttpStatus.CONFLICT_409, null);
        }

        // reniec service error
        ReniecDTO reniecDto = null;
        try {
            reniecDto = getReniecProvider().getReniecObject(dni);
        } catch (Exception ex) { // Runtime exception
            return new DniValidationResult(true, HttpStatus.SERVICE_UNAVAILABLE_503, reniecDto);
        }

        // dni not found
        if (reniecDto == null) {
            return new DniValidationResult(true, HttpStatus.NOT_FOUND_404, reniecDto);
        }

        // dni and date doesn't match
        if (!date.isEqual(reniecDto.getBirthDate())) {
            return new DniValidationResult(true, HttpStatus.UNPROCESSABLE_ENTITY_422, reniecDto);
        }
        return new DniValidationResult(false,  HttpStatus.NO_CONTENT_204, reniecDto);
    }

    public ReniecProvider getReniecProvider() {
        return reniecProvider;
    }

    public void setReniecProvider(ReniecProvider reniecProvider) {
        this.reniecProvider = reniecProvider;
    }

}
