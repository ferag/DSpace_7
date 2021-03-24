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
import org.springframework.beans.factory.annotation.Autowired;

public class DniRegistrationServiceImpl implements DniRegistrationService {

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ReniecProvider reniecProvider;

    @Override
    public DniValidationResult validateDni(Context context, String dni, LocalDate localDate) throws SQLException {

        if (dni == null || localDate == null) {
            return new DniValidationResult(HttpStatus.BAD_REQUEST_400, null);
        }

        // dni already exists
        EPerson ePerson = ePersonService.findByNetid(context, dni);
        if (ePerson != null) {
            return new DniValidationResult(HttpStatus.CONFLICT_409, null);
        }

        // reniec service error
        ReniecDTO reniecDto;
        try {
            reniecDto = getReniecProvider().getReniecObject(dni);
        } catch (Exception ex) { // Runtime exception
            return new DniValidationResult(HttpStatus.SERVICE_UNAVAILABLE_503, null);
        }

        // dni not found
        if (reniecDto == null) {
            return new DniValidationResult(HttpStatus.NOT_FOUND_404, reniecDto);
        }

        // dni and date doesn't match
        if (!localDate.isEqual(reniecDto.getBirthDate())) {
            return new DniValidationResult(HttpStatus.UNPROCESSABLE_ENTITY_422, reniecDto);
        }
        return new DniValidationResult(HttpStatus.NO_CONTENT_204, reniecDto);
    }

    public ReniecProvider getReniecProvider() {
        return reniecProvider;
    }

    public void setReniecProvider(ReniecProvider reniecProvider) {
        this.reniecProvider = reniecProvider;
    }

}
