/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static org.dspace.content.Item.ANY;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.REINSTATE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.tools.ant.util.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.versioning.ItemCorrectionProvider;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.PostShadowCopyCreationAction;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class of an action that create a shadow copy of the given item
 * into the Directorio or update that copy if the item was already archived.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ShadowCopyAction extends ProcessingAction {

    public static final int OUTCOME_FINALIZE_ITEM = 1;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ItemCorrectionProvider itemCorrectionProvider;

    @Autowired
    private WorkflowService<XmlWorkflowItem> workflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired(required = false)
    private List<PostShadowCopyCreationAction> postCreationActions;

    @Override
    public void activate(Context context, XmlWorkflowItem workflowItem) {

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, WorkflowException, AuthorizeException, IOException {

        WorkspaceItem workspaceItemShadowCopy = createShadowCopy(context, workflowItem.getItem());
        if (workspaceItemShadowCopy == null) {
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_FINALIZE_ITEM);
        }

        if (CollectionUtils.isNotEmpty(postCreationActions)) {

            for (PostShadowCopyCreationAction postAction : postCreationActions) {
                postAction.process(context, workspaceItemShadowCopy);
            }

            itemService.update(context, workspaceItemShadowCopy.getItem());
        }

        workflowService.start(context, workspaceItemShadowCopy);

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

        Item itemToReinstate = concytecWorkflowService.findReinstateItem(context, item);
        if (itemToReinstate != null) {
            return createShadowCopyForReinstate(context, item, itemToReinstate);
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

        Item mergedInItem = concytecWorkflowService.findMergeOfItem(ctx, itemToCorrectCopy);
        if (mergedInItem != null) {
            itemToCorrectCopy = mergedInItem;
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

        WorkspaceItem withdrawnWorkspaceItemCopy = createItemCopyWithdraw(ctx, itemToWithdrawCopy.getID());
        concytecWorkflowService.createShadowRelationship(ctx, withdrawItem, withdrawnWorkspaceItemCopy.getItem());
        return withdrawnWorkspaceItemCopy;
    }

    private WorkspaceItem createShadowCopyForReinstate(Context ctx, Item reinstateItem, Item itemToReinstate)
        throws SQLException, AuthorizeException {

        Item itemToReinstateCopy = concytecWorkflowService.findShadowItemCopy(ctx, itemToReinstate);
        if (itemToReinstateCopy == null || !itemToReinstateCopy.isWithdrawn() || isMergedIn(ctx, itemToReinstateCopy)) {
            return null;
        }

        WorkspaceItem reinstateWorkspaceItemCopy = createItemCopyReinstate(ctx, itemToReinstateCopy.getID());
        concytecWorkflowService.createShadowRelationship(ctx, reinstateItem, reinstateWorkspaceItemCopy.getItem());
        return reinstateWorkspaceItemCopy;
    }

    private boolean isMergedIn(Context context, Item itemToReinstateCopy) throws SQLException {
        return concytecWorkflowService.findMergeOfItem(context, itemToReinstateCopy) != null;
    }

    private Collection findDirectorioCollectionByRelationshipType(Context context, Item item)
        throws SQLException, WorkflowException {

        Community directorio = communityService.findDirectorioCommunity(context);
        if (directorio == null) {
            throw new WorkflowException("No Directorio's root community configured");
        }

        String itemType = getEntityTypeWithoutInstitutionOrCvPrefixes(item);
        Predicate<Collection> entityTypePredicate = (collection) -> hasSameEntityType(collection, itemType);
        List<Collection> collections = communityService.getCollections(context, directorio, entityTypePredicate);
        if (CollectionUtils.isEmpty(collections)) {
            throw new WorkflowException("No directorio collection found for the shadow copy of item " + item.getID());
        }

        return collections.get(0);
    }

    private String getEntityTypeWithoutInstitutionOrCvPrefixes(Item item) {
        String itemType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", ANY);
        if (itemType == null) {
            return null;
        }

        itemType = StringUtils.removePrefix(itemType, "Institution");
        itemType = StringUtils.removePrefix(itemType, "Cv");
        itemType = StringUtils.removeSuffix(itemType, "Clone");

        return itemType;
    }

    private boolean hasSameEntityType(Collection collection, String entityType) {
        String collectionType = collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", ANY);
        return Objects.equals(entityType, collectionType);
    }

    private WorkspaceItem createItemCopyCorrection(Context context, UUID itemCopyId)
        throws SQLException, AuthorizeException {
        return itemCorrectionService.createCorrectionItem(context, itemCopyId);
    }

    private WorkspaceItem createItemCopyWithdraw(Context context, UUID itemCopyId)
        throws SQLException, AuthorizeException {
        return itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            itemCopyId, WITHDRAW.getLeftType());
    }

    private WorkspaceItem createItemCopyReinstate(Context context, UUID itemCopyId)
        throws SQLException, AuthorizeException {
        return itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            itemCopyId, REINSTATE.getLeftType());
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

}
