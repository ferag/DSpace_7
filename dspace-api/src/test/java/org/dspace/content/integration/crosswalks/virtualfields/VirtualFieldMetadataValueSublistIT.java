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

import java.util.Arrays;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldMetadataValueSublistIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldMetadataValueSublist virtualFieldMetadataValueSublist;
    private Collection collection;

    @Before
    public void init() {
        context.setCurrentUser(admin);
        virtualFieldMetadataValueSublist = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("virtualFieldMetadataSublist",
                VirtualFieldMetadataValueSublist.class);
        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community)
            .withEntityType("Publication").build();
    }

    @Test
    public void sublistWithValidIndexes() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth");

        String fieldName = "virtual.valuesSublist.1-3.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] {"second", "third"}));
    }

    @Test
    public void sublistWithOnlyStartIndex() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth", "fifth", "sixth");

        String fieldName = "virtual.valuesSublist.2-.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] { "third", "fourth", "fifth", "sixth" }));
    }

    @Test
    public void sublistWithStartHigherThanSize() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth", "fifth", "sixth");

        String fieldName = "virtual.valuesSublist.20-.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] { }));
    }

    @Test
    public void sublistWithStartAndEndEqual() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth", "fifth", "sixth");

        String fieldName = "virtual.valuesSublist.2-2.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] { }));
    }

    @Test
    public void sublistWithEndLowerThanStart() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth", "fifth", "sixth");

        String fieldName = "virtual.valuesSublist.2-2.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] { }));
    }

    @Test
    public void sublistWithEndHigherThanSize() {

        Item item = publicationWithAlternativeTitles("fist", "second", "third", "fourth", "fifth", "sixth", "seventh");

        String fieldName = "virtual.valuesSublist.1-.dc-title-alternative";

        String[] metadataValues = virtualFieldMetadataValueSublist.getMetadata(context, item, fieldName);

        assertThat(metadataValues, is(new String[] { "second", "third", "fourth", "fifth", "sixth", "seventh" }));
    }

    private Item publicationWithAlternativeTitles(String... titles) {
        ItemBuilder itemBuilder = ItemBuilder.createItem(context, collection);
        Arrays.stream(titles).forEach(itemBuilder::withAlternativeTitle);
        return itemBuilder.build();
    }
}