/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;

/**
 * 
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */public class AuthorityMetadataContributorTest {

    private final ChoiceAuthorityService choiceAuthorityService = mock(ChoiceAuthorityService.class);
    private final MetadataContributor<String> innerContributor = mock(MetadataContributor.class);
    private final ChoiceAuthority choiceAuthority = mock(ChoiceAuthority.class);

    @Test
    public void authorityAppended() {


        String metadataField = "metadata";
        String authorityName = "authority";
        String mdAuthority = "mdAuthority";
        String metadataValue = "metadataValue";

        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName(authorityName))
            .thenReturn(choiceAuthority);
        when(choiceAuthority.getBestMatch(metadataValue, StringUtils.EMPTY))
            .thenReturn(choices(mdAuthority, metadataValue));

        MetadatumDTO originalMetadata = metadatum(metadataValue);

        when(innerContributor.contributeMetadata(metadataField))
            .thenReturn(Collections.singletonList(originalMetadata));


        AuthorityMetadataContributor authorityMetadataContributor = new AuthorityMetadataContributor(
            choiceAuthorityService,
            innerContributor,
            authorityName);

        Collection<MetadatumDTO> result = authorityMetadataContributor
            .contributeMetadata(metadataField);

        assertEquals(result.iterator().next().getValue(), "metadataValue$$mdAuthority$$600");
    }

    @Test
    public void authorityAppendedToMetadataCollection() {


        String metadataField = "metadata";
        String authorityName = "authority";
        String mdAuthority = "mdAuthority";
        String secondMdAuthority = "authorityTwo";
        String metadataValue = "metadataValue";
        String secondMetadataValue = "secondMetadataValue";

        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName(authorityName))
            .thenReturn(choiceAuthority);
        when(choiceAuthority.getBestMatch(metadataValue, StringUtils.EMPTY))
            .thenReturn(choices(mdAuthority, metadataValue));
        when(choiceAuthority.getBestMatch(secondMetadataValue, StringUtils.EMPTY))
            .thenReturn(choices(secondMdAuthority, secondMetadataValue));

        MetadatumDTO originalMetadata = metadatum(metadataValue);
        MetadatumDTO secondOriginalMetadata = metadatum(secondMetadataValue);

        when(innerContributor.contributeMetadata(metadataField))
            .thenReturn(Arrays.asList(originalMetadata, secondOriginalMetadata));


        AuthorityMetadataContributor authorityMetadataContributor = new AuthorityMetadataContributor(
            choiceAuthorityService,
            innerContributor,
            authorityName);

        Collection<MetadatumDTO> result = authorityMetadataContributor
            .contributeMetadata(metadataField);

        Iterator<MetadatumDTO> iterator = result.iterator();
        assertEquals(iterator.next().getValue(), "metadataValue$$mdAuthority$$600");
        assertEquals(iterator.next().getValue(), "secondMetadataValue$$authorityTwo$$600");
    }

    @Test
    public void choicesNotFound() {


        String metadataField = "metadata";
        String authorityName = "authority";
        String metadataValue = "metadataValue";

        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName(authorityName))
            .thenReturn(choiceAuthority);
        when(choiceAuthority.getBestMatch(metadataValue, StringUtils.EMPTY))
            .thenReturn(new Choices(Choices.CF_UNSET));

        MetadatumDTO originalMetadata = metadatum(metadataValue);

        when(innerContributor.contributeMetadata(metadataField))
            .thenReturn(Collections.singletonList(originalMetadata));


        AuthorityMetadataContributor authorityMetadataContributor = new AuthorityMetadataContributor(
            choiceAuthorityService,
            innerContributor,
            authorityName);

        Collection<MetadatumDTO> result = authorityMetadataContributor
            .contributeMetadata(metadataField);

        assertEquals(result.iterator().next().getValue(), "metadataValue");
    }

    @Test
    public void authorityNotFound() {


        String metadataField = "metadata";
        String authorityName = "authority";
        String metadataValue = "metadataValue";

        doThrow(new IllegalArgumentException()).when(
            choiceAuthorityService).getChoiceAuthorityByAuthorityName(authorityName);

        MetadatumDTO originalMetadata = metadatum(metadataValue);

        when(innerContributor.contributeMetadata(metadataField))
            .thenReturn(Collections.singletonList(originalMetadata));


        AuthorityMetadataContributor authorityMetadataContributor = new AuthorityMetadataContributor(
            choiceAuthorityService,
            innerContributor,
            authorityName);

        Collection<MetadatumDTO> result = authorityMetadataContributor
            .contributeMetadata(metadataField);

        assertEquals(result.iterator().next().getValue(), "metadataValue");
        verifyNoInteractions(choiceAuthority);
    }

    private MetadatumDTO metadatum(String metadataValue) {
        MetadatumDTO originalMd = new MetadatumDTO();
        originalMd.setValue(metadataValue);
        return originalMd;
    }

    private Choices choices(String mdAuthority, String metadataValue) {
        Choice cc = new Choice(mdAuthority, metadataValue, metadataValue);
        Choice[] ch = new Choice[] {cc};
        Choices c = new Choices(ch, 0, 1, 600, false);
        return c;
    }
}