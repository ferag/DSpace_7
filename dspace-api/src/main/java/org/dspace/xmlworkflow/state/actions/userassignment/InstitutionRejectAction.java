/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extension of {@link UserSelectionAction} that allow institution's users to
 * reject a submitted item.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class InstitutionRejectAction extends UserSelectionAction {

    @Autowired
    private XmlWorkflowService xmlWorkflowService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void activate(Context context, XmlWorkflowItem wfItem) throws SQLException, IOException, AuthorizeException {
        Step owningStep = getParent().getStep();

        RoleMembers allroleMembers = getParent().getStep().getRole().getMembers(context, wfItem);
        // Create pooled tasks for each member of our group
        if (allroleMembers != null && (allroleMembers.getGroups().size() > 0 || allroleMembers.getEPersons()
                                                                                              .size() > 0)) {
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                     .createPoolTasks(context, wfItem, allroleMembers, owningStep, getParent());
            alertUsersOnActivation(context, wfItem, allroleMembers);
        } else {
            log.info(LogManager.getHeader(context, "warning while activating claim action",
                                          "No group or person was found for the following roleid: " + getParent()
                                              .getStep().getRole().getId()));
        }

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        EPerson currentUser = context.getCurrentUser();
        String startId = this.getProvenanceStartId();

        XmlWorkflowItem workflowShadowItemCopy = findWorkflowShadowItemCopy(context, workflowItem.getItem());
        if (workflowShadowItemCopy != null) {
            xmlWorkflowService.deleteWorkflowByWorkflowItem(context, workflowShadowItemCopy, currentUser);
        }

        xmlWorkflowService.sendWorkflowItemBackSubmission(context, workflowItem, currentUser, startId, "");

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

    @Override
    public void alertUsersOnActivation(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers)
        throws IOException, SQLException {
        try {
            EPerson ep = wfi.getSubmitter();
            String submitterName = null;
            if (ep != null) {
                submitterName = ep.getFullName();
            }
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.alertUsersOnTaskActivation(c, wfi, "submit_task", roleMembers.getAllUniqueMembers(c),
                    wfi.getItem().getName(),
                    wfi.getCollection().getName(),
                    submitterName,
                    "New task available.",
                    xmlWorkflowService.getMyDSpaceLink()
            );
        } catch (MessagingException e) {
            log.info(LogManager.getHeader(c, "error emailing user(s) for claimed task",
                    "step: " + getParent().getStep().getId() + " workflowitem: " + wfi.getID()));
        }
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers) {

    }

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return false;
    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI)
        throws WorkflowConfigurationException, SQLException {
        //A user claim action always needs to have a UI, since somebody needs to be able to claim it
        if (hasUI) {
            Step step = getParent().getStep();
            //First of all check if our step has a role
            Role role = step.getRole();
            if (role != null) {
                //We have a role, check if we have a group to with that role
                RoleMembers roleMembers = role.getMembers(context, wfi);

                ArrayList<EPerson> epersons = roleMembers.getAllUniqueMembers(context);
                return !(epersons.size() == 0 || step.getRequiredUsers() > epersons.size());
            } else {
                // We don't have a role and do have a UI so throw a workflow exception
                throw new WorkflowConfigurationException(
                    "The next step is invalid, since it doesn't have a valid role");
            }
        } else {
            return true;
        }

    }

    @Override
    public boolean usesTaskPool() {
        return true;
    }

    private XmlWorkflowItem findWorkflowShadowItemCopy(Context context, Item item) throws SQLException {
        Item shadowItemCopy = concytecWorkflowService.findShadowItemCopy(context, item);
        return shadowItemCopy != null ? workflowItemService.findByItem(context, shadowItemCopy) : null;
    }

}
