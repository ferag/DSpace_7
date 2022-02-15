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
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.dspace.perucris.externalservices.reniec.UpdateItemWithInformationFromReniecService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for RENIEC
 *
 * @author Mykhaylo Boychuk - (mykhaylo.boychuk at 4science.it)
 */
public class ReniecUpdateIT extends AbstractControllerIntegrationTest {

    @Autowired
    ReniecProvider reniecProvider;

    @Autowired
    UpdateItemWithInformationFromReniecService updateItemWithInformationFromReniec;

    @Test
//    @Ignore("external services temporarily not available")
    public void updateItemWithInformationFromReniecByCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("EDWIN")
                                     .withPersonIdentifierLastName("SAUCEDO")
                                     .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("JAIME")
                                      .withDNI("41918939").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", col1.getID().toString(), "-s", "reniec"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918979")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender'].[0].value", is("m")))
                .andExpect(jsonPath("$.metadata['person.birthDate'].[0].value", is("14-08-1983")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno'].[0].value", is("ASCONA")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno'].[0].value", is("SAUCEDO")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].authority", is("150132")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].value",
                    is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].authority", is("150132")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].value",
                    is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("EDWIN MANUEL")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.direccion'].[0].value",
                        is("MZ.7 LT.8 ASENT.H.19 DE ABRIL CANTO GRANDE")))
                .andExpect(
                        jsonPath("$.metadata['perucris.domicilio.distrito'].[0].value", is("SAN JUAN DE LURIGANCHO")))
                .andExpect(
                        jsonPath("$.metadata['perucris.nacimiento.distrito'].[0].value", is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoCasada']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender'].[0].value", is("f")))
                .andExpect(jsonPath("$.metadata['person.birthDate'].[0].value", is("09-11-1982")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno'].[0].value", is("SANCHEZ")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno'].[0].value", is("ALIAGA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].value", is("PUEBLO LIBRE")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].authority", is("150121")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region'].[0].value", is("CAJAMARCA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].authority", is("060301")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("SHEYLA JULISSA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0].value", is("PUEBLO LIBRE")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoCasada']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.direccion'].[0].value",
                                 is("SENDA DORADA 101 URB.ARCO IRIS")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918939")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("JAIME")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender']").doesNotExist())
                .andExpect(jsonPath("$.metadata['person.birthDate']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0]").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito']").doesNotExist());
    }

    @Test
//    @Ignore("external services temporarily not available")
    public void updateAllItemsWithInformationFromReniecTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Person")
                                           .withName("Collection 2").build();

        Item itemPersonA = ItemBuilder.createItem(context, col1)
                                     .withPersonIdentifierFirstName("EDWIN")
                                     .withPersonIdentifierLastName("SAUCEDO")
                                     .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("JAIME")
                                      .withDNI("41918939").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", "", "-s", "reniec"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918979")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender'].[0].value", is("m")))
                .andExpect(jsonPath("$.metadata['person.birthDate'].[0].value", is("14-08-1983")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno'].[0].value", is("ASCONA")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno'].[0].value", is("SAUCEDO")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].value",
                    is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].authority", is("150132")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].value",
                    is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].authority", is("150132")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("EDWIN MANUEL")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.direccion'].[0].value",
                        is("MZ.7 LT.8 ASENT.H.19 DE ABRIL CANTO GRANDE")))
                .andExpect(
                        jsonPath("$.metadata['perucris.domicilio.distrito'].[0].value", is("SAN JUAN DE LURIGANCHO")))
                .andExpect(
                        jsonPath("$.metadata['perucris.nacimiento.distrito'].[0].value", is("SAN JUAN DE LURIGANCHO")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoCasada']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender'].[0].value", is("f")))
                .andExpect(jsonPath("$.metadata['person.birthDate'].[0].value", is("09-11-1982")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno'].[0].value", is("SANCHEZ")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno'].[0].value", is("ALIAGA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].authority", is("150121")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].value", is("PUEBLO LIBRE")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region'].[0].value", is("CAJAMARCA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].authority", is("060301")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("SHEYLA JULISSA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0].value", is("PUEBLO LIBRE")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito'].[0].value", is("CELENDIN")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoCasada']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.direccion'].[0].value",
                                 is("SENDA DORADA 101 URB.ARCO IRIS")));

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918939")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender'].[0].value", is("m")))
                .andExpect(jsonPath("$.metadata['person.birthDate'].[0].value", is("24-01-1982")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno'].[0].value", is("NUÑEZ")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno'].[0].value", is("BASILIO")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].authority", is("150110")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec'].[0].value", is("COMAS")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region'].[0].value", is("LIMA")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].authority", is("150110")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec'].[0].value", is("COMAS")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("JAIME JHONI")))
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0].value", is("COMAS")))
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito'].[0].value", is("COMAS")))
                .andExpect(jsonPath("$.metadata['perucris.apellidoCasada']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.direccion'].[0].value",
                                 is("JR.HUAYNA CAPAC 250 P.JOVEN COLLIQUE")));
    }

    @Test
    @Ignore("external services temporarily not available")
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
                                     .withPersonIdentifierFirstName("EDWIN")
                                     .withPersonIdentifierLastName("SAUCEDO")
                                     .withDNI("41918979").build();

        Item itemPersonB = ItemBuilder.createItem(context, col1)
                                      .withPersonIdentifierFirstName("Sheyla")
                                      .withPersonIdentifierLastName("Aliaga")
                                      .withDNI("41918999").build();

        Item itemPersonC = ItemBuilder.createItem(context, col2)
                                      .withPersonIdentifierFirstName("JAIME")
                                      .withDNI("41918939").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", UUID.randomUUID().toString(), "-s", "reniec"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/items/" + itemPersonA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonA.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918979")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("EDWIN")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender']").doesNotExist())
                .andExpect(jsonPath("$.metadata['person.birthDate']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0]").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonB.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonB.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918999")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("Sheyla")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender']").doesNotExist())
                .andExpect(jsonPath("$.metadata['person.birthDate']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0]").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + itemPersonC.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", Matchers.is(itemPersonC.getID().toString())))
                .andExpect(jsonPath("$.metadata['perucris.identifier.dni'].[0].value", is("41918939")))
                .andExpect(jsonPath("$.metadata['person.givenName'].[0].value", is("JAIME")))
                .andExpect(jsonPath("$.metadata['oairecerif.person.gender']").doesNotExist())
                .andExpect(jsonPath("$.metadata['person.birthDate']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoMaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.apellidoPaterno']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.provincia']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.region']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.ubigeoReniec']").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.domicilio.distrito'].[0]").doesNotExist())
                .andExpect(jsonPath("$.metadata['perucris.nacimiento.distrito']").doesNotExist());
    }

}
