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

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing class of an action that allows users to accept/reject a workflow
 * item on directorio.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class FinalEditDirectorioAction extends AbstractDirectorioAction {

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(context, workflowItem.getItem());
                case SUBMIT_ASSIGN:
                    return processAssign(context, workflowItem, step, request);
                default:
                    //We pressed the leave button so return to our submissions page
                    return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_ASSIGN);
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }

}
