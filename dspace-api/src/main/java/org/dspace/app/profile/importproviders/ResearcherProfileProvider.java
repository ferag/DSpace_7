/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;

/**
 * Provider of Metadata for the enrichment of a Researcher Profile object.
 */
public interface ResearcherProfileProvider {

    Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList);

    Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source);

}
