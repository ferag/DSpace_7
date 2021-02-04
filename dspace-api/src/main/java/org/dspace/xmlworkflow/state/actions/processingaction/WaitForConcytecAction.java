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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class WaitForConcytecAction extends ProcessingAction {

    @Override
    public void activate(Context context, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
