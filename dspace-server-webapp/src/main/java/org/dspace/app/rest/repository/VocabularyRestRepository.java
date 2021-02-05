/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.authority.service.FormNameLookup;
import org.dspace.content.Collection;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of vocabularies for the submission
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyRest.NAME)
public class VocabularyRestRepository extends DSpaceRestRepository<VocabularyRest, String> {

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private AuthorityUtils authorityUtils;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    private final SubmissionConfigReader submissionConfigReader;

    public VocabularyRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public VocabularyRest findOne(Context context, String name) {
        ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(name);
        return authorityUtils.convertAuthority(source, name, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<VocabularyRest> findAll(Context context, Pageable pageable) {
        Set<String> authoritiesName = cas.getChoiceAuthoritiesNames();
        List<VocabularyRest> results = new ArrayList<>();
        Projection projection = utils.obtainProjection();
        for (String authorityName : authoritiesName) {
            ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(authorityName);
            VocabularyRest result = authorityUtils.convertAuthority(source, authorityName, projection);
            results.add(result);
        }
        return utils.getPage(results, pageable);
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @SearchRestMethod(name = "byMetadataAndCollection")
    public VocabularyRest findByMetadataAndCollection(
           @Parameter(value = "metadata", required = true) String metadataField,
           @Parameter(value = "collection", required = true) UUID collectionUuid) {

        Collection collection = null;
        MetadataField metadata = null;
        String[] tokens = org.dspace.core.Utils.tokenize(metadataField);

        try {
            collection = collectionService.find(obtainContext(), collectionUuid);
            metadata = metadataFieldService.findByElement(obtainContext(), tokens[0], tokens[1], tokens[2]);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "A database error occurs retrieving the metadata and/or the collection information", e);
        }

        if (metadata == null) {
            throw new UnprocessableEntityException(metadataField + " is not a valid metadata");
        }
        if (collection == null) {
            throw new UnprocessableEntityException(collectionUuid + " is not a valid collection");
        }

        final MetadataField md = metadata;

        String submissionName = submissionConfigReader.getSubmissionConfigByCollection(collection)
            .getSubmissionName();
        List<String> formNames = FormNameLookup.getInstance()
            .formContainingField(submissionName, md.toString('_'));

        if (formNames.size() > 1) {
            throw new IllegalStateException(
                String.format("%s defined multiple times in %s collection submission form", metadataField,
                    collectionUuid));
        }

        String formNameDefinition = formNames.isEmpty() ? "" : formNames.get(0);
        String authorityName =
            cas.getChoiceAuthorityName(tokens[0], tokens[1], tokens[2], formNameDefinition);

        ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(authorityName);

        return authorityUtils.convertAuthority(source, authorityName, utils.obtainProjection());
    }

    private boolean containsField(DCInputSet dcInputSet, MetadataField metadataField) {
        return Arrays.stream(dcInputSet.getFields())
            .anyMatch(input -> Arrays.stream(input).anyMatch(dc ->
                metadataField.toString('.').equals(dc.getFieldName())));
    }

    @Override
    public Class<VocabularyRest> getDomainClass() {
        return VocabularyRest.class;
    }

}
