/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests to verify that proper discovery queries return only specific community Items.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DiscoveryCommunityRelatedItemsIT extends AbstractControllerIntegrationTest {


    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;


    @Test
    public void onlyGivenCommunityItems() throws Exception {

        context.turnOffAuthorisationSystem();

        Community displayedCommunity = CommunityBuilder.createCommunity(context).withName("displayed community")
            .build();
        Community notDisplayedCommunity = CommunityBuilder.createCommunity(context).withName("not displayed community")
            .build();

        Collection displayedCollection =
            CollectionBuilder.createCollection(context, displayedCommunity).withName("Displayed publications")
                .withEntityType("Publication").build();

        Collection notDisplayedCollection =
            CollectionBuilder.createCollection(context, notDisplayedCommunity).withName("Not displayed publications")
                .withEntityType("ItemPublication").build();


        Item displayedItem = ItemBuilder.createItem(context, displayedCollection)
            .withTitle("Item displayed")
            .withIssueDate("2020-12-18")
            .withAuthor("Smith, John")
            .build();
        Item notDisplayedItem = ItemBuilder.createItem(context, notDisplayedCollection)
            .withTitle("Item not displayed")
            .withIssueDate("2000-12-18")
            .withAuthor("Doe, Jane")
            .build();


        for (Map.Entry<String, DiscoveryConfiguration> entry : discoveryConfigurationService.getMap()
            .entrySet()) {
            for (String filterQuery : entry.getValue().getDefaultFilterQueries()) {
                if (filterQuery.contains("directorios-id")) {
                    entry.getValue().getDefaultFilterQueries().remove(filterQuery);
                    filterQuery = filterQuery.replace("directorios-id", displayedCommunity.getID().toString());
                    entry.getValue().getDefaultFilterQueries().add(filterQuery);
                }
            }
        }

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/discover/search/objects")
            .param("configuration", "researchoutputs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(
                jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                    is(displayedItem.getID().toString())));

    }
}
