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

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
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
public class VirtualFieldControlledVocabularyAuthorityIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldControlledVocabularyAuthority virtualField;
    private Collection collection;

    @Before
    public void init() {
        context.setCurrentUser(admin);
        collection = CollectionBuilder.createCollection(context,
            CommunityBuilder.createCommunity(context).build())
            .withEntityType("Publication")
            .build();
        virtualField = new DSpace().getServiceManager()
            .getServiceByName("virtualFieldControlledVocabularyAuthority",
                VirtualFieldControlledVocabularyAuthority.class);
    }

    @Test
    public void controlledVocabularyNameStrippedFromAuthority() {
        Item item = ItemBuilder.createItem(context, collection)
            .withType("ResourceTypeGenres::text::bibliography", "types:c_86bc")
            .build();

        String fieldName = "virtual.controlledVocabularyAuthority.dc-type";
        String[] metadata = virtualField.getMetadata(context, item, fieldName);

        assertThat(metadata, is(new String[] { "c_86bc" }));
    }

    @Test
    public void originalAuthorityWithoutControlledVocabularyPrefix() {
        Item item = ItemBuilder.createItem(context, collection)
            .withType("ResourceTypeGenres::text::bibliography", "c_86bc")
            .build();

        String fieldName = "virtual.controlledVocabularyAuthority.dc-type";
        String[] metadata = virtualField.getMetadata(context, item, fieldName);

        assertThat(metadata, is(new String[] { "c_86bc" }));
    }

    @Test
    public void originalValueWithoutAuthority() {
        Item item = ItemBuilder.createItem(context, collection)
            .withType("ResourceTypeGenres::text::bibliography")
            .build();

        String fieldName = "virtual.controlledVocabularyAuthority.dc-type";
        String[] metadata = virtualField.getMetadata(context, item, fieldName);

        assertThat(metadata, is(new String[] {""}));
    }
}