/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.factory;

import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the profile package, use ResearcherProfileServiceFactory.getInstance()
 *  to retrieve an implementation
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public abstract class ResearcherProfileServiceFactory {

    public abstract ResearcherProfileService getResearcherProfileService();

    public static ResearcherProfileServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance()
                .getServiceManager()
                .getServiceByName("researcherProfileServiceFactory", ResearcherProfileServiceFactory.class);
    }
}
