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
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CORRECTION;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.REINSTATE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.WITHDRAW;

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
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.ConcytecFeedback;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
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

    @Autowired
    protected ItemCorrectionService itemCorrectionService;

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {

    }

    protected ActionResult processAccept(Context context, Item item) throws SQLException, AuthorizeException {
        addApprovedProvenance(context, item);
        addFeedback(context, item, ConcytecFeedback.APPROVE, null);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    protected ActionResult processAssign(Context context, XmlWorkflowItem workflowItem, Step step,
        HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {

        UUID userToAssignId = UUIDUtils.fromString(request.getParameter("user"));
        if (userToAssignId == null) {
            LOGGER.warn("A parameter 'user' with the uuid of the user to assign the task must be provided");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        EPerson user = ePersonService.find(context, userToAssignId);
        if (user == null) {
            LOGGER.warn("The given user uuid does not match any user in the system");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        List<EPerson> members = step.getRole().getMembers(context, workflowItem).getAllUniqueMembers(context);
        if (!members.contains(user)) {
            LOGGER.warn("The given user is not member of the current step role");
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
        addFeedback(context, item, ConcytecFeedback.REJECT, reason);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private void addFeedback(Context context, Item item, ConcytecFeedback feedback, String comment)
        throws SQLException {

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        if (itemToCorrect != null) {
            concytecWorkflowService.addConcytecFeedback(context, itemToCorrect, CORRECTION, feedback, comment);
            concytecWorkflowService.addConcytecFeedback(context, item, CORRECTION, feedback, comment);
            return;
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, item);
        if (itemToWithdraw != null) {
            concytecWorkflowService.addConcytecFeedback(context, itemToWithdraw, WITHDRAW, feedback, comment);
            concytecWorkflowService.addConcytecFeedback(context, item, WITHDRAW, feedback, comment);
            return;
        }

        Item itemToReinstate = concytecWorkflowService.findReinstateItem(context, item);
        if (itemToReinstate != null) {
            concytecWorkflowService.addConcytecFeedback(context, itemToReinstate, REINSTATE, feedback, comment);
            concytecWorkflowService.addConcytecFeedback(context, item, REINSTATE, feedback, comment);
            return;
        }

        concytecWorkflowService.addConcytecFeedback(context, item, feedback, comment);
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

        EPerson currentUser = context.getCurrentUser();

        ClaimedTask task = claimedTaskService.findByWorkflowIdAndEPerson(context, workflowItem, currentUser);
        if (task != null) {
            xmlWorkflowService.deleteClaimedTask(context, workflowItem, task);
            workflowRequirementsService.removeClaimedUser(context, workflowItem, task.getOwner(), task.getStepID());
        }

        workflowRequirementsService.addClaimedUser(context, workflowItem, step, user);
        xmlWorkflowService.createOwnedTask(context, workflowItem, step, getParent(), user);

        context.restoreAuthSystemState();

    }

}
