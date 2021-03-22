/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.factory;

import org.dspace.app.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the profile package, use ResearcherProfileServiceFactory.getInstance()
 *  to retrieve an implementation
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileServiceFactoryImpl extends ResearcherProfileServiceFactory {

    @Autowired(required = true)
    private ResearcherProfileService researcherProfileService;

    @Override
    public ResearcherProfileService getResearcherProfileService() {
        return researcherProfileService;
    }

}
