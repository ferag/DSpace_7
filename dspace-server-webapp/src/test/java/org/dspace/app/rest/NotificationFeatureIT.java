/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.junit.Test;

/**
 * Test suite to verify the Notification feature.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
public class NotificationFeatureIT extends AbstractControllerIntegrationTest {

    private Item ePersonItem;
    private Item personCv;
    private Item notification;
    private Item notification2;

    @Test
    public void notificationAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("PersonCv Collection").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                          .withName("Notification Collection").build();

       this.personCv = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("personCv")
                .withRelationshipType("PersonCv")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withNotificationTo("test notification", personCv.getID().toString())
                .withRelationshipType("Notification")
                .build();

       this.notification2 = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 2")
                .withNotificationTo("test notification 2", personCv.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/discover/search/objects")
                             .param("configuration", "RELATION.CvPerson.notifications")
                             .param("scope", this.personCv.getID().toString()));
    }

    @Test
    public void findNotificationTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("PersonCv Collection").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPersonCv)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.personCv = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("CvPerson")
                .withCrisOwner(user.getID().toString(), ePersonItem.getID().toString())
                .withRelationshipType("PersonCv")
                .build();



       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", personCv.getID().toString())
                .withRelationshipType("Notification")
                .build();

       this.notification2 = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 2")
                .withIssueDate("2021-02-11")
                .withNotificationTo("test notification 2", personCv.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       String tokenUser = getAuthToken(user.getEmail(), password);
       getClient(tokenUser).perform(get("/api/core/items/" + notification.getID()))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                    this.notification, "Notification 1", "2021-02-17")));

        getClient(tokenUser).perform(get("/api/core/items/" + notification2.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                     this.notification2, "Notification 2", "2021-02-11")));
    }

    @Test
    public void findNotificationForbiddenTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("PersonCv Collection").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPersonCv)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.personCv = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("CvPerson")
                .withCrisOwner(user.getID().toString(), ePersonItem.getID().toString())
                .withRelationshipType("PersonCv")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", personCv.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       String tokenEPerson = getAuthToken(eperson.getEmail(), password);
       getClient(tokenEPerson).perform(get("/api/core/items/" + notification.getID()))
                              .andExpect(status().isForbidden());
    }

}