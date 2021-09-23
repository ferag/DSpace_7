/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.ConcytecWorkflowRelation;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SolrServiceInstitutionWorkflowFilterPlugin implements SolrServiceIndexPlugin {

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    @SuppressWarnings("rawtypes")
    private WorkflowItemService workflowItemService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        XmlWorkflowItem xmlWorkflowItem = null;
        if (indexableObject instanceof IndexableClaimedTask) {
            ClaimedTask claimedTask = ((IndexableClaimedTask) indexableObject).getIndexedObject();
            if (Objects.nonNull(claimedTask)) {
                xmlWorkflowItem = claimedTask.getWorkflowItem();
            }
        }
        if (indexableObject instanceof IndexablePoolTask) {
            PoolTask poolTask = ((IndexablePoolTask) indexableObject).getIndexedObject();
            if (Objects.nonNull(poolTask)) {
                xmlWorkflowItem = poolTask.getWorkflowItem();
            }
        }
        if (Objects.nonNull(xmlWorkflowItem)) {
            try {
                Item item = xmlWorkflowItem.getItem();
                List<RelationshipType> relationshipTypes = relationshipTypeService.findByItemAndTypeNames(
                                                           context, item, false,
                                                           ConcytecWorkflowRelation.SHADOW_COPY.getLeftType(),
                                                           ConcytecWorkflowRelation.SHADOW_COPY.getRightType());
                if (relationshipTypes.isEmpty()) {
                    return;
                }
                List<Relationship> relationships = relationshipService.findByItemAndRelationshipType(context,
                                                                       item, relationshipTypes.get(0));
                if (!relationships.isEmpty()) {
                    Item leftItem = relationships.get(0).getLeftItem();
                    WorkflowItem workflowItem = workflowItemService.findByItem(context, leftItem);
                    Community com = workflowItem.getCollection().getCommunities().get(0);
                    if (StringUtils.isNotBlank(com.getName())) {
                        document.addField("submitting", com.getName());
                        document.addField("submitting_keyword", com.getName());
                        document.addField("submitting_filter", com.getName());
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}