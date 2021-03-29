/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalregistration.provider;

import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public abstract class AbstractExternalRegistrationProvider implements ExternalRegistrationProvider {

    @Autowired(required = true)
    private EPersonService ePersonService;

    public EPersonService getePersonService() {
        return ePersonService;
    }

    public void setePersonService(EPersonService ePersonService) {
        this.ePersonService = ePersonService;
    }

}
