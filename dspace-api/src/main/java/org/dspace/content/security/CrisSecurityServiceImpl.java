/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CrisSecurityService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisSecurityServiceImpl implements CrisSecurityService {

    private static final Logger log = LoggerFactory.getLogger(CrisSecurityServiceImpl.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private GroupService groupService;

    @Override
    public boolean hasAccess(Context context, Item item, EPerson user, AccessItemMode accessMode)
        throws SQLException {
        boolean isOwner = isOwner(item, user);
        boolean isAdmin = authorizeService.isAdmin(context, user);
        return hasAccess(context, item, user, isOwner, isAdmin, accessMode);
    }

    /**
     * Returns true if the given eperson is the owner of item, false otherwise
     * @param  item
     * @param  eperson
     * @return         true if the given eperson is the owner, false otherwise
     */
    private boolean isOwner(Item item, EPerson eperson) {
        return itemService.getMetadata(item, "cris", "owner", null, Item.ANY).stream()
            .anyMatch(value -> eperson.getID().toString().equals(value.getAuthority()));
    }

    private boolean hasAccess(Context context, Item item, EPerson user, boolean isOwner,
        boolean isAdmin, AccessItemMode accessMode) throws SQLException {

        CrisSecurity security = accessMode.getSecurity();

        if ((security == CrisSecurity.ADMIN || security == CrisSecurity.ADMIN_OWNER) && isAdmin) {
            return true;
        }

        if ((security == CrisSecurity.OWNER || security == CrisSecurity.ADMIN_OWNER) && isOwner) {
            return true;
        }

        if (security == CrisSecurity.CUSTOM) {

            boolean hasAccessByGroup = hasAccessByGroup(context, item, user, accessMode.getGroups());
            if (hasAccessByGroup) {
                return true;
            }

            boolean hasAccessByUser = hasAccessByUser(context, item, user, accessMode.getUsers());
            if (hasAccessByUser) {
                return true;
            }

        }

        return false;
    }

    private boolean hasAccessByGroup(Context context, Item item, EPerson user, List<String> groups)
        throws SQLException {

        if (CollectionUtils.isEmpty(groups)) {
            return false;
        }

//        List<Group> userGroups = user.getGroups();
        log.info("looking up user groups");
        Set<Group> userGroups = groupService.allMemberGroupsSet(context, user);
        log.info("found {} user groups", userGroups.size());
        userGroups.forEach(ug -> log.info("Group: {} - {}", ug.getName(), ug.getID().toString()));
        if (CollectionUtils.isEmpty(userGroups)) {
            log.info("User groups empty");
            return false;
        }

        for (Group group : userGroups) {
            for (String metadataGroup : groups) {
                if (check(context, item, metadataGroup, group.getID())) {
                    log.info("Found matching group");
                    return true;
                }
            }
        }
        log.info("Matching group not found");
        return false;
    }

    private boolean hasAccessByUser(Context context, Item item, EPerson user, List<String> users) throws SQLException {

        if (CollectionUtils.isEmpty(users)) {
            return false;
        }

        for (String metadataUser : users) {
            if (check(context, item, metadataUser, user.getID())) {
                return true;
            }
        }

        return false;
    }

    private boolean check(Context context, Item item, String metadata, UUID uuid) throws SQLException {
        return itemService.getMetadataByMetadataString(item, metadata).stream()
            .anyMatch(value -> uuid.toString().equals(value.getAuthority()));
    }

}
