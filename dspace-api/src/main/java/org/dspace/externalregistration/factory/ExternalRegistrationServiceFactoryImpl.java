/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalregistration.factory;

import org.dspace.externalregistration.service.ExternalRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ExternalRegistrationServiceFactoryImpl extends ExternalRegistrationServiceFactory {

    @Autowired(required = true)
    private ExternalRegistrationService externalRegistrationService;

    @Override
    public ExternalRegistrationService getExternalRegistrationService() {
        return externalRegistrationService;
    }
}
