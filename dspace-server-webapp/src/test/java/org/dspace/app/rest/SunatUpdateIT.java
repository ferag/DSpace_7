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
 * Integration test class for SUNAT service
 *
 * @author Mykhaylo Boychuk - (mykhaylo.boychuk at 4science.it)
 */
public class SunatUpdateIT extends AbstractControllerIntegrationTest {

    @Test
    public void updateItemWithInformationFromSunatByCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 2").build();

        Item orgUnitA = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item A")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20172627421").build();

        Item orgUnitB = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item B")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20101247865").build();

        Item orgUnitC = ItemBuilder.createItem(context, col2)
                                   .withTitle("Title item C")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20102982372").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", col1.getID().toString(), "-s", "sunat"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        // verify that the changes have been persisted
        getClient(authToken).perform(get("/api/core/items/" + orgUnitA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitA.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item A")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20172627421")))
                 .andExpect(jsonPath("$.metadata['organization.legalName'].[0].value", is("UNIVERSIDAD DE PIURA")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].value", is("PIURA")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].authority", is("200101")))
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu'].[0].value", is("80309")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality'].[0].value",
                                  is("RAMON MUGICA,SAN EDUARDO,131,-,SECTOR EL CHIPE,PIURA,PIURA,PIURA")));

        // verify that the changes have been persisted
        getClient(authToken).perform(get("/api/core/items/" + orgUnitB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitB.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item B")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20101247865")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].authority", is("150114")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].value", is("LA MOLINA")))
                 .andExpect(jsonPath("$.metadata['organization.legalName'].[0].value",
                                  is("COOP.SERV.EDUC. ING CARLOS LISSON B LTDA")))
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu'].[0].value", is("80210")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality'].[0].value",
                  is("PETROLEROS,RESIDENCIAL INGENIEROS,141,-,ALT CDRA 60DE JAVIER PRADO ESTE,LIMA,LIMA,LA MOLINA")));

        // verify that Item C have not been changed
        getClient(authToken).perform(get("/api/core/items/" + orgUnitC.getID())).andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitC.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item C")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20102982372")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['organization.legalName']").doesNotExist());
    }

    @Test
    public void updateAllItemsWithInformationFromSunatTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 2").build();

        Item orgUnitA = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item A")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20172627421").build();

        Item orgUnitB = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item B")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20101247865").build();

        Item orgUnitC = ItemBuilder.createItem(context, col2)
                                   .withTitle("Title item C")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20102982372").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", "", "-s", "sunat"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        // verify that the changes have been persisted
        getClient(authToken).perform(get("/api/core/items/" + orgUnitA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitA.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item A")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].value", is("PIURA")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].authority", is("200101")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20172627421")))
                 .andExpect(jsonPath("$.metadata['organization.legalName'].[0].value", is("UNIVERSIDAD DE PIURA")))
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu'].[0].value", is("80309")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality'].[0].value",
                                  is("RAMON MUGICA,SAN EDUARDO,131,-,SECTOR EL CHIPE,PIURA,PIURA,PIURA")));

        // verify that the changes have been persisted
        getClient(authToken).perform(get("/api/core/items/" + orgUnitB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitB.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item B")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].value", is("LA MOLINA")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].authority", is("150114")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20101247865")))
                 .andExpect(jsonPath("$.metadata['organization.legalName'].[0].value",
                                  is("COOP.SERV.EDUC. ING CARLOS LISSON B LTDA")))
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu'].[0].value", is("80210")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality'].[0].value",
                  is("PETROLEROS,RESIDENCIAL INGENIEROS,141,-,ALT CDRA 60DE JAVIER PRADO ESTE,LIMA,LIMA,LA MOLINA")));

        // verify that the patch changes have been persisted
        getClient(authToken).perform(get("/api/core/items/" + orgUnitC.getID())).andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitC.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item C")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].value", is("SULLANA")))
                 .andExpect(jsonPath("$.metadata['perucris.ubigeo.ubigeoSunat'].[0].authority", is("200601")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20102982372")))
                 .andExpect(jsonPath("$.metadata['organization.legalName'].[0].value",
                                  is("INSTITUCION EDUCATIVA SANTA URSULA")))
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu'].[0].value", is("80210")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality'].[0].value",
                                  is("STA. J VEDRUNA,-,1,-,-,PIURA,SULLANA,SULLANA")));
    }

    @Test
    public void updateItemsWithInformationFromSunatByWrongIDColTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("OrgUnit")
                                           .withName("Collection 2").build();

        Item orgUnitA = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item A")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20172627421").build();

        Item orgUnitB = ItemBuilder.createItem(context, col1)
                                   .withTitle("Title item B")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20101247865").build();

        Item orgUnitC = ItemBuilder.createItem(context, col2)
                                   .withTitle("Title item C")
                                   .withOrgUnitCountry("Peru")
                                   .withOrganizationRuc("20102982372").build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"update-from-supplier", "-i", UUID.randomUUID().toString(), "-s", "sunat"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

        assertEquals(0, status);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/core/items/" + orgUnitA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitA.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item A")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20172627421")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.ubigeoSunat']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['organization.legalNamen']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + orgUnitB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitB.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item B")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20101247865")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.ubigeoSunat']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['organization.legalNamen']").doesNotExist());

        getClient(authToken).perform(get("/api/core/items/" + orgUnitC.getID())).andExpect(status().isOk())
                 .andExpect(jsonPath("$.uuid", Matchers.is(orgUnitC.getID().toString())))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Title item C")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressCountry'].[0].value", is("Peru")))
                 .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is("OrgUnit")))
                 .andExpect(jsonPath("$.metadata['organization.identifier.ruc'].[0].value", is("20102982372")))
                 .andExpect(jsonPath("$.metadata['organization.address.addressLocality']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.type.ciiu']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['perucris.ubigeoSunat']").doesNotExist())
                 .andExpect(jsonPath("$.metadata['organization.legalNamen']").doesNotExist());
    }

}
