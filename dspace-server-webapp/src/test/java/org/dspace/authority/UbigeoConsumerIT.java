/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;
import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite to verify the UbigeoConsumer.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UbigeoConsumerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void ubigeoMetadataWithAuthorityTest() throws Exception {
        configurationService.setProperty("webui.submit.upload.required", false);
        configurationService.setProperty("authority.controlled.perucris.ubigeo", "true");
        configurationService.setProperty("authority.controlled.perucris.ubigeo.ubigeoSunat", "true");
        configurationService.setProperty("authority.controlled.perucris.domicilio.ubigeoReniec", "true");
        configurationService.setProperty("authority.controlled.perucris.nacimiento.ubigeoReniec", "true");
        metadataAuthorityService.clearCache();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context).build();

            Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                    .withSubmitterGroup(eperson).withTemplateItem().build();

            Item templateItem = col.getTemplateItem();

            itemService.addMetadata(context, templateItem, "perucris", "ubigeo", null, null,
                                    "UbiGeo::Amazonas::Chachapoyas", "peru_ubigeo:0101", Choices.CF_ACCEPTED);

            itemService.addMetadata(context, templateItem, "perucris", "ubigeo", "ubigeoSunat", null,
                                    "UbiGeo::Arequipa::Arequipa::Cayma", "peru_ubigeo:040103", Choices.CF_ACCEPTED);

            itemService.addMetadata(context, templateItem, "perucris", "domicilio", "ubigeoReniec", null,
                                    "UbiGeo::Ayacucho::Huamanga::Socos", "peru_ubigeo:050112", Choices.CF_ACCEPTED);

            itemService.addMetadata(context, templateItem, "perucris", "nacimiento", "ubigeoReniec", null,
                                    "UbiGeo::Junín::Huancayo::Colca", "peru_ubigeo:120112", Choices.CF_ACCEPTED);

            itemService.addMetadata(context, templateItem, "dc", "title", null, null, "Test Title");
            itemService.addMetadata(context, templateItem, "dc", "date", "issued", null, "2021-03-01");

            context.restoreAuthSystemState();

            String authToken = getAuthToken(eperson.getEmail(), password);

            getClient(authToken).perform(post("/api/submission/workspaceitems")
                                .param("owningCollection", col.getID().toString())
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$._embedded.collection.id", is(col.getID().toString())))
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/submission/workspaceitems/" + idRef.get()))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$._embedded.item.metadata['dc.title'][0].value", is("Test Title")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeo'][0].value",
                                      is("UbiGeo::Amazonas::Chachapoyas")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeoRegion'][0].value",
                                      is("Amazonas")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeo.ubigeoSunat'][0].value",
                                      is("UbiGeo::Arequipa::Arequipa::Cayma")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeo.ubigeoSunatRegion'][0].value",
                                      is("Arequipa")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.domicilio.ubigeoReniec'][0].value",
                                      is("UbiGeo::Ayacucho::Huamanga::Socos")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.domicilio.ubigeoReniecRegion'][0].value",
                                      is("Ayacucho")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.nacimiento.ubigeoReniec'][0].value",
                                      is("UbiGeo::Junín::Huancayo::Colca")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.nacimiento.ubigeoReniecRegion'][0].value",
                                       is("Junín")));

        } finally {
            WorkspaceItemBuilder.deleteWorkspaceItem(idRef.get());
            configurationService.setProperty("webui.submit.upload.required", true);
            configurationService.setProperty("authority.controlled.perucris.ubigeo", "false");
            configurationService.setProperty("authority.controlled.perucris.ubigeo.ubigeoSunat", "false");
            configurationService.setProperty("authority.controlled.perucris.domicilio.ubigeoReniec", "false");
            configurationService.setProperty("authority.controlled.perucris.nacimiento.ubigeoReniec", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void ubigeoMetadataWithoutAuthorityTest() throws Exception {
        configurationService.setProperty("webui.submit.upload.required", false);
        configurationService.setProperty("authority.controlled.perucris.ubigeo", "true");
        metadataAuthorityService.clearCache();
        AtomicReference<Integer> idRef = new AtomicReference<>();
        try {
            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context).build();

            Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                    .withSubmitterGroup(eperson).withTemplateItem().build();

            Item templateItem = col.getTemplateItem();
            itemService.addMetadata(context, templateItem, "perucris", "ubigeo", null, null,
                                    "UbiGeo::Ayacucho::Huamanga::Socos", null, Choices.CF_UNSET);

            itemService.addMetadata(context, templateItem, "dc", "title", null, null, "Test Title");
            itemService.addMetadata(context, templateItem, "dc", "date", "issued", null, "2021-03-01");

            context.restoreAuthSystemState();

            String authToken = getAuthToken(eperson.getEmail(), password);

            getClient(authToken).perform(post("/api/submission/workspaceitems")
                                .param("owningCollection", col.getID().toString())
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$._embedded.collection.id", is(col.getID().toString())))
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(authToken).perform(get("/api/submission/workspaceitems/" + idRef.get()))
                     .andExpect(status().isOk())
                     .andExpect(jsonPath("$._embedded.item.metadata['dc.title'][0].value", is("Test Title")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeo'][0].value",
                                      is("UbiGeo::Ayacucho::Huamanga::Socos")))
                     .andExpect(jsonPath("$._embedded.item.metadata['perucris.ubigeoRegion']").doesNotExist());

        } finally {
            WorkspaceItemBuilder.deleteWorkspaceItem(idRef.get());
            configurationService.setProperty("webui.submit.upload.required", true);
            configurationService.setProperty("authority.controlled.perucris.ubigeo", "false");
            configurationService.setProperty("authority.controlled.perucris.ubigeo.ubigeoSunat", "false");
            configurationService.setProperty("authority.controlled.perucris.domicilio.ubigeoReniec", "false");
            configurationService.setProperty("authority.controlled.perucris.nacimiento.ubigeoReniec", "false");
            metadataAuthorityService.clearCache();
        }
    }
}
