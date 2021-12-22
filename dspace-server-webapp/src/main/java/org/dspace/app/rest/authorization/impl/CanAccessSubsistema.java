/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupType;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks if the given user can claim the given item.
 *
 * @author Davide Negretti (davide.negretti at 4science.it)
 *
 */
@Component
@AuthorizationFeatureDocumentation(name = CanAccessSubsistema.NAME,
    description = "Used to verify if current user can access Subsistema")
public class CanAccessSubsistema implements AuthorizationFeature {

    public static final String NAME = "canAccessSubsistema";

    @Autowired
    private GroupService groupService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        List<Group> specialGroups = context.getSpecialGroups();

        return specialGroups.stream().anyMatch(group -> groupService.getGroupType(group).equals(GroupType.SCOPED));
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { SiteRest.CATEGORY + "." + SiteRest.NAME };
    }

}
