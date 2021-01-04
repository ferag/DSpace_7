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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A user selection action that will create pooled tasks
 * for all the user who have a workflowItemRole for this
 * workflow item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AutoAssignAction extends UserSelectionAction {

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(AutoAssignAction.class);

    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected WorkflowRequirementsService workflowRequirementsService;
    @Autowired(required = true)
    protected XmlWorkflowService xmlWorkflowService;

    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        try {
            createTasks(c, wfi, step);
        } catch (SQLException | AuthorizeException | IOException e) {
            log.error(LogManager.getHeader(c, "Error while executing auto assign action",
                "Workflow item: " + wfi.getID() + " step :" + getParent().getStep().getId()), e);
            throw e;
        }


        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private void createTasks(Context context, XmlWorkflowItem workflowItem, Step step)
        throws SQLException, AuthorizeException, IOException {

        Role role = getParent().getStep().getRole();
        if (role == null) {
            return;
        }

        WorkflowActionConfig nextAction = getParent().getStep().getNextAction(this.getParent());
        // Retrieve the action which has a user interface
        while (nextAction != null && !nextAction.requiresUI()) {
            nextAction = nextAction.getStep().getNextAction(nextAction);
        }

        if (nextAction == null) {
            log.warn(LogManager.getHeader(context, "Error while executing auto assign action",
                "No valid next action. Workflow item:" + workflowItem.getID()));
            return;
        }

        RoleMembers members = role.getMembers(context, workflowItem);
        ArrayList<EPerson> allMembers = members.getAllUniqueMembers(context);
        for (EPerson member : allMembers) {
            createTaskForEPerson(context, workflowItem, step, nextAction, member);
        }

        List<WorkflowItemRole> workflowItemRoles = workflowItemRoleService.find(context, workflowItem, role.getId());
        for (WorkflowItemRole workflowItemRole : workflowItemRoles) {
            // Delete our workflow item role since the users have been assigned
            workflowItemRoleService.delete(context, workflowItemRole);
        }

    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

    /**
     * Create a claimed task for the user IF this user doesn't have a claimed action for this workflow item
     *
     * @param c            the dspace context
     * @param wfi          the workflow item
     * @param step         the current step
     * @param actionConfig the action
     * @param user         the user to create the action for
     * @throws SQLException       ...
     * @throws AuthorizeException ...
     * @throws IOException        ...
     */
    protected void createTaskForEPerson(Context c, XmlWorkflowItem wfi, Step step, WorkflowActionConfig actionConfig,
                                        EPerson user) throws SQLException, AuthorizeException, IOException {
        if (claimedTaskService.find(c, wfi, step.getId(), actionConfig.getId()) != null) {
            workflowRequirementsService.addClaimedUser(c, wfi, step, c.getCurrentUser());
            xmlWorkflowService.createOwnedTask(c, wfi, step, actionConfig, user);
        }
    }

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return true;
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi, RoleMembers roleMembers) throws SQLException {

    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI)
        throws WorkflowConfigurationException, SQLException {

        //This is an automatic assign action, it can never have a user interface
        Role role = getParent().getStep().getRole();
        if (role == null) {
            throw new WorkflowConfigurationException("The next step is invalid, since it doesn't have a valid role");
        }

        RoleMembers members = role.getMembers(context, wfi);
        if (CollectionUtils.isEmpty(members.getEPersons()) && CollectionUtils.isEmpty(members.getGroups())) {
            throw new WorkflowConfigurationException(
                "The next step is invalid, since it doesn't have any role members");
        }

        return true;
    }

    @Override
    public boolean usesTaskPool() {
        return false;
    }
}
