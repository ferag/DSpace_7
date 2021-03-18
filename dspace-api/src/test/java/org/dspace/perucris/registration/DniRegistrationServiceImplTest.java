/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDate;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DniRegistrationServiceImplTest {

    @InjectMocks
    private DniRegistrationServiceImpl dniRegistrationService;

    @Mock
    private EPersonService ePersonService;

    @Mock
    private ReniecProvider reniecProvider;

    @Mock
    private Context context;

    @Mock
    private EPerson ePerson;

    private final static LocalDate TEST_DATE = LocalDate.of(2021, 01, 01);
    private final static LocalDate TEST_NOT_MATCHING_DATE = LocalDate.of(2020, 01, 01);
    private final static String TEST_DNI = "dni";

    @Test
    public void validateDni_whenNoDni_shouldReturnBadRequest() throws SQLException {

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, null, TEST_DATE);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void validateDni_whenNoDate_shouldReturnBadRequest() throws SQLException {

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, null);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void validateDni_whenDniAlreadyExists_shouldReturnConflict() throws SQLException {

        when(ePersonService.findByNetid(context, TEST_DNI)).thenReturn(ePerson);

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, TEST_DATE);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.CONFLICT_409);
    }

    @Test
    public void validateDni_whenUnavailableReniecProvider_shouldReturnServiceUnavailable() throws SQLException {

        when(ePersonService.findByNetid(context, TEST_DNI)).thenReturn(null);
        when(reniecProvider.getReniecObject(TEST_DNI)).thenThrow(RuntimeException.class);

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, TEST_DATE);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE_503);
    }

    @Test
    public void validateDni_whenDniNotFound_shouldReturnNotFound() throws SQLException {
        when(ePersonService.findByNetid(context, TEST_DNI)).thenReturn(null);
        when(reniecProvider.getReniecObject(TEST_DNI)).thenReturn(null);

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, TEST_DATE);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void validateDni_whenNotMatchingDate_shouldReturnUnprocessableEntity() throws SQLException {
        when(ePersonService.findByNetid(context, TEST_DNI)).thenReturn(null);
        when(reniecProvider.getReniecObject(TEST_DNI)).thenReturn(reniecDTOWithBirthDate(TEST_NOT_MATCHING_DATE));

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, TEST_DATE);

        assertNotNull(validateDni);
        assertTrue(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY_422);
    }

    @Test
    public void validateDni_whenMatchingDate_shouldReturnSuccessNoContent() throws SQLException {
        when(ePersonService.findByNetid(context, TEST_DNI)).thenReturn(null);
        when(reniecProvider.getReniecObject(TEST_DNI)).thenReturn(reniecDTOWithBirthDate(TEST_DATE));

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, TEST_DNI, TEST_DATE);

        assertNotNull(validateDni);
        assertFalse(validateDni.isError());
        assertEquals(validateDni.getStatusCode(), HttpStatus.NO_CONTENT_204);
    }

    // ----------------

    private ReniecDTO reniecDTOWithBirthDate(LocalDate date) {
        ReniecDTO dto = new ReniecDTO();
        dto.setBirthDate(date);
        return dto;
    }

}
