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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to finalize the current workflow item base on it's scope (standard
 * submission, correction request or withdraw request).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class FinalizeInstitutionItemAction extends ProcessingAction {

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private XmlWorkflowService workflowService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        if (itemCorrectionService.checkIfIsCorrectionItem(context, workflowItem.getItem())) {
            return applyCorrectionOnItem(context, workflowItem);
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, workflowItem.getItem());
        if (itemToWithdraw != null) {
            return withdrawItem(context, workflowItem, itemToWithdraw);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private ActionResult applyCorrectionOnItem(Context context, XmlWorkflowItem workflowItem) {
        itemCorrectionService.replaceCorrectionItemWithNative(context, workflowItem);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private ActionResult withdrawItem(Context context, XmlWorkflowItem workflowItem, Item itemToWithdraw)
        throws SQLException, AuthorizeException, IOException {
        itemService.withdraw(context, itemToWithdraw);
        workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
