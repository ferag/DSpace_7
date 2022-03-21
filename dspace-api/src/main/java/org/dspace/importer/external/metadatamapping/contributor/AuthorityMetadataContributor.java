/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.DSpaceControlledVocabulary;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataContributor} adding authority information
 * to metadata, according to authority name passed as parameter
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class AuthorityMetadataContributor implements MetadataContributor<String> {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    private final MetadataContributor<String> innerContributor;

    private final String authorityName;

    public AuthorityMetadataContributor(
        MetadataContributor<String> innerContributor, String authorityName) {
        this.innerContributor = innerContributor;
        this.authorityName = authorityName;
    }

    /**
     * Constructor used for testing purposes
     *  @param choiceAuthorityService
     *  @param innerContributor
     *  @param authorityName
     */
    AuthorityMetadataContributor(ChoiceAuthorityService choiceAuthorityService,
                                 MetadataContributor<String> innerContributor, String authorityName) {
        this(innerContributor, authorityName);
        this.choiceAuthorityService = choiceAuthorityService;
    }

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<String, MetadataContributor<String>> rt) {

    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(String metadata) {
        return innerContributor.contributeMetadata(metadata)
            .stream()
            .map(this::addAuthority)
        .collect(Collectors.toList());
    }

    private MetadatumDTO addAuthority(MetadatumDTO metadatumDTO) {
        ChoiceAuthority choiceAuthority;

        try {
            choiceAuthority = choiceAuthorityService.getChoiceAuthorityByAuthorityName(this.authorityName);
        } catch (IllegalArgumentException e) {
            return metadatumDTO;
        }

        Choices bestMatch = choiceAuthority.getBestMatch(metadatumDTO.getValue(), StringUtils.EMPTY);
        if (bestMatch.values.length == 0) {
            return metadatumDTO;
        }

        String authority = authorityString(choiceAuthority, bestMatch);
        metadatumDTO.setValue(metadatumDTO.getValue() + "$$" + authority + "$$" + bestMatch.confidence);
        return metadatumDTO;

    }

    private String authorityString(DSpaceControlledVocabulary choiceAuthority, Choices bestMatch) {
        return new StringBuilder()
            .append(authorityName)
            .append(":")
            .append(bestMatch.values[0].authority)
            .toString();
    }

    private String authorityString(ChoiceAuthority choiceAuthority, Choices bestMatch) {
        if (!(choiceAuthority instanceof DSpaceControlledVocabulary)) {
            return bestMatch.values[0].authority;
        }
        return new StringBuilder()
            .append(authorityName)
            .append(":")
            .append(bestMatch.values[0].authority)
            .toString();
    }
}
