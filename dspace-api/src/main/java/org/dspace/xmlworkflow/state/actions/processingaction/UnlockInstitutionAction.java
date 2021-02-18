/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static org.dspace.app.deduplication.model.DuplicateDecisionType.WORKFLOW;
import static org.dspace.app.deduplication.model.DuplicateDecisionValue.VERIFY;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CORRECTION;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.REINSTATE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.deduplication.utils.DuplicateItemInfo;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.core.Context.Mode;
import org.dspace.discovery.SearchServiceException;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.ConcytecFeedback;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to unlock the institution workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class UnlockInstitutionAction extends ProcessingAction {

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private XmlWorkflowFactory workflowFactory;

    @Autowired
    private XmlWorkflowService workflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private DedupUtils dedupUtils;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        Item item = workflowItem.getItem();

        Item institutionItem = concytecWorkflowService.findCopiedItem(context, item);

        ConcytecFeedback concytecFeedback = concytecWorkflowService.getLastConcytecFeedback(context, item);

        try {

            if (institutionItem != null) {
                unlockInstitutionWorkflow(context, item, institutionItem, concytecFeedback);
            }

        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }

        return finalizeItem(context, workflowItem, institutionItem, concytecFeedback);

    }

    private void unlockInstitutionWorkflow(Context context, Item directorioItem, Item institutionItem,
        ConcytecFeedback feedback) throws IOException, AuthorizeException, SQLException, WorkflowException,
        WorkflowConfigurationException {

        XmlWorkflowItem institutionWorkflowItem = workflowItemService.findByItem(context, institutionItem);

        if (feedback != ConcytecFeedback.NONE) {
            String comment = concytecWorkflowService.getLastConcytecComment(context, directorioItem);
            addFeedback(context, institutionItem, feedback, comment);
        }

        Workflow institutionWorkflow = workflowFactory.getWorkflow(institutionWorkflowItem.getCollection());
        Step waitForConcytecStep = institutionWorkflow.getStep("waitForConcytecStep");
        WorkflowActionConfig waitForConcytecActionConfig = waitForConcytecStep.getActionConfig("waitForConcytecAction");

        workflowService.processOutcome(context, context.getCurrentUser(), institutionWorkflow, waitForConcytecStep,
            waitForConcytecActionConfig, getCompleteActionResult(), institutionWorkflowItem, true);

    }

    private void addFeedback(Context context, Item item, ConcytecFeedback feedback, String comment)
        throws SQLException {

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        if (itemToCorrect != null) {
            concytecWorkflowService.addConcytecFeedback(context, item, CORRECTION, feedback, comment);
            return;
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, item);
        if (itemToWithdraw != null) {
            concytecWorkflowService.addConcytecFeedback(context, itemToWithdraw, WITHDRAW, feedback, comment);
            return;
        }

        Item itemToReinstate = concytecWorkflowService.findReinstateItem(context, item);
        if (itemToReinstate != null) {
            concytecWorkflowService.addConcytecFeedback(context, itemToReinstate, REINSTATE, feedback, comment);
            return;
        }

        concytecWorkflowService.addConcytecFeedback(context, item, feedback, comment);
    }

    private ActionResult finalizeItem(Context context, XmlWorkflowItem workflowItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, IOException, WorkflowException {

        Item duplicateItem = findVerifiedDuplicateItem(context, workflowItem.getItem());
        if (duplicateItem != null) {
            return finalizeItemMerge(context, workflowItem, institutionItem, duplicateItem, concytecFeedback);
        }

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, workflowItem.getItem());
        if (itemToCorrect != null) {
            return finalizeItemCorrection(context, workflowItem, institutionItem, concytecFeedback);
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, workflowItem.getItem());
        if (itemToWithdraw != null) {
            return finalizeItemWithdraw(context, workflowItem, itemToWithdraw, concytecFeedback);
        }

        Item itemToReinstate = concytecWorkflowService.findReinstateItem(context, workflowItem.getItem());
        if (itemToReinstate != null) {
            return finalizeItemReinstate(context, workflowItem, itemToReinstate, concytecFeedback);
        }

        return finalizeItemCreation(context, workflowItem, institutionItem, concytecFeedback);
    }

    private Item findVerifiedDuplicateItem(Context context, Item item) throws SQLException, WorkflowException {
        List<DuplicateItemInfo> duplicatedItemInfos = findVerifiedDuplicatedItems(context, item);
        if (CollectionUtils.isEmpty(duplicatedItemInfos)) {
            return null;
        }

        if (duplicatedItemInfos.size() > 1) {
            throw new WorkflowException("Multiple duplicated item found related to the item with id: " + item.getID());
        }

        return (Item) duplicatedItemInfos.get(0).getDuplicateItem();
    }

    private List<DuplicateItemInfo> findVerifiedDuplicatedItems(Context context, Item item)
        throws SQLException, WorkflowException {

        try {

            return dedupUtils.findDuplicatedItems(context, item).stream()
                .filter(duplicateItemInfo -> VERIFY == duplicateItemInfo.getDecision(WORKFLOW))
                .collect(Collectors.toList());

        } catch (SearchServiceException e) {
            throw new WorkflowException(e);
        }

    }

    private ActionResult finalizeItemMerge(Context context, XmlWorkflowItem workflowItem, Item institutionItem,
        Item duplicateItem, ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, IOException {

        Item item = workflowItem.getItem();

        boolean isRejected = concytecFeedback == ConcytecFeedback.REJECT;

        if (institutionItem != null) {
            replaceWillBeReferencedWithItemId(context, isRejected ? duplicateItem : item, institutionItem);
        }

        if (isRejected) {

            item = installItemService.installItem(context, workflowItem);
            itemService.withdraw(context, item);

            concytecWorkflowService.createMergedInRelationship(context, item, duplicateItem);
            if (institutionItem != null) {
                concytecWorkflowService.createOriginatedFromRelationship(context, duplicateItem, institutionItem);
            }

            return getCancelActionResult();

        } else {

            itemService.withdraw(context, duplicateItem);
            concytecWorkflowService.createMergedInRelationship(context, duplicateItem, item);

            replaceOriginatedFromRelationships(context, duplicateItem, item);
            replaceAuthoritiesWithItemId(context, duplicateItem, item);

            return getCompleteActionResult();
        }

    }

    private ActionResult finalizeItemCorrection(Context context, XmlWorkflowItem workflowItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, IOException {

        if (institutionItem != null) {
            replaceWillBeReferencedWithItemId(context, workflowItem.getItem(), institutionItem);
        }

        if (concytecFeedback == ConcytecFeedback.REJECT) {
            workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
            return getCancelActionResult();
        } else {
            itemCorrectionService.replaceCorrectionItemWithNative(context, workflowItem);
            return getCompleteActionResult();
        }

    }

    private ActionResult finalizeItemCreation(Context context, XmlWorkflowItem workflowItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, WorkflowException {

        if (institutionItem != null) {
            replaceWillBeReferencedWithItemId(context, workflowItem.getItem(), institutionItem);
        }

        if (concytecFeedback == ConcytecFeedback.REJECT) {
            Item item = installItemService.installItem(context, workflowItem);
            itemService.withdraw(context, item);
            return getCancelActionResult();
        }

        return getCompleteActionResult();
    }

    private ActionResult finalizeItemWithdraw(Context context, XmlWorkflowItem workflowItem, Item itemToWithdraw,
        ConcytecFeedback feedback) throws SQLException, AuthorizeException, IOException {

        if (feedback == ConcytecFeedback.APPROVE) {
            itemService.withdraw(context, itemToWithdraw);
        }

        workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
        return getCancelActionResult();
    }

    private ActionResult finalizeItemReinstate(Context context, XmlWorkflowItem workflowItem, Item itemToReinstate,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, IOException {

        if (concytecFeedback == ConcytecFeedback.APPROVE) {
            itemService.reinstate(context, itemToReinstate);
        }

        workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
        return getCancelActionResult();
    }

    private void replaceWillBeReferencedWithItemId(Context context, Item item, Item institutionItem)
        throws SQLException, AuthorizeException {
        Mode originalMode = context.getCurrentMode();
        try {
            context.setMode(Mode.BATCH_EDIT);
            String authority = AuthorityValueService.REFERENCE + "SHADOW::" + institutionItem.getID();
            replaceAuthorities(context, item, authority);
        } finally {
            context.setMode(originalMode);
        }
    }

    private void replaceAuthoritiesWithItemId(Context context, Item itemToReplace, Item item)
        throws SQLException, AuthorizeException {
        Mode originalMode = context.getCurrentMode();
        try {
            context.setMode(Mode.BATCH_EDIT);
            replaceAuthorities(context, item, itemToReplace.getID().toString());
        } finally {
            context.setMode(originalMode);
        }
    }


    private void replaceAuthorities(Context context, Item item, String authority)
        throws SQLException, AuthorizeException {
        Iterator<Item> itemIterator = findItemsWithAuthority(context, authority, item);

        while (itemIterator.hasNext()) {
            Item itemToUpdate = itemIterator.next();

            itemToUpdate.getMetadata().stream()
                .filter(metadataValue -> authority.equals(metadataValue.getAuthority()))
                .forEach(metadataValue -> metadataValue.setAuthority(item.getID().toString()));

            itemService.update(context, itemToUpdate);
        }
    }

    private Iterator<Item> findItemsWithAuthority(Context context, String authority, Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        if (relationshipType == null) {
            throw new IllegalArgumentException("The given item has no relationship.type: " + item.getID());
        }

        return itemService.findByAuthorityControlledMetadataFields(context, authority, relationshipType);
    }

    private void replaceOriginatedFromRelationships(Context context, Item duplicateItem, Item item)
        throws SQLException, AuthorizeException {

        Item institutionDuplicateItem = concytecWorkflowService.findCopiedItem(context, duplicateItem);
        if (institutionDuplicateItem != null) {
            concytecWorkflowService.createOriginatedFromRelationship(context, item, institutionDuplicateItem);
        }

        List<Item> originatedFromItems = concytecWorkflowService.findOriginatedFromItems(context, duplicateItem);
        for (Item originatedFromItem : originatedFromItems) {
            concytecWorkflowService.createOriginatedFromRelationship(context, item, originatedFromItem);
        }

    }

    private ActionResult getCompleteActionResult() {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private ActionResult getCancelActionResult() {
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
