/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * Interface for classes that post process the shadow copy workspace item after
 * its creation in the directorio and before the related workflow is started.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface PostShadowCopyCreationAction {

    /**
     * Process the given shadow copy workspace item.
     *
     * @param context       the DSpace context
     * @param workspaceItem the workspace item to process
     * @throws AuthorizeException if an authorization error occurs
     * @throws SQLException       if an SQL error occurs
     */
    void process(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException;
}
