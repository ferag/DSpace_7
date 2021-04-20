/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterImportAction} that replace dni and orcid
 * metadata with the metadata values of the current user.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class SetOrcidAndDniAction implements AfterImportAction {

    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public void applyTo(Context context, Item item, ExternalDataObject externalDataObject)
        throws SQLException, AuthorizeException {

        itemService.clearMetadata(context, item, "perucris", "identifier", "dni", Item.ANY);
        itemService.clearMetadata(context, item, "person", "identifier", "orcid", Item.ANY);

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String orcid = ePersonService.getMetadataFirstValue(currentUser, "perucris", "eperson", "orcid", Item.ANY);
        if (StringUtils.isNotBlank(orcid)) {
            itemService.addMetadata(context, item, "person", "identifier", "orcid", null, orcid);
        }

        String dni = ePersonService.getMetadataFirstValue(currentUser, "perucris", "eperson", "dni", Item.ANY);
        if (StringUtils.isNotBlank(dni)) {
            itemService.addMetadata(context, item, "perucris", "identifier", "dni", null, dni);
        }

    }

}
