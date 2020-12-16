/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static java.lang.String.format;
import static org.dspace.content.MetadataSchemaEnum.DC;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.ConcytecFeedback;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
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
public class AcceptEditRejectDirectorioAction extends ProcessingAction {

    public static final int OUTCOME_REJECT = 1;

    private static final String SUBMIT_APPROVE = "submit_approve";
    private static final String SUBMIT_REJECT = "submit_reject";

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private XmlWorkflowService xmlWorkflowService;

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(context, workflowItem.getItem());
                case SUBMIT_REJECT:
                    return processRejectPage(context, workflowItem.getItem(), request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_REJECT);
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }

    public ActionResult processAccept(Context context, Item item)
        throws SQLException, AuthorizeException {
        //Delete the tasks
        addApprovedProvenance(context, item);
        concytecWorkflowService.setConcytecFeedback(context, item, ConcytecFeedback.APPROVE);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    public ActionResult processRejectPage(Context context, Item item, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        String reason = request.getParameter("reason");
        if (reason == null || 0 == reason.trim().length()) {
            addErrorField(request, "reason");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        concytecWorkflowService.setConcytecFeedback(context, item, ConcytecFeedback.REJECT);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_REJECT);
    }

    private void addApprovedProvenance(Context context, Item item) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = xmlWorkflowService.getEPersonName(context.getCurrentUser());
        String startId = getProvenanceStartId();

        String provenance = format("%s Approved for entry into archive by %s on %s (GMT)", startId, usersName, now);

        // Add to item as a DC field
        itemService.addMetadata(context, item, DC.getName(), "description", "provenance", "en", provenance);
        itemService.update(context, item);
    }
}
