/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
public class RenacytUpdateIT extends AbstractControllerIntegrationTest {

    @Test
    public void updateItemWithInformationFromRenacytByCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("MARTHA")
                                      .withPersonIdentifierLastName("RUTH")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("06027011").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("ANTONIO")
                                      .withPersonIdentifierLastName("JULIO")
                                      .withDNI("43881726").build();

        Item itemPersonC = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("76047206").build();

        Item itemPersonD = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("KARINA")
                                      .withPersonIdentifierLastName("VANESSA")
                                      .withDNI("10280437").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", col1.getID().toString(), "-s", "renacyt"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("06027011")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("II")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("16-09-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("16-09-2023")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value",
                                    is("Carlos Monge Medrano")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("43881726")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("III")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("16-09-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("16-09-2022")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value", is("María Rostworowski")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("76047206")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonD.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonD.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("10280437")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());
    }

    @Test
    public void updateAllItemsWithInformationFromRenacytTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("MARTHA")
                                      .withPersonIdentifierLastName("RUTH")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("06027011").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("ANTONIO")
                                      .withPersonIdentifierLastName("JULIO")
                                      .withDNI("43881726").build();

        Item itemPersonC = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("76047206").build();

        Item itemPersonD = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("KARINA")
                                      .withPersonIdentifierLastName("VANESSA")
                                      .withDNI("07537170").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", "", "-s", "renacyt"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                            .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("06027011")))
                            .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("II")))
                            .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("16-09-2020")))
                            .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("16-09-2023")))
                            .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value",
                                                is("Carlos Monge Medrano")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("43881726")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("III")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("16-09-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("16-09-2022")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value", is("María Rostworowski")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("76047206")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonD.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonD.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("07537170")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("III")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("07-05-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("07-05-2022")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value",
                                    is("María Rostworowski")));
    }

    @Test
    public void updateItemsWithInformationFromReniecByWrongIDColTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("MARTHA")
                                      .withPersonIdentifierLastName("RUTH")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("06027011").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("ANTONIO")
                                      .withPersonIdentifierLastName("JULIO")
                                      .withDNI("43881726").build();

        Item itemPersonC = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("76047206").build();

        Item itemPersonD = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("KARINA")
                                      .withPersonIdentifierLastName("VANESSA")
                                      .withDNI("10280437").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", UUID.randomUUID().toString(), "-s", "renacyt"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("06027011")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("43881726")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("76047206")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonD.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonD.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("10280437")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit']").doesNotExist());
    }

    @Test
    public void updateItemByCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("MARTHA")
                                      .withPersonIdentifierLastName("RUTH")
                                      .withPersonQualification("I")
                                      .withPersonQualificationStartDate("01-01-2018")
                                      .withPersonQualificationEndDate("01-01-2021")
                                      .withPersonQualificationOrgUnit("X-Group")
                                      .withDNI("06027011").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("KARINA")
                                      .withPersonIdentifierLastName("VANESSA")
                                      .withDNI("40202098").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", col1.getID().toString(), "-s", "renacyt"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("06027011")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("I")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[1].value", is("II")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("01-01-2018")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[1].value", is("16-09-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("01-01-2021")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[1].value", is("16-09-2023")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value", is("X-Group")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[1].value",
                                    is("Carlos Monge Medrano")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("40202098")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification'].[0].value", is("II")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.start'].[0].value", is("31-12-2020")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.end'].[0].value", is("31-12-2023")))
                .andExpect(jsonPath("$.metadata['crisrp.qualification.orgunit'].[0].value",
                                    is("María Rostworowski")));

    }

}
