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

import java.util.Locale;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class VirtualFieldValuePairIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldValuePair virtualFieldValuePair;
    private Collection collection;

    @Before
    public void init() {
        context.setCurrentUser(admin);

        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community)
            .withEntityType("Publication")
            .build();

        virtualFieldValuePair = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("virtualFieldValuePair", VirtualFieldValuePair.class);

        String[] supportedLanguage = { "it", "en" };

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("webui.supported.locales", supportedLanguage);

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        ChoiceAuthorityService choiceAuthorityService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("org.dspace.content.authority.ChoiceAuthorityServiceImpl",
                ChoiceAuthorityService.class);

        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void defaultLanguageSet() {

        virtualFieldValuePair.setDefaultLanguage("it");

        Item publication = ItemBuilder.createItem(context, collection)
            .withLanguage("fr")
            .withLanguage("es")
            .build();

        String[] metadata = virtualFieldValuePair.getMetadata(context, publication,
            "virtual.valuepair.common_iso_languages.dc-language-iso");

        assertThat(metadata, is(new String[]{"Francese", "Spagnolo"}));

    }

    @Test
    public void defaultLanguageNotSet() {

        context.setCurrentLocale(Locale.ENGLISH);
        virtualFieldValuePair.setDefaultLanguage(null);
        Item publication = ItemBuilder.createItem(context, collection)
            .withLanguage("fr")
            .withLanguage("it")
            .build();

        String[] metadata = virtualFieldValuePair.getMetadata(context, publication,
            "virtual.valuepair.common_iso_languages.dc-language-iso");

        assertThat(metadata, is(new String[]{ "French", "Italian" }));

    }

    @Test
    public void emptyMetadatalist() {

        Item publication = ItemBuilder.createItem(context, collection)
            .build();

        String[] metadata = virtualFieldValuePair.getMetadata(context, publication,
            "virtual.valuepair.common_iso_languages.dc-language-iso");

        assertThat(metadata, is(new String[]{}));

    }
}