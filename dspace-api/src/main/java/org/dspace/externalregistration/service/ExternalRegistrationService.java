package org.dspace.externalregistration.service;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authenticate.model.OIDCProfileElementsResponse;
import org.dspace.authenticate.model.OIDCTokenResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This is an interface that will deal with all Service level calls for External Registration
 */
public interface ExternalRegistrationService {

    /**
     * This method will return whether a suitable registration provider exists for a given userData context.
     * @return  true if a suitable registration provider exists, false otherwise
     */
    boolean canRegister(OIDCProfileElementsResponse userData);

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
    EPerson registerEPerson(Context context, HttpServletRequest request, OIDCTokenResponse tokens,
            OIDCProfileElementsResponse userData) throws SQLException;

}
