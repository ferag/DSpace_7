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
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class WaitForConcytecAction extends ProcessingAction {

    public static final String HAS_SHADOW_COPY_RELATIONSHIP = "hasShadowCopy";

    public static final String IS_SHADOW_COPY_RELATIONSHIP = "isShadowCopy";

    private static final String SUBMIT_REJECT = "submit_reject";

    @Autowired
    private XmlWorkflowService xmlWorkflowService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        switch (Util.getSubmitButton(request, SUBMIT_REJECT)) {
            case SUBMIT_REJECT:
                return processReject(context, workflowItem, request);
            default:
                return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }

    }

    private ActionResult processReject(Context context, XmlWorkflowItem workflowItem, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        String reason = request.getParameter("reason");
        if (reason == null || 0 == reason.trim().length()) {
            addErrorField(request, "reason");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        EPerson currentUser = context.getCurrentUser();
        String startId = this.getProvenanceStartId();

        XmlWorkflowItem workflowShadowItemCopy = findWorkflowShadowItemCopy(context, workflowItem.getItem());
        if (workflowShadowItemCopy != null) {
            xmlWorkflowService.deleteWorkflowByWorkflowItem(context, workflowShadowItemCopy, currentUser);
        }

        xmlWorkflowService.sendWorkflowItemBackSubmission(context, workflowItem, currentUser, startId, reason);

        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);

    }

    private XmlWorkflowItem findWorkflowShadowItemCopy(Context context, Item item) throws SQLException {
        Item shadowItemCopy = concytecWorkflowService.findShadowItemCopy(context, item);
        return shadowItemCopy != null ? workflowItemService.findByItem(context, shadowItemCopy) : null;
    }

    @Override
    public List<String> getOptions() {
        return List.of(SUBMIT_REJECT);
    }

}
