/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.dspace.content.Item.ANY;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.ItemCorrectionProvider;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class of an action that create a shadow copy of the given item
 * into the Directorio or update that copy if the item was already archived.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ShadowCopyAction extends ProcessingAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShadowCopyAction.class);

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemCorrectionProvider itemCorrectionProvider;

    @Autowired
    private WorkflowService<XmlWorkflowItem> workflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private InstallItemService installItemService;

    @Override
    public void activate(Context context, XmlWorkflowItem workflowItem) {

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, WorkflowException, AuthorizeException, IOException {

        WorkspaceItem workspaceItemShadowCopy = createShadowCopy(context, workflowItem.getItem());

        if (workspaceItemShadowCopy != null) {
            replaceMetadataAuthorities(context, workspaceItemShadowCopy.getItem());
            setCrisPolicyGroupMetadata(context, workspaceItemShadowCopy.getItem());
            workflowService.start(context, workspaceItemShadowCopy);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private WorkspaceItem createShadowCopy(Context context, Item item)
        throws SQLException, WorkflowException, AuthorizeException, IOException {

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        if (itemToCorrect != null) {
            return createShadowCopyForCorrection(context, item, itemToCorrect);
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, item);
        if (itemToWithdraw != null) {
            return createShadowCopyForWithdraw(context, item, itemToWithdraw);
        }

        return createShadowCopyForCreation(context, item);
    }

    private WorkspaceItem createShadowCopyForCreation(Context ctx, Item item)
        throws AuthorizeException, IOException, SQLException, WorkflowException {
        Collection collection = findDirectorioCollectionByRelationshipType(ctx, item);
        WorkspaceItem workspaceItem = itemCorrectionProvider.createNewItemAndAddItInWorkspace(ctx, collection, item);
        concytecWorkflowService.createShadowRelationship(ctx, item, workspaceItem.getItem());
        return workspaceItem;
    }

    private WorkspaceItem createShadowCopyForCorrection(Context ctx, Item correctionItem, Item itemToCorrect)
        throws SQLException, WorkflowException, AuthorizeException, IOException {

        Item itemToCorrectCopy = concytecWorkflowService.findShadowItemCopy(ctx, itemToCorrect);
        if (itemToCorrectCopy == null) {
            WorkspaceItem workspaceItemShadowCopy = createShadowCopyForCreation(ctx, itemToCorrect);
            itemToCorrectCopy = installItemService.installItem(ctx, workspaceItemShadowCopy);
            itemService.withdraw(ctx, itemToCorrectCopy);
        }

        WorkspaceItem correctionWorkspaceItemCopy = createItemCopyCorrection(ctx, itemToCorrectCopy.getID());
        concytecWorkflowService.createShadowRelationship(ctx, correctionItem, correctionWorkspaceItemCopy.getItem());

        ItemCorrection itemCorrection = itemCorrectionService.getAppliedCorrections(ctx, itemToCorrect, correctionItem);
        itemCorrectionService.applyCorrectionsOnItem(ctx, correctionWorkspaceItemCopy.getItem(), itemCorrection);

        return correctionWorkspaceItemCopy;

    }

    private WorkspaceItem createShadowCopyForWithdraw(Context ctx, Item withdrawItem, Item itemToWithdraw)
        throws SQLException, AuthorizeException {

        Item itemToWithdrawCopy = concytecWorkflowService.findShadowItemCopy(ctx, itemToWithdraw);
        if (itemToWithdrawCopy == null || itemToWithdrawCopy.isWithdrawn()) {
            return null;
        }

        WorkspaceItem withdrawnWorkspaceItemCopy = createItemCopyWithDraw(ctx, itemToWithdrawCopy.getID());
        concytecWorkflowService.createShadowRelationship(ctx, withdrawItem, withdrawnWorkspaceItemCopy.getItem());
        return withdrawnWorkspaceItemCopy;
    }

    private Collection findDirectorioCollectionByRelationshipType(Context context, Item item)
        throws SQLException, WorkflowException {

        Community directorio = findDirectorioCommunity(context);

        Predicate<Collection> relationshipTypePredicate = (collection) -> hasSameRelatioshipType(collection, item);
        List<Collection> collections = communityService.getCollections(context, directorio, relationshipTypePredicate);
        if (CollectionUtils.isEmpty(collections)) {
            throw new WorkflowException("No directorio collection found for the shadow copy of item " + item.getID());
        }

        return collections.get(0);
    }

    private Community findDirectorioCommunity(Context context) throws WorkflowException, SQLException {
        UUID directorioId = UUIDUtils.fromString(configurationService.getProperty("directorios.community-id"));
        if (directorioId == null) {
            throw new WorkflowException("Invalid directorios.community-id set");
        }
        return communityService.find(context, directorioId);
    }

    private boolean hasSameRelatioshipType(Collection collection, Item item) {
        String collectionType = collectionService.getMetadataFirstValue(collection, "relationship", "type", null, ANY);
        String itemType = itemService.getMetadataFirstValue(item, "relationship", "type", null, ANY);
        return Objects.equals(collectionType, itemType) || Objects.equals("Institution" + collectionType, itemType);
    }

    private WorkspaceItem createItemCopyCorrection(Context context, UUID itemCopyId)
        throws SQLException, AuthorizeException {
        String relationshipName = itemCorrectionService.getCorrectionRelationshipName();
        return itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context, itemCopyId, relationshipName);
    }

    private WorkspaceItem createItemCopyWithDraw(Context context, UUID itemCopyId)
        throws SQLException, AuthorizeException {
        String relationshipName = ConcytecWorkflowService.IS_WITHDRAW_OF_ITEM_RELATIONSHIP;
        return itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context, itemCopyId, relationshipName);
    }

    private void replaceMetadataAuthorities(Context context, Item item) throws SQLException, AuthorizeException {
        for (MetadataValue metadataValue : item.getMetadata()) {
            String authority = metadataValue.getAuthority();
            if (isNotBlank(authority) && notStartsWithWillBePrefix(authority) && isItemAuthority(metadataValue)) {
                metadataValue.setAuthority(AuthorityValueService.REFERENCE + "SHADOW::" + authority);
            }
        }
        itemService.update(context, item);
    }

    private boolean notStartsWithWillBePrefix(String authority) {
        return !authority.startsWith(REFERENCE) && !authority.startsWith(GENERATE);
    }

    private boolean isItemAuthority(MetadataValue value) {
        return choiceAuthorityService.isItemAuthority(value.getMetadataField().toString('_'));
    }

    private void setCrisPolicyGroupMetadata(Context context, Item item) throws SQLException {

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

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

}
