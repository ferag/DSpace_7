/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.authorization.impl.CanManageCTIVitaeFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageCTIVitae authorization feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanManageCTIVitaeFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private AuthorizationFeature canManageCTIVitaeFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        canManageCTIVitaeFeature = authorizationFeatureService.find(CanManageCTIVitaeFeature.NAME);
    }

    @Test
    public void canManageCTIVitaeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Dima", "Chornenkiy")
                                      .withEmail("dima.chornenkiy@example.com")
                                      .withPassword(password).build();

        Group specialGroup = GroupBuilder.createGroup(context)
                                         .withName("CTIVitae Group")
                                         .addMember(owner)
                                         .build();

        configurationService.setProperty("cti-vitae.group.id", specialGroup.getID());
        context.setSpecialGroup(specialGroup.getID());

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root Community")
                                          .build();

        Collection cvPersonCol = CollectionBuilder.createCollection(context, parentCommunity)
                                                  .withName("Collection cvPerson")
                                                  .withEntityType("CvPerson")
                                                  .build();

        Item cvPerson = ItemBuilder.createItem(context, cvPersonCol)
                                   .withTitle("cv person user")
                                   .withCrisOwner(owner.getName(), owner.getID().toString())
                                   .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(cvPerson, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenOwner = getAuthToken(owner.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2Item = new Authorization(admin, canManageCTIVitaeFeature, itemRest);
        Authorization owner2Item = new Authorization(owner, canManageCTIVitaeFeature, itemRest);

        // define authorization that we know not exists
        Authorization eperson2Item = new Authorization(eperson, canManageCTIVitaeFeature, itemRest);
        Authorization anonymous2Item = new Authorization(null, canManageCTIVitaeFeature, itemRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2Item.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",Matchers.is(AuthorizationMatcher.matchAuthorization(admin2Item))));

        getClient(tokenOwner).perform(get("/api/authz/authorizations/" + owner2Item.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",Matchers.is(AuthorizationMatcher.matchAuthorization(owner2Item))));

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2Item.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2Item.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void canManageCTIVitaeAndOwnerNotBelongsToSpecialGroupTest() throws Exception {
        configurationService.setProperty("cti-vitae.group.id", UUID.randomUUID());
        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Viktok", "Bandola")
                                      .withEmail("viktor.bandola@example.test")
                                      .withPassword(password)
                                      .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root Community")
                                          .build();

        Collection cvPersonCol = CollectionBuilder.createCollection(context, parentCommunity)
                                                  .withName("Collection cvPerson")
                                                  .withEntityType("CvPerson")
                                                  .build();

        Item cvPerson = ItemBuilder.createItem(context, cvPersonCol)
                                   .withTitle("cv person user")
                                   .withCrisOwner(owner.getName(), owner.getID().toString())
                                   .build();

        context.restoreAuthSystemState();

        ItemRest itemRest = itemConverter.convert(cvPerson, DefaultProjection.DEFAULT);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenOwner = getAuthToken(owner.getEmail(), password);
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);

        // define authorizations that we know must exists
        Authorization admin2Item = new Authorization(admin, canManageCTIVitaeFeature, itemRest);

        // define authorization that we know not exists
        Authorization owner2Item = new Authorization(owner, canManageCTIVitaeFeature, itemRest);
        Authorization anonymous2Item = new Authorization(null, canManageCTIVitaeFeature, itemRest);
        Authorization eperson2Item = new Authorization(eperson, canManageCTIVitaeFeature, itemRest);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2Item.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",Matchers.is(AuthorizationMatcher.matchAuthorization(admin2Item))));

        getClient(tokenOwner).perform(get("/api/authz/authorizations/" + owner2Item.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2Item.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2Item.getID()))
                   .andExpect(status().isNotFound());
    }

}