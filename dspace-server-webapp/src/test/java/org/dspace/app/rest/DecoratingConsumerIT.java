/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test class for the decorating consumer {@DecoratingConsumer}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class DecoratingConsumerIT extends AbstractControllerIntegrationTest {

    @Test
    public void decoratingProjectTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("asdf")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 1")
                                   .withOrgUnitLegalName("LegalName of orgUnit 1")
                                   .withAcronym("Acronym of orgUnit 1")
                                   .build();

        Item orgUnit2 = ItemBuilder.createItem(context, collection)
                                   .withTitle("Test university")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 2")
                                   .withOrgUnitLegalName("LegalName of orgUnit 2")
                                   .withAcronym("Acronym of orgUnit 2")
                                   .build();

        Item orgUnit3 = ItemBuilder.createItem(context, collection)
                                   .withTitle("test ipi")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 3")
                                   .withOrgUnitLegalName("LegalName of orgUnit 3")
                                   .withAcronym("Acronym of orgUnit 3")
                                   .build();

        Item orgUnit4 = ItemBuilder.createItem(context, collection)
                                   .withTitle("test Organization")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 4")
                                   .withOrgUnitLegalName("LegalName of orgUnit 4")
                                   .withAcronym("Acronym of orgUnit 4")
                                   .build();

        Item orgUnit5 = ItemBuilder.createItem(context, collection)
                                   .withTitle("4Science")
                                   .withOrgUnitCountry("IT")
                                   .withOrgUnitLocality("Milan")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 5")
                                   .withOrgUnitLegalName("LegalName of orgUnit 5")
                                   .withAcronym("Acronym of orgUnit 5")
                                   .build();

        Item project1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("My first project")
                                   .withEntityType("Project")
                                   .withContractorou("asdf", orgUnit1.getID().toString())
                                   .withPartnerou("Test university", orgUnit2.getID().toString())
                                   .withInKindContributorou("test ipi", orgUnit3.getID().toString())
                                   .withOrganization("test Organization", orgUnit4.getID().toString())
                                   .withFunder("4Science", orgUnit5.getID().toString())
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + project1.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadata("perucris.contractorou.titleAlternative",
                                            "AlternativeTitle of orgUnit 1"),
                                       matchMetadata("perucris.contractorou.legalName",
                                            "LegalName of orgUnit 1"),
                                       matchMetadata("perucris.contractorou.acronym",
                                            "Acronym of orgUnit 1"),
                                       matchMetadata("perucris.partnerou.titleAlternative",
                                            "AlternativeTitle of orgUnit 2"),
                                       matchMetadata("perucris.partnerou.legalName",
                                            "LegalName of orgUnit 2"),
                                       matchMetadata("perucris.partnerou.acronym",
                                            "Acronym of orgUnit 2"),
                                       matchMetadata("perucris.inKindContributorou.titleAlternative",
                                            "AlternativeTitle of orgUnit 3"),
                                       matchMetadata("perucris.inKindContributorou.legalName",
                                            "LegalName of orgUnit 3"),
                                       matchMetadata("perucris.inKindContributorou.acronym",
                                            "Acronym of orgUnit 3"),
                                       matchMetadata("perucris.organization.titleAlternative",
                                            "AlternativeTitle of orgUnit 4"),
                                       matchMetadata("perucris.organization.legalName",
                                            "LegalName of orgUnit 4"),
                                       matchMetadata("perucris.organization.acronym",
                                            "Acronym of orgUnit 4"),
                                       matchMetadata("perucris.funder.titleAlternative",
                                            "AlternativeTitle of orgUnit 5"),
                                       matchMetadata("perucris.funder.legalName",
                                            "LegalName of orgUnit 5"),
                                       matchMetadata("perucris.funder.acronym",
                                            "Acronym of orgUnit 5")
                                       )))));
    }

    @Test
    public void decoratingEquipmentTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("asdf")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 1")
                                   .withOrgUnitLegalName("LegalName of orgUnit 1")
                                   .withAcronym("Acronym of orgUnit 1")
                                   .build();

        Item orgUnit2 = ItemBuilder.createItem(context, collection)
                                   .withTitle("university 4")
                                   .withEntityType("OrgUnit")
                                   .withAlternativeTitle("AlternativeTitle of orgUnit 2")
                                   .withOrgUnitLegalName("LegalName of orgUnit 2")
                                   .withAcronym("Acronym of orgUnit 2")
                                   .build();

        Item equipment = ItemBuilder.createItem(context, collection)
                                   .withTitle("My equipment")
                                   .withEntityType("Equipment")
                                   .withContractorou("asdf", orgUnit1.getID().toString())
                                   .withOwnerou("university 4", orgUnit2.getID().toString())
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + equipment.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadata("perucris.contractorou.titleAlternative",
                                            "AlternativeTitle of orgUnit 1"),
                                       matchMetadata("perucris.contractorou.legalName",
                                            "LegalName of orgUnit 1"),
                                       matchMetadata("perucris.contractorou.acronym",
                                            "Acronym of orgUnit 1"),
                                       matchMetadata("perucris.ownerou.titleAlternative",
                                            "AlternativeTitle of orgUnit 2"),
                                       matchMetadata("perucris.ownerou.legalName",
                                            "LegalName of orgUnit 2"),
                                       matchMetadata("perucris.ownerou.acronym",
                                            "Acronym of orgUnit 2")
                                       )))));
    }

    @Test
    public void decoratingProjectWithTitleAlternativeAndLegalNameEmptyTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("asdf")
                                   .withEntityType("OrgUnit")
                                   .withAcronym("Acronym of asdf")
                                   .build();

        Item project1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("My 2 project")
                                   .withEntityType("Project")
                                   .withContractorou("asdf", orgUnit1.getID().toString())
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + project1.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadataDoesNotExist("perucris.contractorou.titleAlternative"),
                                       matchMetadataDoesNotExist("perucris.contractorou.legalName"),
                                       matchMetadata("perucris.contractorou.acronym", "Acronym of asdf")
                                       )))));
    }

    @Test
    public void decoratingProjectWithContractorouAuthorityNotConfiguredTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Root Community")
                                                  .build();

        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        ItemBuilder.createItem(context, collection)
                   .withTitle("asdf")
                   .withEntityType("OrgUnit")
                   .withAlternativeTitle("AlternativeTitle of orgUnit 1")
                   .withOrgUnitLegalName("LegalName of orgUnit 1")
                   .withAcronym("Acronym of orgUnit 1")
                   .build();

        Item project1 = ItemBuilder.createItem(context, collection)
                                   .withTitle("My first project")
                                   .withEntityType("Project")
                                   .withContractorou("asdf", null)
                                   .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + project1.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.allOf(
                                hasJsonPath("$.metadata", Matchers.allOf(
                                       matchMetadataDoesNotExist("perucris.contractorou.titleAlternative"),
                                       matchMetadataDoesNotExist("perucris.contractorou.legalName"),
                                       matchMetadataDoesNotExist("perucris.contractorou.acronym")
                                       )))));
    }

}