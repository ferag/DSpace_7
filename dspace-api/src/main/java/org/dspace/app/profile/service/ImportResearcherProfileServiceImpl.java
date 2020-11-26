/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile.service;

import java.net.URI;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;


/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ImportResearcherProfileServiceImpl implements ImportResearcherProfileService {

    @Override
    public Item createFrom(Context context, EPerson ePerson, URI source) {
        return null;
    }
}
