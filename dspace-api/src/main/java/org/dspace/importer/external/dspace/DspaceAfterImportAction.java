/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterImportAction} that create relationship between
 * a newly created item and an existing one already in DSpace, identified by external data object id
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DspaceAfterImportAction implements AfterImportAction {

    private final ItemService itemService;


    private final DSpaceItemRelationshipService dSpaceItemRelationshipService;

    @Autowired
    public DspaceAfterImportAction(ItemService itemService,
                                   DSpaceItemRelationshipService dSpaceItemRelationshipService) {
        this.itemService = itemService;
        this.dSpaceItemRelationshipService = dSpaceItemRelationshipService;
    }

    @Override
    public void applyTo(Context context, Item item, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {

        Item rightItem = itemService.find(context, UUID.fromString(externalDataObject.getId()));
        dSpaceItemRelationshipService.create(context, item, rightItem);
    }
}
