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
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class UnlockInstitutionAction extends ProcessingAction {

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        List<ClaimedTask> tasks = getClaimedTaskOfInstitutionWorkflowItem(context, workflowItem);

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private List<ClaimedTask> getClaimedTaskOfInstitutionWorkflowItem(Context context, XmlWorkflowItem workflowItem)
        throws SQLException {

        Item item = concytecWorkflowService.findCopiedItem(context, workflowItem.getItem());
        XmlWorkflowItem institutionWorkflowItem = workflowItemService.findByItem(context, item);

        return claimedTaskService.findByWorkflowItem(context, institutionWorkflowItem);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
