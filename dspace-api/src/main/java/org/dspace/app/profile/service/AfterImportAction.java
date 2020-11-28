/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.profile.service;

import org.dspace.content.Item;

/**
 * Defines a contract that applies on items created with {@link ImportResearcherProfileService}
 * in case of need provide one or many implementations and inject them into {@link ImportResearcherProfileServiceImpl}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface AfterImportAction {
    /**
     * Manipulates and performs actions on a given item.
     *
     * @param item item to be manipulated by this action implementation
     */
    void applyTo(Item item);
}
