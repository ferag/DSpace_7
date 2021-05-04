/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import static java.util.List.of;
import static org.dspace.app.matcher.MetadataCorrectionMatcher.additionOf;
import static org.dspace.app.matcher.MetadataCorrectionMatcher.editOf;
import static org.dspace.app.matcher.MetadataCorrectionMatcher.removalOf;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.versioning.model.MetadataCorrection.metadataAddition;
import static org.dspace.versioning.model.MetadataCorrection.metadataModification;
import static org.dspace.versioning.model.MetadataCorrection.metadataRemoval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.CrisConstants;
import org.dspace.utils.DSpace;
import org.dspace.versioning.model.ItemCorrection;
import org.dspace.versioning.model.MetadataCorrection;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link ItemCorrectionService}
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemCorrectionServiceIT extends AbstractIntegrationTestWithDatabase {

    private ItemCorrectionService itemCorrectionService = getItemCorrectionService();

    private RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Community community;

    private Collection collection;

    private RelationshipType correctionRelationType;

    @Before
    public void beforeTests() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(admin);

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        correctionRelationType = createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1).build();

        community = createCommunity(context).build();

        collection = createCollection(context, community)
            .withEntityType("Publication")
            .withSubmissionDefinition("publication")
            .withWorkflowGroup(1, admin)
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCreateWorkspaceItemAndRelationshipByItem() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter", "6238d077-059a-4207-a075-c9ec79709945")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withCrisOwner("User", "9912fdb6-b817-4c56-aac3-54f612c3031a")
            .build();

        context.restoreAuthSystemState();

        WorkspaceItem correctionWorkspaceItem = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context,
            item.getID(), "isCorrectionOfItem");

        assertThat(correctionWorkspaceItem, notNullValue());

        Item correctionItem = correctionWorkspaceItem.getItem();
        assertThat(correctionItem, notNullValue());

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertThat(relationships, hasSize(1));
        assertThat(relationships.get(0).getLeftItem(), equalTo(correctionItem));
        assertThat(relationships.get(0).getRightItem(), equalTo(item));

        List<MetadataValue> correctionValues = correctionItem.getMetadata();
        assertThat(correctionValues, hasItem(with("dc.title", "Test publication")));
        assertThat(correctionValues, hasItem(with("dspace.entity.type", "Publication")));
        assertThat(correctionValues, hasItem(with("dc.date.issued", "2020-01-01")));
        assertThat(correctionValues, hasItem(with("dc.contributor.author", "White, Walter", null,
            "6238d077-059a-4207-a075-c9ec79709945", 0, 600)));
        assertThat(correctionValues, hasItem(with("oairecerif.author.affiliation",
            "4Science", null, null, 0, 400)));
        assertThat(correctionValues, hasItem(with("dc.contributor.author",
            "Jesse Pinkman", null, null, 1, 400)));
        assertThat(correctionValues, hasItem(with("oairecerif.author.affiliation",
            CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE, null, null, 1, 400)));
        assertThat(correctionValues, hasItem(with("cris.owner", "User", null,
            "9912fdb6-b817-4c56-aac3-54f612c3031a", 0, 600)));
    }

    @Test
    public void testReplaceCorrectionItemWithNative() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter", "6238d077-059a-4207-a075-c9ec79709945")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withCrisOwner("User", "9912fdb6-b817-4c56-aac3-54f612c3031a")
            .build();

        XmlWorkflowItem correctionWorkflowItem = WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle("Test publication correction")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withSubject("test subject")
            .build();

        Item correctionItem = correctionWorkflowItem.getItem();

        RelationshipBuilder.createRelationshipBuilder(context, correctionItem, item, correctionRelationType).build();

        context.restoreAuthSystemState();

        itemCorrectionService.replaceCorrectionItemWithNative(context, correctionWorkflowItem);

        correctionItem = context.reloadEntity(correctionItem);
        assertThat(correctionItem, nullValue());

        correctionWorkflowItem = context.reloadEntity(correctionWorkflowItem);
        assertThat(correctionWorkflowItem, notNullValue());
        assertThat(correctionWorkflowItem.getItem(), equalTo(item));

        item = context.reloadEntity(item);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasItem(with("dc.title", "Test publication correction")));
        assertThat(values, hasItem(with("dspace.entity.type", "Publication")));
        assertThat(values, hasItem(with("dc.date.issued", "2020-01-01")));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", null, null, 0, 400)));
        assertThat(values, hasItem(with("dc.subject", "test subject")));

        assertThat(getMetadataValues(item, "dc.contributor.author"), hasSize(1));
        assertThat(getMetadataValues(item, "oairecerif.author.affiliation"), empty());

    }

    @Test
    public void testGetAppliedCorrections() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter", "6238d077-059a-4207-a075-c9ec79709945")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withCrisOwner("User", "9912fdb6-b817-4c56-aac3-54f612c3031a")
            .build();

        Item correctionItem = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter", "6238d077-059a-4207-a075-c9ec79709945")
            .withSubject("test subject")
            .withSubject("another test subject")
            .build();

        context.restoreAuthSystemState();

        ItemCorrection itemCorrection = itemCorrectionService.getAppliedCorrections(context, item, correctionItem);
        assertThat(itemCorrection, notNullValue());

        List<MetadataCorrection> corrections = itemCorrection.getMetadataCorrections();
        assertThat(corrections, hasItem(removalOf("cris.owner")));
        assertThat(corrections, hasItem(removalOf("oairecerif.author.affiliation")));
        assertThat(corrections, hasItem(additionOf("dc.subject", of(metadataValueDto("dc.subject", "test subject"),
            metadataValueDto("dc.subject", "another test subject")))));
        assertThat(corrections, hasItem(editOf("dc.contributor.author", of(metadataValueDto("dc.contributor.author",
            "White, Walter", "6238d077-059a-4207-a075-c9ec79709945", 600)))));

    }

    @Test
    public void testApplyCorrectionsOnItem() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter", "6238d077-059a-4207-a075-c9ec79709945")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withCrisOwner("User", "9912fdb6-b817-4c56-aac3-54f612c3031a")
            .build();

        context.restoreAuthSystemState();

        ItemCorrection correction = new ItemCorrection();
        correction.addMetadataCorrection(metadataRemoval("dc.date.issued"));
        correction.addMetadataCorrection(metadataAddition("dc.contributor.editor",
            of(metadataValueDto("dc.contributor.editor", "Test editor", "9912fdb6-b817-4c56-aac3-54f612c3031a", 600))));
        correction.addMetadataCorrection(metadataAddition("dc.subject",
            of(metadataValueDto("dc.subject", "test subject"))));
        correction.addMetadataCorrection(metadataModification("dc.contributor.author",
            of(metadataValueDto("dc.contributor.author", "Mario Rossi"))));
        correction.addMetadataCorrection(metadataModification("oairecerif.author.affiliation",
            of(metadataValueDto("oairecerif.author.affiliation", CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE))));

        itemCorrectionService.applyCorrectionsOnItem(context, item, correction);

        item = context.reloadEntity(item);
        assertThat(item, notNullValue());

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasItem(with("dc.title", "Test publication")));
        assertThat(values, hasItem(with("dspace.entity.type", "Publication")));
        assertThat(values, hasItem(with("dc.contributor.author", "Mario Rossi")));
        assertThat(values, hasItem(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItem(with("dc.subject", "test subject")));
        assertThat(values, hasItem(with("dc.contributor.editor", "Test editor", null,
            "9912fdb6-b817-4c56-aac3-54f612c3031a", 0, 600)));

        assertThat(getMetadataValues(item, "dc.date.issued"), empty());
        assertThat(getMetadataValues(item, "dc.contributor.author"), hasSize(1));
        assertThat(getMetadataValues(item, "oairecerif.author.affiliation"), hasSize(1));
    }

    private MetadataValueDTO metadataValueDto(String metadataField, String value, String authority, int confidence) {
        MetadataValueDTO metadataValueDto = metadataValueDto(metadataField, value);
        metadataValueDto.setAuthority(authority);
        metadataValueDto.setConfidence(confidence);
        return metadataValueDto;
    }

    private MetadataValueDTO metadataValueDto(String metadataField, String value) {
        String[] sections = StringUtils.split(metadataField, ".");
        if (sections.length == 3) {
            return new MetadataValueDTO(sections[0], sections[1], sections[2], null, value);
        }
        return new MetadataValueDTO(sections[0], sections[1], null, null, value);
    }

    private List<MetadataValue> getMetadataValues(Item item, String field) {
        return itemService.getMetadataByMetadataString(item, field);
    }

    private ItemCorrectionService getItemCorrectionService() {
        return new DSpace().getSingletonService(ItemCorrectionService.class);
    }
}
