/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.SubmissionFormFieldMatcher;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests that verifies that depending on submission form name and definition
 * different authorities are set for the same metadata.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SubmissionFormControllerCustomAuthoritiesIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Override
    public void setUp() throws Exception {

        super.setUp();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] {
                "org.dspace.content.authority.ItemAuthority = AuthorAuthority",
                "org.dspace.content.authority.ItemAuthority = EditorAuthority",
                "org.dspace.content.authority.ItemAuthority = AdvisorAuthority",
                "org.dspace.content.authority.ItemAuthority = InstitutionAuthorAuthority",
                "org.dspace.content.authority.ItemAuthority = InstitutionEditorAuthority",
                "org.dspace.content.authority.ItemAuthority = InstitutionAdvisorAuthority",
                "org.dspace.content.authority.DisabledAuthority = DisabledAuthority"
            });

        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("cris.ItemAuthority.EditorAuthority.entityType", "Person");
        configurationService.setProperty("cris.ItemAuthority.AdvisorAuthority.entityType", "Person");
        configurationService
            .setProperty("cris.ItemAuthority.InstitutionAuthorAuthority.entityType", "InstitutionPerson");
        configurationService
            .setProperty("cris.ItemAuthority.InstitutionEditorAuthority.entityType", "InstitutionPerson");
        configurationService
            .setProperty("cris.ItemAuthority.InstitutionAdvisorAuthority.entityType", "InstitutionPerson");

        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        configurationService
            .setProperty("choices.plugin.institution-publication-dc-contributor-author.override.dc.contributor.author",
                "InstitutionAuthorAuthority");


        configurationService.setProperty("choices.plugin.dc.contributor.editor", "EditorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.editor", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");

        configurationService.setProperty("choices.plugin.dc.contributor.advisor", "AdvisorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.advisor", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.advisor", "true");


        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

    }


    @After
    public void tearDown() throws Exception {
        // we need to force a reload of the config now to be able to reload also the cache of the other
        // authority related services. As this is needed just by this test method it is more efficient do it
        // here instead that force these reload for each method extending the destroy method
        configurationService.reloadConfig();
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }

    @Test
    public void findFieldWithoutCustomAuthorityConfigured() throws Exception {


        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/directorio-publication-dc-contributor-author"))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //We expect the content type to be "application/hal+json;charset=UTF-8"
            .andExpect(content().contentType(contentType))
            //Check that the JSON root matches the expected "sampleauthority" input forms
            .andExpect(jsonPath("$.id", is("directorio-publication-dc-contributor-author")))
            .andExpect(jsonPath("$.name", is("directorio-publication-dc-contributor-author")))
            .andExpect(jsonPath("$.type", is("submissionform")))
            .andExpect(jsonPath("$.rows[0].fields", contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Author",
                    "You must enter at least the author.", false,
                    "Enter the names of the authors of this item in the form Lastname, " +
                        "Firstname [i.e. Smith, Josh or Smith, J].",
                    null, "dc.contributor.author", "AuthorAuthority")
            )))
        ;

    }

    @Test
    public void findFieldWithAuthorityDisabledByFieldAndFormName() throws Exception {


        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/config/submissionforms/cv-patent-dc-contributor-author"))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //We expect the content type to be "application/hal+json;charset=UTF-8"
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.rows[0].fields", contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Author",
                    "You must enter at least the author.", false,
                    "Enter the names of the authors of this item in the form Lastname, " +
                        "Firstname [i.e. Smith, Josh or Smith, J].",
                    null, "dc.contributor.author", "AuthorAuthority")
            )));

        configurationService
            .setProperty("choices.plugin.cv-patent-dc-contributor-author.override.dc.contributor.author",
                "DisabledAuthority");
        cas.clearCache();

        getClient(token).perform(get("/api/config/submissionforms/cv-patent-dc-contributor-author"))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //We expect the content type to be "application/hal+json;charset=UTF-8"
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.rows[0].fields", contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Author",
                    "You must enter at least the author.", false,
                    "Enter the names of the authors of this item in the form Lastname, " +
                        "Firstname [i.e. Smith, Josh or Smith, J].",
                    null, "dc.contributor.author", null)
            )));

    }

    @Test
    public void findFieldWithCustomAuthorityConfigured() throws Exception {


        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(
            get("/api/config/submissionforms/institution-publication_bibliographic_details-dc-contributor-author"))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //We expect the content type to be "application/hal+json;charset=UTF-8"
            .andExpect(content().contentType(contentType))
            //Check that the JSON root matches the expected "sampleauthority" input forms
            .andExpect(jsonPath("$.id", is("institution-publication_bibliographic_details-dc-contributor-author")))
            .andExpect(jsonPath("$.name", is("institution-publication_bibliographic_details-dc-contributor-author")))
            .andExpect(jsonPath("$.type", is("submissionform")))
            .andExpect(jsonPath("$.rows[0].fields", contains(
                SubmissionFormFieldMatcher.matchFormFieldDefinition("onebox", "Author",
                    "You must enter at least the author.", false,
                    "Enter the names of the authors of this item in the form Lastname, " +
                        "Firstname [i.e. Smith, Josh or Smith, J].",
                    null, "dc.contributor.author", "InstitutionAuthorAuthority")
            )))
        ;

    }
}
