/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ProfileItemCloneService;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile claim patches.
 *
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}/api/cris/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": " /claim", "value": <item-id>]'
 * </code>
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class ResearcherProfileClaimAddOperation extends PatchOperation<ResearcherProfile> {

    /**
     * Path in json body of patch that uses this operation.
     */
    public static final String OPERATION_CLAIM = "/claim";

    @Autowired
    private ProfileItemCloneService profileItemCloneService;

    @Autowired
    private ItemService itemService;

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        UUID value = UUIDUtils.fromString((String) operation.getValue());
        if (value == null) {
            throw new UnprocessableEntityException("The /claim value must be a valid UUID");
        }

        Item personItem = itemService.find(context, value);
        if (personItem == null) {
            throw new UnprocessableEntityException("No item found with the provided UUID: " + value);
        }

        try {
            profileItemCloneService.cloneProfile(context, profile.getItem(), personItem);
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return profile;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResearcherProfile
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
            && operation.getPath().trim().equalsIgnoreCase(OPERATION_CLAIM));
    }

}
