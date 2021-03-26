/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalregistration.factory;

import org.dspace.externalregistration.service.ExternalRegistrationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the ExternalRegistration package.
 * Use ExternalRegistrationServiceFactory.getInstance() to retrieve
 * an implementation
 */
public abstract class ExternalRegistrationServiceFactory {

    /**
     * Calling this method will provide an ExternalRegistrationService bean
     * @return  An implementation of the ExternalRegistrationService
     */
    public abstract ExternalRegistrationService getExternalRegistrationService();


    /**
     * This method will provide you with an implementation of this class to work with
     * @return  An implementation of this class to work with
     */
    public static ExternalRegistrationServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("externalRegistrationServiceFactory",
                                            ExternalRegistrationServiceFactory.class);
    }
}
