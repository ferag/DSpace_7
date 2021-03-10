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
    private Item ePerson2Item;
    private Item cvPersonItem;
    private Item cvPerson2Item;
    private Item notification;
    private Item notification2;

    @Test
    public void notificationAuthorityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colCvPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("PersonCv Collection")
                                          .build();

       Collection colNotification = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Notification Collection")
                                          .build();

       this.cvPersonItem = ItemBuilder.createItem(context, colCvPerson)
                .withTitle("CvPerson Title")
                .withRelationshipType("CvPerson")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withNotificationTo("test notification", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       this.notification2 = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 2")
                .withNotificationTo("test notification 2", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/discover/search/objects")
                             .param("configuration", "RELATION.CvPerson.notifications")
                             .param("scope", this.cvPersonItem.getID().toString()))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                                 is(this.notification.getID().toString())))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.name",
                                 is(this.notification.getName())))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[1]._embedded.indexableObject.id",
                                 is(this.notification2.getID().toString())))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[1]._embedded.indexableObject.name",
                                 is(this.notification2.getName())));
    }

    @Test
    public void findNotificationUnauthorizedTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("CvPerson Collection")
                                                 .withRelationshipType("CvPerson").build();

       Collection colPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Person Collection")
                                               .withRelationshipType("Person").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection")
                                                     .withRelationshipType("Notification").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPerson)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.cvPersonItem = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("CvPerson")
                .withCrisOwner(user.getName(), user.getID().toString())
                .withRelationshipType("CvPerson")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       getClient().perform(get("/api/core/items/" + notification.getID()))
                  .andExpect(status().isUnauthorized());
    }

    @Test
    public void findNotificationForbiddenTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("CvPerson Collection")
                                                 .withRelationshipType("CvPerson").build();

       Collection colPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Person Collection")
                                               .withRelationshipType("Person").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection")
                                                     .withRelationshipType("Notification").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPerson)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.cvPersonItem = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("CvPerson")
                .withCrisOwner(user.getName(), user.getID().toString())
                .withRelationshipType("CvPerson")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       String tokenEPerson = getAuthToken(eperson.getEmail(), password);
       getClient(tokenEPerson).perform(get("/api/core/items/" + notification.getID()))
                              .andExpect(status().isForbidden());
    }

    @Test
    public void findNotificationTwoEPersonTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colCvPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("PersonCv Collection")
                                                 .withRelationshipType("CvPerson").build();

       Collection colPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Person Collection")
                                                 .withRelationshipType("Person").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection")
                                                     .withRelationshipType("Notification").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       EPerson user2 = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("Anton", "Beket")
               .withEmail("antonbeket@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPerson)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.ePerson2Item = ItemBuilder.createItem(context, colPerson)
               .withTitle("Person2 Item Title")
               .withRelationshipType("Person").build();

       this.cvPersonItem = ItemBuilder.createItem(context, colCvPerson)
                .withTitle("CvPerson")
                .withCrisOwner(user.getName(), user.getID().toString())
                .withRelationshipType("CvPerson").build();

       this.cvPerson2Item = ItemBuilder.createItem(context, colCvPerson)
               .withTitle("CvPerson")
               .withCrisOwner(user2.getName(), user2.getID().toString())
               .withRelationshipType("CvPerson").build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification For User1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       this.notification2 = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification For User2")
                .withIssueDate("2021-02-11")
                .withNotificationTo("test notification 2", cvPerson2Item.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       String tokenUser = getAuthToken(user.getEmail(), password);
       String tokenUser2 = getAuthToken(user2.getEmail(), password);

       getClient(tokenUser).perform(get("/api/core/items/" + notification.getID()))
                           .andExpect(status().isOk())
                           .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                    this.notification, "Notification For User1", "2021-02-17")));

       getClient(tokenUser).perform(get("/api/core/items/" + notification2.getID()))
                           .andExpect(status().isForbidden());


       getClient(tokenUser2).perform(get("/api/core/items/" + notification2.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                     this.notification2, "Notification For User2", "2021-02-11")));

        getClient(tokenUser2).perform(get("/api/core/items/" + notification.getID()))
                             .andExpect(status().isForbidden());
    }

    @Test
    public void findNotificationAdminTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colPersonCv = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("CvPerson Collection")
                                                 .withRelationshipType("CvPerson").build();

       Collection colPerson = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Person Collection")
                                               .withRelationshipType("Person").build();

       Community notificationCommunity = CommunityBuilder.createCommunity(context).build();
       Collection colNotification = CollectionBuilder.createCollection(context, notificationCommunity)
                                                     .withName("Notification Collection")
                                                     .withRelationshipType("Notification").build();

       EPerson user = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       this.ePersonItem = ItemBuilder.createItem(context, colPerson)
               .withTitle("Person Item Title")
               .withRelationshipType("Person").build();

       this.cvPersonItem = ItemBuilder.createItem(context, colPersonCv)
                .withTitle("CvPerson")
                .withCrisOwner(user.getName(), user.getID().toString())
                .withRelationshipType("CvPerson")
                .build();

       this.notification = ItemBuilder.createItem(context, colNotification)
                .withTitle("Notification 1")
                .withIssueDate("2021-02-17")
                .withNotificationTo("test notification", cvPersonItem.getID().toString())
                .withRelationshipType("Notification")
                .build();

       context.restoreAuthSystemState();

       String tokenAdmin = getAuthToken(admin.getEmail(), password);
       getClient(tokenAdmin).perform(get("/api/core/items/" + notification.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                     this.notification, "Notification 1", "2021-02-17")));
    }

}