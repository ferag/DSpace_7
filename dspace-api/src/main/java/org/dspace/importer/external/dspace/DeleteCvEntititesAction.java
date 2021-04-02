/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.sql.SQLException;

import org.dspace.app.profile.service.BeforeProfileHardDeleteAction;
import org.dspace.app.profile.service.CvEntityService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link BeforeProfileHardDeleteAction} that deletes all the
 * CV entities related to the given profile item and create a withdrawal request
 * for each of the relative items on directorio, if present and if already
 * archived. If the items on the directory side are still in workflow they are
 * completely deleted.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DeleteCvEntititesAction implements BeforeProfileHardDeleteAction {

    @Autowired
    private CvEntityService cvEntityService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void apply(Context context, Item profileItem) throws SQLException, AuthorizeException {

        try {
            context.turnOffAuthorisationSystem();
            cvEntityService.deleteByProfileItem(context, profileItem);
            concytecWorkflowService.deleteClone(context, profileItem);
        } finally {
            context.restoreAuthSystemState();
        }

    }

}
