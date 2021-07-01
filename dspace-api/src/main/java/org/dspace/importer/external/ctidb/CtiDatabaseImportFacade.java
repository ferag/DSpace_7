/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb;

import java.util.List;

import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.external.model.ExternalDataObject;

/**
 * Point of access to import methods of CtiEntities as ExternalDataObjects.
 * Retrieve metadata of Profile, Publication, Project, Patent and Suggestions from the cti database.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface CtiDatabaseImportFacade {

    /**
     * Get the cti profile metadata from the provided source id.
     * Allowed id are "dni" and "orcid".
     * @param source the source containing the investigador dni or orcid.
     * @return
     */
    ExternalDataObject getCtiProfile(ResearcherProfileSource source);

    /**
     * Get suggestions related to the researcher profile.
     * Allowed id are "dni" and "orcid".
     * @param source the source containing the investigador dni or orcid.
     * @return
     */
    List<ExternalDataObject> getCtiSuggestions(ResearcherProfileSource source);

    /**
     * Get the Publication data.
     * @param ctiPublicationId
     * @return
     */
    ExternalDataObject getCtiPublication(String ctiId);

    /**
     * Get the Project data.
     * @param ctiPublicationId
     * @return
     */
    ExternalDataObject getCtiProject(String ctiId);

    /**
     * Get the Patent data.
     * @param ctiPublicationId
     * @return
     */
    ExternalDataObject getCtiPatent(String ctiId);

}
