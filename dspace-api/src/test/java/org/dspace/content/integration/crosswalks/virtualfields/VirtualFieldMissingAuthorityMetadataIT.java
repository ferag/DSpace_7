/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldMissingAuthorityMetadataIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldMissingAuthorityMetadata virtualFieldMissingAuthorityMetadata;
    private Collection collection;

    @Before
    public void init() throws Exception {

        context.setCurrentUser(admin);
        virtualFieldMissingAuthorityMetadata = new DSpace().getServiceManager()
            .getServiceByName("virtualFieldMissingAuthorityMetadata", VirtualFieldMissingAuthorityMetadata.class);

        Community community = CommunityBuilder.createCommunity(context)
            .build();
        collection = CollectionBuilder.createCollection(context, community)
            .withEntityType("Publication")
            .build();
    }

    @Test
    public void metadataWithAuthorityValueNotReturned() {

        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Rossi Mario", UUID.randomUUID().toString())
            .build();

        String[] metadata = virtualFieldMissingAuthorityMetadata.getMetadata(context, item, "dc.contributor.author");
        assertThat(metadata, is(new String[] {}));
    }

    @Test
    public void metadataWithoutAuthorityValueReturned() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Verdi Antonio")
            .build();
        context.restoreAuthSystemState();

        String[] metadata = virtualFieldMissingAuthorityMetadata.getMetadata(context, item, "dc.contributor.author");
        assertThat(metadata, is(new String[] {"Verdi Antonio"}));
    }

    @Test
    public void onlyMetadataWithoutAuthorityReturned() {

        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Rossi Mario", UUID.randomUUID().toString())
            .withAuthor("Cooper Sheldon Lee")
            .withAuthor("Verdi Giuseppe", UUID.randomUUID().toString())
            .withAuthor("Doe John")
            .build();

        String[] metadata = virtualFieldMissingAuthorityMetadata.getMetadata(context, item, "dc.contributor.author");
        assertThat(metadata, is(new String[] {"Cooper Sheldon Lee", "Doe John"}));
    }

    @Test
    public void metadataListAllWithAuthority() {

        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Rossi Mario", UUID.randomUUID().toString())
            .withAuthor("Smith John", UUID.randomUUID().toString())
            .withAuthor("Cooper Sheldon Lee", UUID.randomUUID().toString())
            .build();

        String[] metadata = virtualFieldMissingAuthorityMetadata.getMetadata(context, item, "dc.contributor.author");
        assertThat(metadata, is(new String[] {}));
    }
}