/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import static org.dspace.app.rest.repository.patch.operation.DSpaceObjectMetadataPatchUtils.OPERATION_METADATA_PATH;
import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupType;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link PatchOperation} to handle the addition or
 * replacement of the perucris.group.status metadata of groups. This metadata
 * causes the group to be activated or deactivated, based on the new value to be
 * set. In addition to modifying the metadata, for normal groups that do not
 * represent roles, deactivating a group involves emptying the memberships with
 * a copy in the appropriate metadata perucris.group.disabledusermember and
 * perucris.group.disabledgroupmember, while the reactivation involves the
 * reverse process.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
@Order(GroupStatusMetadataAddOrReplaceOperation.ORDER)
public class GroupStatusMetadataAddOrReplaceOperation extends PatchOperation<Group> {

    public static final int ORDER = DSpaceObjectMetadataAddOperation.ORDER - 1;

    private final Logger log = LoggerFactory.getLogger(GroupStatusMetadataAddOrReplaceOperation.class);

    @Autowired
    private DSpaceObjectMetadataPatchUtils metadataPatchUtils;

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        String op = operation.getOp().trim();
        return (operation.getPath().startsWith(OPERATION_METADATA_PATH + "/perucris.group.status")
            && (op.equalsIgnoreCase(OPERATION_ADD) || op.equalsIgnoreCase(OPERATION_REPLACE))
            && objectToMatch instanceof Group);
    }

    @Override
    public Group perform(Context context, Group group, Operation operation) throws SQLException {
        if (!supports(group, operation)) {
            throw new DSpaceBadRequestException("EPersonCertificateReplaceOperation does not support this operation.");
        }

        checkOperationValue(operation.getValue());
        MetadataValueRest metadataValue = metadataPatchUtils.extractMetadataValueFromOperation(operation);

        if (group.isPermanent() && metadataValue.getValue().equalsIgnoreCase("DISABLED")) {
            throw new UnprocessableEntityException("A permanent group can't be disabled");
        }

        boolean valueHasChanged;
        if (operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)) {
            addMetadata(context, group, metadataValue);
            valueHasChanged = true;
        } else {
            valueHasChanged = replaceMetadata(context, group, metadataValue);
        }

        if (GroupType.NORMAL == groupService.getGroupType(group) && valueHasChanged) {

            if (metadataValue.getValue().equalsIgnoreCase("ENABLED")) {
                enableMembers(context, group);
            } else if (metadataValue.getValue().equalsIgnoreCase("DISABLED")) {
                disableMembers(context, group);
            }

            try {
                groupService.update(context, group);
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException(e);
            }

        }

        return group;
    }

    private void addMetadata(Context context, Group group, MetadataValueRest metadataValue) throws SQLException {
        List<MetadataValue> metadata = groupService.getMetadata(group, "perucris", "group", "status", ANY);
        if (CollectionUtils.isNotEmpty(metadata)) {
            throw new UnprocessableEntityException("The group already has a perucris.group.status metadata");
        }

        groupService.addAndShiftRightMetadata(context, group, "perucris", "group", "status",
            metadataValue.getLanguage(), metadataValue.getValue(), metadataValue.getAuthority(),
            metadataValue.getConfidence(), 0);
    }

    private boolean replaceMetadata(Context context, Group group, MetadataValueRest metadataValue) {
        List<MetadataValue> metadata = groupService.getMetadata(group, "perucris", "group", "status", ANY);
        if (CollectionUtils.isEmpty(metadata)) {
            throw new UnprocessableEntityException("The group has no perucris.group.status metadata to replace");
        }

        MetadataValue existingMdv = metadata.get(0);
        existingMdv.setAuthority(metadataValue.getAuthority());
        existingMdv.setConfidence(metadataValue.getConfidence());
        existingMdv.setLanguage(metadataValue.getLanguage());
        groupService.setMetadataModified(group);

        if (existingMdv.getValue() != null && existingMdv.getValue().equalsIgnoreCase(metadataValue.getValue())) {
            return false;
        } else {
            existingMdv.setValue(metadataValue.getValue());
            return true;
        }
    }

    private void enableMembers(Context context, Group group) throws SQLException {
        enableEPersonMembers(context, group);
        enableGroupMembers(context, group);
    }

    private void disableMembers(Context context, Group group) throws SQLException {
        disableEPersonMembers(context, group);
        disableGroupMembers(context, group);
    }

    private void enableEPersonMembers(Context context, Group group) throws SQLException {
        List<MetadataValue> values = groupService.getMetadata(group, "perucris", "group", "disabledusermember", ANY);
        for (MetadataValue metadata : values) {
            String value = metadata.getValue();

            UUID epersonId = UUIDUtils.fromString(value);
            if (epersonId == null) {
                log.warn("Found perucris.group.disabledusermember metadata with invalid value: {}", value);
                continue;
            }

            EPerson member = ePersonService.find(context, epersonId);
            if (member == null) {
                log.warn("No ePerson found with id {}", epersonId);
                continue;
            }

            groupService.addMember(context, group, member);
        }

        groupService.removeMetadataValues(context, group, values);
    }

    private void enableGroupMembers(Context context, Group group) throws SQLException {
        List<MetadataValue> values = groupService.getMetadata(group, "perucris", "group", "disabledgroupmember", ANY);
        for (MetadataValue metadata : values) {
            String value = metadata.getValue();

            UUID groupId = UUIDUtils.fromString(value);
            if (groupId == null) {
                log.warn("Found perucris.group.disabledgroupmember metadata with invalid value: {}", value);
                continue;
            }

            Group member = groupService.find(context, groupId);
            if (member == null) {
                log.warn("No group found with id {}", groupId);
                continue;
            }

            groupService.addMember(context, group, member);
        }

        groupService.removeMetadataValues(context, group, values);
    }

    private void disableEPersonMembers(Context context, Group group) throws SQLException {
        for (EPerson member : new ArrayList<>(group.getMembers())) {
            String value = UUIDUtils.toString(member.getID());
            groupService.addMetadata(context, group, "perucris", "group", "disabledusermember", null, value, null, -1);
            groupService.removeMember(context, group, member);
        }
    }

    private void disableGroupMembers(Context context, Group group) throws SQLException {
        for (Group member : new ArrayList<>(group.getMemberGroups())) {
            String value = UUIDUtils.toString(member.getID());
            groupService.addMetadata(context, group, "perucris", "group", "disabledgroupmember", null, value, null, -1);
            groupService.removeMember(context, group, member);
        }
    }

}
