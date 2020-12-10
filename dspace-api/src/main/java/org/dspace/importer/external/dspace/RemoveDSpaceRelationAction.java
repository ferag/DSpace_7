/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.sql.SQLException;

import org.dspace.app.profile.service.AfterProfileDeleteAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterProfileDeleteAction} that removes relationships
 * between deleted profile and other DSpace items.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class RemoveDSpaceRelationAction implements AfterProfileDeleteAction {


    private final DSpaceItemRelationshipService dSpaceItemRelationshipService;

    @Autowired
    public RemoveDSpaceRelationAction(
        DSpaceItemRelationshipService dSpaceItemRelationshipService) {
        this.dSpaceItemRelationshipService = dSpaceItemRelationshipService;
    }


    @Override
    public void apply(Context context, Item profileItem) throws SQLException, AuthorizeException {
        dSpaceItemRelationshipService.delete(context, profileItem);
    }
}
