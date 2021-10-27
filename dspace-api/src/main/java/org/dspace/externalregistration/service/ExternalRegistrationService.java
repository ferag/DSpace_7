/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalregistration.service;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authenticate.model.CasProfileElementsResponse;
import org.dspace.authenticate.model.CasTokenResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This is an interface that will deal with all Service level calls for External Registration.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public interface ExternalRegistrationService {

    /**
     * This method will return whether a suitable registration provider exists for a given userData context.
     * @return  true if a suitable registration provider exists, false otherwise
     */
    boolean canRegister(CasProfileElementsResponse userData);

    /**
     * Register the ePerson in DSpace after an successful authentication event.
     * @param context
     * @param tokens
     * @param userData
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws JsonProcessingException
     */
    EPerson registerEPerson(Context context, HttpServletRequest request, CasTokenResponse tokens,
            CasProfileElementsResponse userData) throws SQLException;

}
