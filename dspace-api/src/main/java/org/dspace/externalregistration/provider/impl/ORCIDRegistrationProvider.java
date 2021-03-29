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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authenticate.model.OIDCProfileElementsResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.dspace.externalregistration.provider.AbstractExternalRegistrationProvider;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ORCIDRegistrationProvider extends AbstractExternalRegistrationProvider {

    @Autowired(required = true)
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    @Override
    public boolean support(OIDCProfileElementsResponse userData) {
        return userData.getOrcid() != null;
    }

    @Override
    public EPerson createEPerson(Context context, OIDCProfileElementsResponse userData)
            throws SQLException, AuthorizeException {
        EPerson eperson = getePersonService().create(context);
        List<String> vals = new ArrayList<String>();

        Person orcidPerson = getOrcidV3AuthorDataProvider().getBio(userData.getOrcid());
        Optional<Email> email = orcidPerson.getEmails().getEmails().stream().findFirst();
        if (!email.isPresent()) {
            throw new IllegalStateException("Orcid email is required to proceed");
        }

        vals.add(userData.getOrcid());
        getePersonService().addMetadata(context, eperson, "perucris", "eperson", "orcid", null, vals);
        eperson.setNetid(userData.getOrcid());

        eperson.setEmail(email.get().getEmail());
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

    public OrcidV3AuthorDataProvider getOrcidV3AuthorDataProvider() {
        return orcidV3AuthorDataProvider;
    }

    public void setOrcidV3AuthorDataProvider(OrcidV3AuthorDataProvider orcidV3AuthorDataProvider) {
        this.orcidV3AuthorDataProvider = orcidV3AuthorDataProvider;
    }

}
