/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom workspace item generation from template for PeruCRIS integration test
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class WorkspaceItemFromTemplateGeneratorIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    public void createSingleWorkspaceItemWithTemplate() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .withRelationshipType("Publication")
            .withSubmissionDefinition("traditional")
            .withTemplateItem()
            .withSubmitterGroup(eperson)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context).withAction(Constants.ADMIN)
            .withDspaceObject(col1)
            .withGroup(eperson.getGroups().get(0));

        itemService.addMetadata(context, col1.getTemplateItem(), "dc", "title", null, null, "SimpleTitle");
        itemService.addMetadata(context, col1.getTemplateItem(), "dc", "date", "issued", null, "###DATE.yyyy-MM-dd###");
        itemService.addMetadata(context, col1.getTemplateItem(), "dc", "contributor", "author", null,
            "###RESPOLICY.admin.epersongroup###");
        itemService.addMetadata(context, col1.getTemplateItem(), "dc", "contributor", "editor", null,
            "###RESPOLICY.default_item_read.invalid###");

        String authToken = getAuthToken(eperson.getEmail(), password);

        context.restoreAuthSystemState();

        final String today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now());

        getClient(authToken).perform(post("/api/submission/workspaceitems")
            .param("owningCollection", col1.getID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$._embedded.item.metadata['dc.title'][0].value", is("SimpleTitle")))
            .andExpect(jsonPath("$._embedded.item.metadata['dc.date.issued'][0].value",
                is(today)))
            .andExpect(jsonPath("$._embedded.item.metadata['dc.contributor.author'][0].value",
                is(eperson.getGroups().get(0).getName())))
            .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.item.metadata['dc.contributor.editor']")))
            .andExpect(jsonPath("$._embedded.collection.id", is(col1.getID().toString())));
    }
}
