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

import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class of an action that allows users to edit/accept/reject a
 * workflow item of the directorio.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class AcceptEditRejectDirectorioAction extends AbstractDirectorioAction {

    @Autowired
    private DedupUtils dedupUtils;

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        if (!super.isOptionInParam(request)) {
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }

        Item item = workflowItem.getItem();

        String submitButton = Util.getSubmitButton(request, SUBMIT_CANCEL);

        if (!SUBMIT_ASSIGN.equals(submitButton) && hasDuplicationNotSolved(context, item)) {
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }

        switch (submitButton) {
            case SUBMIT_APPROVE:
                return processAccept(context, workflowItem.getItem());
            case SUBMIT_ASSIGN:
                return processAssign(context, workflowItem, step, request);
            case SUBMIT_REJECT:
                return processRejectPage(context, workflowItem.getItem(), request);
            default:
                return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_ASSIGN);
        options.add(SUBMIT_REJECT);
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }

    private boolean hasDuplicationNotSolved(Context context, Item item) throws SQLException, WorkflowException {
        try {
            return dedupUtils.hasDuplicationNotSolved(context, item);
        } catch (SearchServiceException e) {
            throw new WorkflowException(e);
        }
    }
}
