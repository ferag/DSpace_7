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
public class SuneduUpdateIT extends AbstractControllerIntegrationTest {

    @Test
    public void updateItemWithInformationFromSuneduByCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("41918988").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update", "-i", col1.getID().toString(), "-s", "sunedu"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918979")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value", is("INGENIERO DE SISTEMAS")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Titulo profesional")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[0].value", is("PE")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[0].value",
                                 is("UNIVERSIDAD NACIONAL DE INGENIERÍA")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value",
                                 is("TITULO PROFESIONAL DE LICENCIADO EN ADMINISTRACION")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[1].value", is("BACHILLER EN ADMINISTRACION")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Titulo profesional")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[1].value", is("Bachiller")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[0].value",
                                 is("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[1].value",
                                 is("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[0].value", is("PE")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[1].value", is("PE")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918988")))
                .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.education.role']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.grantor']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.country']").doesNotExist());
    }

    @Test
    public void updateAllItemsWithInformationFromSuneduTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("41918988").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update", "-i", "", "-s", "sunedu"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918979")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value", is("INGENIERO DE SISTEMAS")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Titulo profesional")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[0].value", is("PE")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[0].value",
                                 is("UNIVERSIDAD NACIONAL DE INGENIERÍA")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value",
                                 is("TITULO PROFESIONAL DE LICENCIADO EN ADMINISTRACION")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[1].value", is("BACHILLER EN ADMINISTRACION")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Titulo profesional")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[1].value", is("Bachiller")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[0].value",
                                 is("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[1].value",
                                 is("UNIVERSIDAD PRIVADA NORBERT WIENER S.A.")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[0].value", is("PE")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[1].value", is("PE")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918988")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value",
                        is("LICENCIADO EN ARTE CON MENCIÓN EN PINTURA")))
                .andExpect(jsonPath("$.metadata['crisrp.education'].[1].value",
                        is("BACHILLER EN ARTE, ESPECIALIDAD: CON MENCION EN PINTURA")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Titulo profesional")))
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[1].value", is("Bachiller")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[0].value",
                        is("PONTIFICIA UNIVERSIDAD CATÓLICA DEL PERÚ")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor'].[1].value",
                        is("PONTIFICIA UNIVERSIDAD CATÓLICA DEL PERÚ")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[0].value", is("PE")))
                .andExpect(jsonPath("$.metadata['perucris.education.country'].[1].value", is("PE")));
    }

    @Test
    public void updateItemsWithInformationFromReniecByWrongIDColTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withAbreviaturaTitulo("Bachiller")
                                      .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("DIEGO")
                                      .withPersonIdentifierLastName("BEJAR")
                                      .withDNI("41918988").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update", "-i", UUID.randomUUID().toString(), "-s", "sunedu"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.education.role'].[0].value", is("Bachiller")))
                .andExpect(jsonPath("$.metadata['perucris.education.grantor']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.country']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.education.role']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.grantor']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.country']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918988")))
                .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                .andExpect(jsonPath("$.metadata['crisrp.education.role']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.grantor']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.education.country']").doesNotExist());
    }
}