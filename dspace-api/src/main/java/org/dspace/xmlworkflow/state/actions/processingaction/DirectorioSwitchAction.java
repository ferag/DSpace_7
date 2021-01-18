/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ProcessingAction} related to the first step of the
 * Directorio's workflow to switch between the flow to accept/reject/edit an
 * item submission/correction and the flow to accept/reject an item withdrawn.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DirectorioSwitchAction extends ProcessingAction {

    public static final int OUTCOME_SUBMISSION_OR_CORRECTION = 1;

    public static final int OUTCOME_WITHDRAWN = 2;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException {

        Item item = workflowItem.getItem();

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        if (itemToCorrect != null) {
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_SUBMISSION_OR_CORRECTION);
        }

        Item itemToWithdraw = concytecWorkflowService.findWithdrawnItem(context, item);
        if (itemToWithdraw != null) {
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_WITHDRAWN);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, OUTCOME_SUBMISSION_OR_CORRECTION);
    }

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public List<String> getOptions() {
        return Collections.emptyList();
    }

}
