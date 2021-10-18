/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link ResearcherProfileValueGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResearcherProfileValueGeneratorTest {

    @Mock
    private Context context;

    @Mock
    private Item targetItem;

    @Mock
    private Item templateItem;

    private String extraParams = "";

    @Mock
    private ResearcherProfileService researcherProfileService;

    @InjectMocks
    private ResearcherProfileValueGenerator generator;

    @Test
    public void testWithoutUserInTheContext() {

        MetadataValueVO metadataValue = generator
            .generator(context, targetItem, templateItem, extraParams)
            .get(0);

        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is(""));
        assertThat(metadataValue.getAuthority(), nullValue());
        assertThat(metadataValue.getConfidence(), is(-1));

    }

    @Test
    public void testWithUserInTheContextWithoutProfile() {

        EPerson currentUser = buildEPersonMock(UUID.fromString("25ad8d1a-e00f-4077-b2a2-326822d6aea4"));
        when(context.getCurrentUser()).thenReturn(currentUser);

        MetadataValueVO metadataValue = generator
            .generator(context, targetItem, templateItem, extraParams)
            .get(0);

        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is(""));
        assertThat(metadataValue.getAuthority(), nullValue());
        assertThat(metadataValue.getConfidence(), is(-1));

    }

    @Test
    public void testWithUserInTheContextWithProfile() throws Exception {

        UUID userId = UUID.fromString("25ad8d1a-e00f-4077-b2a2-326822d6aea4");

        EPerson currentUser = buildEPersonMock(userId);
        when(context.getCurrentUser()).thenReturn(currentUser);

        ResearcherProfile researcherProfile = buildResearcherProfile("72d8ff65-619e-4f78-b005-78db7a2c835c", "Profile");
        when(researcherProfileService.findById(context, userId)).thenReturn(researcherProfile);

        MetadataValueVO metadataValue = generator
            .generator(context, targetItem, templateItem, extraParams)
            .get(0);

        assertThat(metadataValue, notNullValue());
        assertThat(metadataValue.getValue(), is("Profile"));
        assertThat(metadataValue.getAuthority(), is("72d8ff65-619e-4f78-b005-78db7a2c835c"));
        assertThat(metadataValue.getConfidence(), is(600));

    }

    private EPerson buildEPersonMock(UUID uuid) {
        EPerson ePerson = mock(EPerson.class);
        when(ePerson.getID()).thenReturn(uuid);
        return ePerson;
    }

    private ResearcherProfile buildResearcherProfile(String itemId, String fullname) {
        ResearcherProfile researcherProfile = mock(ResearcherProfile.class);
        when(researcherProfile.getItemFullName()).thenReturn(fullname);
        when(researcherProfile.getItemId()).thenReturn(UUID.fromString(itemId));
        return researcherProfile;
    }

}
