/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalregistration.provider.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authenticate.model.OIDCProfileElementsResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.externalregistration.provider.AbstractExternalRegistrationProvider;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class RENIECRegistrationProvider extends AbstractExternalRegistrationProvider {

    @Override
    public boolean support(OIDCProfileElementsResponse userData) {
        return StringUtils.isNotBlank(userData.getReniecDni());
    }

    @Override
    public EPerson createEPerson(Context context, OIDCProfileElementsResponse userData)
            throws SQLException, AuthorizeException {
        EPerson eperson = getePersonService().create(context);

        List<String> vals = new ArrayList<String>();
        vals.add(userData.getReniecDni());
        getePersonService().addMetadata(context, eperson, "perucris", "eperson", "dni", null, vals);
        eperson.setNetid(userData.getReniecDni());

        if (StringUtils.isNotBlank(userData.getEmail())) {
            eperson.setEmail(userData.getEmail());
        }
        if (StringUtils.isNotBlank(userData.getGivenName())) {
            eperson.setFirstName(context, userData.getGivenName());
        }
        if (StringUtils.isNotBlank(userData.getFamilyName())) {
            eperson.setLastName(context, userData.getFamilyName());
        }
        if (StringUtils.isNotBlank(userData.getBirthdate())) {
            vals = new ArrayList<String>();
            vals.add(userData.getBirthdate());
            getePersonService().addMetadata(context, eperson, "perucris", "eperson", "birthdate", null, vals);
        }

        return eperson;
    }

}
