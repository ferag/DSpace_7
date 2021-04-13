/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static java.util.Arrays.asList;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.edit.EditItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.DefaultFilter;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.logic.condition.RequiredMetadataCondition;
import org.dspace.content.logic.operator.Or;
import org.dspace.content.service.ItemService;
import org.dspace.utils.DSpace;
import org.dspace.validation.model.ValidationError;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for {@link LogicalStatementValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class LogicalStatementValidatorIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Community community;

    private Collection collection;

    @Before
    public void beforeTests() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community)
            .withEntityType("Publication")
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testValidationPassed() throws Exception {

        String doiPath = "/sections/publication/dc.identifier.doi";
        String issnPath = "/sections/publication/dc.identifier.issn";

        LogicalStatementValidator validator = new LogicalStatementValidator();
        validator.setErrorKey("error.validation.test");
        validator.setFilter(buildAtLeastOneMetadataRequiredFilter("dc.identifier.doi", "dc.identifier.issn"));
        validator.setPaths(asList(doiPath, issnPath));

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, collection)
            .withDoiIdentifier("xxx")
            .build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, workspaceItem, getSubmissionConfig(collection));
        assertThat(errors, empty());

    }

    @Test
    public void testValidationFailsWithPaths() throws Exception {

        String doiPath = "/sections/publication/dc.identifier.doi";
        String issnPath = "/sections/publication/dc.identifier.issn";

        LogicalStatementValidator validator = new LogicalStatementValidator();
        validator.setErrorKey("error.validation.test");
        validator.setFilter(buildAtLeastOneMetadataRequiredFilter("dc.identifier.doi", "dc.identifier.issn"));
        validator.setPaths(asList(doiPath, issnPath));

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, collection)
            .build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, workspaceItem, getSubmissionConfig(collection));
        assertThat(errors, hasSize(1));

        ValidationError error = errors.get(0);
        assertThat(error.getMessage(), equalTo("error.validation.test"));
        assertThat(error.getPaths(), contains(doiPath, issnPath));

    }

    @Test
    public void testValidationFailsWithMetadataFields() throws Exception {

        String doiPath = "/sections/publication/dc.identifier.doi";
        String issnPath = "/sections/publication/dc.identifier.issn";

        LogicalStatementValidator validator = new LogicalStatementValidator();
        validator.setErrorKey("error.validation.test");
        validator.setFilter(buildAtLeastOneMetadataRequiredFilter("dc.identifier.doi", "dc.identifier.issn"));
        validator.setMetadataFields(asList("dc.identifier.doi", "dc.identifier.issn"));

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, collection)
            .build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, workspaceItem, getSubmissionConfig(collection));
        assertThat(errors, hasSize(1));

        ValidationError error = errors.get(0);
        assertThat(error.getMessage(), equalTo("error.validation.test"));
        assertThat(error.getPaths(), contains(doiPath, issnPath));

    }

    @Test
    public void testValidationFailsWithMetadataFieldsOnDifferentStep() throws Exception {
        String doiPath = "/sections/publication/dc.identifier.doi";
        String languagePath = "/sections/publication_indexing/dc.language.iso";

        LogicalStatementValidator validator = new LogicalStatementValidator();
        validator.setErrorKey("error.validation.test");
        validator.setFilter(buildAtLeastOneMetadataRequiredFilter("dc.identifier.doi", "dc.language.iso"));
        validator.setMetadataFields(asList("dc.identifier.doi", "dc.language.iso"));

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, collection)
            .build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, workspaceItem, getSubmissionConfig(collection));
        assertThat(errors, hasSize(1));

        ValidationError error = errors.get(0);
        assertThat(error.getMessage(), equalTo("error.validation.test"));
        assertThat(error.getPaths(), contains(doiPath, languagePath));
    }

    @Test
    public void testPersonWithoutIdsValidation() throws Exception {

        LogicalStatementValidator validator = getValidatorByName("personHasAtLeastOneIdValidation");

        context.turnOffAuthorisationSystem();

        Collection personCollection = createCollection(context, community)
            .withEntityType("Person")
            .withSubmissionDefinition("traditional")
            .withAdminGroup(eperson)
            .build();

        WorkspaceItem itemWithOrcid = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withOrcidIdentifier("0000-0002-9079-593X")
            .build();

        WorkspaceItem itemWithDni = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withDniIdentifier("DNI-01")
            .build();

        WorkspaceItem itemWithDina = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withDinaIdentifier("DINA-01")
            .build();

        WorkspaceItem itemWithRenacyt = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withRenacytIdentifier("Renacyt-01")
            .build();

        WorkspaceItem itemWithScopusAuthor = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withScopusIdentifier("Scopus-01")
            .build();

        WorkspaceItem itemWithResearcherId = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withResearcherIdentifier("R-01")
            .build();

        WorkspaceItem itemWithoutIds = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .build();

        context.restoreAuthSystemState();

        SubmissionConfig submissionConfig = getSubmissionConfig(personCollection);
        assertThat(validator.validate(context, itemWithOrcid, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithDni, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithDina, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithRenacyt, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithScopusAuthor, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithResearcherId, submissionConfig), empty());

        List<ValidationError> errors = validator.validate(context, itemWithoutIds, submissionConfig);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), equalTo("error.validation.personIdRequired"));
        assertThat(errors.get(0).getPaths(),
            contains("/section/person/person.identifier.orcid",
                "/section/person/perucris.identifier.dni",
                "/section/person/perucris.identifier.dina",
                "/section/person/perucris.identifier.renacyt",
                "/section/person/person.identifier.scopus-author-id",
                "/section/person/person.identifier.rid"));

    }

    @Test
    public void testInstitutionPersonWithoutIdsValidation() throws Exception {

        LogicalStatementValidator validator = getValidatorByName("personHasAtLeastOneIdValidation");

        context.turnOffAuthorisationSystem();

        Collection personCollection = createCollection(context, community)
            .withEntityType("InstitutionPerson")
            .withSubmissionDefinition("traditional")
            .withAdminGroup(eperson)
            .build();

        WorkspaceItem itemWithOrcid = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withOrcidIdentifier("0000-0002-9079-593X")
            .build();

        WorkspaceItem itemWithDni = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withDniIdentifier("DNI-01")
            .build();

        WorkspaceItem itemWithDina = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withDinaIdentifier("DINA-01")
            .build();

        WorkspaceItem itemWithRenacyt = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withRenacytIdentifier("Renacyt-01")
            .build();

        WorkspaceItem itemWithScopusAuthor = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withScopusIdentifier("Scopus-01")
            .build();

        WorkspaceItem itemWithResearcherId = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .withResearcherIdentifier("R-01")
            .build();

        WorkspaceItem itemWithoutIds = WorkspaceItemBuilder
            .createWorkspaceItem(context, personCollection)
            .build();

        context.restoreAuthSystemState();

        SubmissionConfig submissionConfig = getSubmissionConfig(personCollection);
        assertThat(validator.validate(context, itemWithOrcid, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithDni, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithDina, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithRenacyt, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithScopusAuthor, submissionConfig), empty());
        assertThat(validator.validate(context, itemWithResearcherId, submissionConfig), empty());

        List<ValidationError> errors = validator.validate(context, itemWithoutIds, submissionConfig);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), equalTo("error.validation.personIdRequired"));
        assertThat(errors.get(0).getPaths(),
            contains("/section/person/person.identifier.orcid",
                "/section/person/perucris.identifier.dni",
                "/section/person/perucris.identifier.dina",
                "/section/person/perucris.identifier.renacyt",
                "/section/person/person.identifier.scopus-author-id",
                "/section/person/person.identifier.rid"));

    }

    @Test
    public void testCvEditPeruvianWithUbigeoValidation() throws Exception {

        LogicalStatementValidator validator = getValidatorByName("peruvianHasUbigeoSet");

        context.turnOffAuthorisationSystem();

        Collection personCollection = createCollection(context, community)
            .withEntityType("CvPerson")
            .withSubmissionDefinition("cvperson")
            .withAdminGroup(eperson)
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withMetadata("perucris" , "address" ,"addressCountry" ,
                "US")
        .build();

        EditItem usResident = EditItem.none(context, ItemBuilder.createItem(context, personCollection)
            .withMetadata("perucris" , "address" ,"addressCountry" ,
                "US")
            .build());

        EditItem peruvianWithUbigeo = EditItem.none(context, ItemBuilder.createItem(context, personCollection)
            .withMetadata("perucris" , "address" ,"addressCountry" ,
                "PE")
            .withMetadata("perucris", "ubigeo", null, "123456")
            .build());

        EditItem peruvianWithoutUbigeo = EditItem.none(context, ItemBuilder.createItem(context, personCollection)
            .withMetadata("perucris" , "address" ,"addressCountry" ,
                "PE")
            .build());


        context.restoreAuthSystemState();

        SubmissionConfig submissionConfig = getSubmissionConfig(personCollection);
        assertThat(validator.validate(context, usResident, submissionConfig), empty());
        assertThat(validator.validate(context, peruvianWithUbigeo, submissionConfig), empty());

        List<ValidationError> errors = validator.validate(context, peruvianWithoutUbigeo, submissionConfig);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), equalTo("error.validation.ubigeoRequired"));
        assertThat(errors.get(0).getPaths(),
            contains("/sections/cv-person/perucris.ubigeo"));

    }

    private LogicalStatementValidator getValidatorByName(String name) {
        return new DSpace().getServiceManager().getServiceByName(name, LogicalStatementValidator.class);
    }

    private SubmissionConfig getSubmissionConfig(Collection collection) throws SubmissionConfigReaderException {
        return new SubmissionConfigReader().getSubmissionConfigByCollection(collection);
    }

    private Filter buildAtLeastOneMetadataRequiredFilter(String... fields) {
        DefaultFilter filter = new DefaultFilter();
        List<LogicalStatement> statements = Arrays.stream(fields)
            .map(this::buildRequiredMetadataCondition)
            .collect(Collectors.toList());
        filter.setStatement(new Or(statements));
        return filter;
    }

    private RequiredMetadataCondition buildRequiredMetadataCondition(String metadataField) {
        RequiredMetadataCondition condition = new RequiredMetadataCondition();
        condition.setItemService(itemService);
        try {
            condition.setParameters(Map.of("field", metadataField));
        } catch (LogicalStatementException e) {
            throw new RuntimeException(e);
        }
        return condition;
    }
}
