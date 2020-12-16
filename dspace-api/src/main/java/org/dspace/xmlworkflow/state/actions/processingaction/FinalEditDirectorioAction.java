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
 * Processing class of an action that allows users to accept/reject a workflow
 * item on directorio.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class FinalEditDirectorioAction extends ProcessingAction {

    private static final String SUBMIT_APPROVE = "submit_approve";

    @Autowired
    private XmlWorkflowService xmlWorkflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        return processMainPage(context, workflowItem, request);
    }

    public ActionResult processMainPage(Context context, XmlWorkflowItem workflowItem, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    processAccept(context, workflowItem.getItem());
                    return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
                default:
                    //We pressed the leave button so return to our submissions page
                    return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    private void processAccept(Context context, Item item) throws SQLException, AuthorizeException {
        concytecWorkflowService.setConcytecFeedback(context, item, ConcytecFeedback.APPROVE);
        addApprovedProvenance(context, item);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
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
