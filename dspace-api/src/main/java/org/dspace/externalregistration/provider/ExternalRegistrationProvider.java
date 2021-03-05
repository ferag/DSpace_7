package org.dspace.externalregistration.provider;

import java.sql.SQLException;

import org.dspace.authenticate.model.OIDCProfileElementsResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This interface should be implemented by all providers that will deal with external registration
 */
public interface ExternalRegistrationProvider {

    /**
     * Check whether the provider support registration for the given userData context.
     * @param userData
     * @return
     */
    boolean support(OIDCProfileElementsResponse userData);

    /**
     * Creates an instance of the ePerson together with relevant attributes and metadata
     *  for the external registration provider.
     * @param context
     * @param userData
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     */
    EPerson createEPerson(
            Context context, OIDCProfileElementsResponse userData) throws SQLException, AuthorizeException;
}
