/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
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

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        Item item = workflowItem.getItem();
        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        boolean isCorrectionItem = itemToCorrect != null;

        ConcytecFeedback concytecFeedback = concytecWorkflowService.getConcytecFeedback(context, item);

        try {

            Item institutionItem = concytecWorkflowService.findCopiedItem(context, item);
            if (institutionItem != null) {
                unlockInstitutionWorkflow(context, item, institutionItem, concytecFeedback);
            }

        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }

        if (concytecFeedback == ConcytecFeedback.REJECT) {
            handleConcytecRejection(context, workflowItem, isCorrectionItem);
            return getCancelActionResult();
        }

        if (isCorrectionItem) {
            itemCorrectionService.replaceCorrectionItemWithNative(context, workflowItem);
            return getCompleteActionResult();
        }

        return getCompleteActionResult();

    }

    private void unlockInstitutionWorkflow(Context context, Item directorioItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws IOException, AuthorizeException, SQLException, WorkflowException,
        WorkflowConfigurationException {

        XmlWorkflowItem institutionWorkflowItem = workflowItemService.findByItem(context, institutionItem);

        if (concytecFeedback != ConcytecFeedback.NONE) {
            concytecWorkflowService.setConcytecFeedback(context, institutionItem, concytecFeedback);
            String concytecComment = concytecWorkflowService.getConcytecComment(context, directorioItem);
            if (StringUtils.isNotBlank(concytecComment)) {
                concytecWorkflowService.setConcytecComment(context, institutionItem, concytecComment);
            }
        }

        Workflow institutionWorkflow = workflowFactory.getWorkflow(institutionWorkflowItem.getCollection());
        Step waitForConcytecStep = institutionWorkflow.getStep("waitForConcytecStep");
        WorkflowActionConfig waitForConcytecActionConfig = waitForConcytecStep.getActionConfig("waitForConcytecAction");

        workflowService.processOutcome(context, context.getCurrentUser(), institutionWorkflow, waitForConcytecStep,
            waitForConcytecActionConfig, getCompleteActionResult(), institutionWorkflowItem, true);

    }

    private void handleConcytecRejection(Context context, XmlWorkflowItem workflowItem, boolean isCorrectionItem)
        throws SQLException, AuthorizeException, IOException {

        if (isCorrectionItem) {
            workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
        } else {
            Item item = installItemService.installItem(context, workflowItem);
            itemService.withdraw(context, item);
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
