/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanManageCTIVitaeFeature.NAME,
    description = "It can be used to verify if the user can manage the CTIVitae Item")
public class CanManageCTIVitaeFeature implements AuthorizationFeature {

    public final static String NAME = "canManageCTIVitae";

    @Autowired
    private ItemService itemService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private CrisSecurityService crisSecurityService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context,  BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            Item item = itemService.find(context, UUID.fromString(((ItemRest) object).getUuid()));
            String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
            return (StringUtils.equals(entityType, "CvPerson") && authorizeService.isAdmin(context)) ||
                       (crisSecurityService.isOwner(context.getCurrentUser(), item) &&
                        groupService.isMemberOfCTIVitaeGroup(context));
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }

}