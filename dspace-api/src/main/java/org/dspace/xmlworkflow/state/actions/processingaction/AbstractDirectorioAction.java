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
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.util.UUIDUtils;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.ConcytecFeedback;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base implementation of {@link ProcessingAction} for actions related to the
 * directorio workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public abstract class AbstractDirectorioAction extends ProcessingAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDirectorioAction.class);

    public static final int OUTCOME_REJECT = 1;

    protected static final String SUBMIT_APPROVE = "submit_approve";
    protected static final String SUBMIT_ASSIGN = "submit_assign";
    protected static final String SUBMIT_REJECT = "submit_reject";

    @Autowired
    protected ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    protected XmlWorkflowService xmlWorkflowService;

    @Autowired
    protected WorkflowRequirementsService workflowRequirementsService;

    @Autowired
    protected EPersonService ePersonService;

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {

    }

    protected ActionResult processAccept(Context context, Item item) throws SQLException, AuthorizeException {
        addApprovedProvenance(context, item);
        concytecWorkflowService.setConcytecFeedback(context, item, ConcytecFeedback.APPROVE);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    protected ActionResult processAssign(Context context, XmlWorkflowItem workflowItem, Step step,
        HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {

        UUID userToAssignId = UUIDUtils.fromString(request.getParameter("user_to_assign"));
        if (userToAssignId == null) {
            LOGGER.warn("A parameter 'user_to_assign' with the uuid of the user to assign the task must be provided");
            addErrorField(request, "user_to_assign");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        EPerson user = ePersonService.find(context, userToAssignId);
        if (user == null) {
            LOGGER.warn("The given 'user_to_assign' uuid does not match any user in the system");
            addErrorField(request, "user_to_assign");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        List<EPerson> members = step.getRole().getMembers(context, workflowItem).getAllUniqueMembers(context);
        if (!members.contains(user)) {
            LOGGER.warn("The given 'user_to_assign' is not member of the current step role");
            addErrorField(request, "user_to_assign");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        try {
            assignTaskToUser(context, workflowItem, step, user);
        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }

    protected ActionResult processRejectPage(Context context, Item item, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        String reason = request.getParameter("reason");
        if (reason == null || 0 == reason.trim().length()) {
            addErrorField(request, "reason");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        concytecWorkflowService.setConcytecFeedback(context, item, ConcytecFeedback.REJECT);
        concytecWorkflowService.setConcytecComment(context, item, reason);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_REJECT);
    }

    private void addApprovedProvenance(Context context, Item item) throws SQLException, AuthorizeException {
        // Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = xmlWorkflowService.getEPersonName(context.getCurrentUser());
        String startId = getProvenanceStartId();

        String provenance = format("%s Approved for entry into archive by %s on %s (GMT)", startId, usersName, now);

        // Add to item as a DC field
        itemService.addMetadata(context, item, DC.getName(), "description", "provenance", "en", provenance);
        itemService.update(context, item);
    }

    private void assignTaskToUser(Context context, XmlWorkflowItem workflowItem, Step step, EPerson user)
        throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {

        context.turnOffAuthorisationSystem();
        workflowRequirementsService.addClaimedUser(context, workflowItem, step, user);
        xmlWorkflowService.createOwnedTask(context, workflowItem, step, getParent(), user);
        context.restoreAuthSystemState();

    }

}
