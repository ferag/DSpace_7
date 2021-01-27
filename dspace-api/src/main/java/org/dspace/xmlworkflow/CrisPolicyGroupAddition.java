/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.service.PostShadowCopyCreationAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link PostShadowCopyCreationAction} that add
 * cris.policy.group metadata to the shadow copy item just created.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisPolicyGroupAddition implements PostShadowCopyCreationAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrisPolicyGroupAddition.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void process(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {

        Item item = workspaceItem.getItem();
        itemService.clearMetadata(context, item, "cris", "policy", "group", Item.ANY);

        String[] policyGroups = configurationService.getArrayProperty("directorio.security.policy-groups");
        if (ArrayUtils.isEmpty(policyGroups)) {
            return;
        }

        for (String policyGroup : policyGroups) {
            UUID groupId = UUIDUtils.fromString(policyGroup);
            if (groupId == null) {
                LOGGER.warn("Invalid directorio.security.policy-groups property set: " + policyGroup);
                continue;
            }

            Group group = groupService.find(context, groupId);
            if (group == null) {
                LOGGER.warn("Policy group not found by directorio.security.policy-groups value " + policyGroup);
                continue;
            }

            itemService.addMetadata(context, item, "cris", "policy", "group", null,
                group.getNameWithoutTypePrefix(), policyGroup, 600);
        }
    }

}
