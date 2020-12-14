/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static org.dspace.content.Item.ANY;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemCopyService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class of an action that create a shadow copy of the given item
 * into the Directorio or update that copy if the item was already archived.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ShadowCopyAction extends ProcessingAction {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemCopyService itemCopyService;

    @Autowired
    private WorkflowService<XmlWorkflowItem> workflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Override
    public void activate(Context context, XmlWorkflowItem workflowItem) {

    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, WorkflowException, AuthorizeException, IOException {

        Item item = workflowItem.getItem();

        Collection directorioCollection = findDirectorioCollectionByRelationshipType(context, item);

        WorkspaceItem shadowWorkspaceItemCopy = itemCopyService.copy(context, item, directorioCollection);

        Item shadowItemCopy = shadowWorkspaceItemCopy.getItem();
        concytecWorkflowService.createShadowRelationship(context, item, shadowItemCopy);

        workflowService.start(context, shadowWorkspaceItemCopy);

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private Collection findDirectorioCollectionByRelationshipType(Context context, Item item)
        throws SQLException, WorkflowException {

        Community directorio = findDirectorioCommunity(context);

        Predicate<Collection> relationshipTypePredicate = (collection) -> hasSameRelatioshipType(collection, item);
        List<Collection> collections = communityService.getCollections(context, directorio, relationshipTypePredicate);
        if (CollectionUtils.isEmpty(collections)) {
            throw new WorkflowException("No directorio collection found for the shadow copy of item " + item.getID());
        }

        return collections.get(0);
    }

    private Community findDirectorioCommunity(Context context) throws WorkflowException, SQLException {
        UUID directorioId = UUIDUtils.fromString(configurationService.getProperty("directorios.community-id"));
        if (directorioId == null) {
            throw new WorkflowException("Invalid directorios.community-id set");
        }
        return communityService.find(context, directorioId);
    }

    private boolean hasSameRelatioshipType(Collection collection, Item item) {
        String collectionType = collectionService.getMetadataFirstValue(collection, "relationship", "type", null, ANY);
        String itemType = itemService.getMetadataFirstValue(item, "relationship", "type", null, ANY);
        return Objects.equals(collectionType, itemType);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

}
