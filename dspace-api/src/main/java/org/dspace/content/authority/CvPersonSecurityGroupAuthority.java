/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CvPersonSecurityGroupAuthority extends GroupAuthority {

    private static final Logger log = Logger.getLogger(CvPersonSecurityGroupAuthority.class);

    @Override
    protected List<Group> getGroups(Context context, String text, int start, int limit) {
        String groupUuid = configurationService.getProperty("cti-vitae.security-policy.parent-group-id");
        Group specialGroup = null;
        List<Group> groups = null;
        try {
            if (StringUtils.isNotBlank(groupUuid)) {
                specialGroup = groupService.find(context, UUID.fromString(groupUuid));
            }
            groups = groupService.search(context, text, start, limit);
            if (Objects.nonNull(specialGroup)) {
                groups = filterGroups(context, specialGroup, groups);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return groups;
    }

    private List<Group> filterGroups(Context context, Group parent, List<Group> groups)
            throws SQLException {
        List<Group> filteredGroups = new ArrayList<Group>();
        for (Group childGroup : groups) {
            if (groupService.isParentOf(context, parent, childGroup)) {
                filteredGroups.add(childGroup);
            }
        }
        return filteredGroups;
    }

}