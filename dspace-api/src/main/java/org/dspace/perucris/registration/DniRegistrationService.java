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

/**
 * Service to check if provided dni and date are valid to register a new ePerson account.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public interface DniRegistrationService {

    /**
     * Dni and date are checked against existent eperson and data provided by the reniec database.
     * 
     * @param context
     * @param dni
     * @param date the birthdate
     * @return a DniValidationResult containing the result, the statusCode and the fetched reniecDTO. <br>
     *  The possible status codes are: <br>
     *  <ul>
     *      <li>400 bad request when dni or date are invalid or absent</li>
     *      <li>409 conflict when a eperson is already present with a netid equals to the dni</li>
     *      <li>503 service unavailable when the reniec provider is unavailable</li>
     *      <li>404 not found when the dni doens't exists in the reniec database</li>
     *      <li>422 unprocessable entity when provided date doens't match with the fetched reniecDTO</li>
     *      <li>204 no content in case of success</li>
     *  </ul>
     * @throws SQLException
     */
    DniValidationResult validateDni(Context context, String dni, LocalDate date) throws SQLException;

}
