/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link FormNameLookup}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class FormNameLookupTest {

    private FormNameLookup formNameLookup;

    @Before
    public void setUp() throws Exception {
        SubmissionConfigReader submissionConfigReader = mock(SubmissionConfigReader.class);

        when(submissionConfigReader.getAllSubmissionConfigs(anyInt(), anyInt()))
            .thenReturn(submissionConfigurations("submission-one", "submission-two"));

        DCInputsReader dcInputsReader = mock(DCInputsReader.class);

        DCInputSet formOneInputs = inputSet("form-one", "dc_title", "dc_contributor_author", "dc_contributor_editor");
        DCInputSet formTwoInputs =
            inputSet("form-two", "metadata_one", "metadata_two_field", "metadata_three_qualifier");
        DCInputSet formThreeInputs =
            inputSet("form-three", "metadata_one", "dc_contributor_author", "metadata_three_qualifier");

        when(dcInputsReader.getInputsBySubmissionName("submission-one"))
            .thenReturn(asList(
                formOneInputs,
                formTwoInputs
            ));

        when(dcInputsReader.getInputsBySubmissionName("submission-two"))
            .thenReturn(asList(
                formOneInputs,
                formThreeInputs
            ));

        formNameLookup = new FormNameLookup(dcInputsReader, submissionConfigReader);
    }

    @Test
    public void fieldInOneForm() {
        List<String> formNames = formNameLookup.formContainingField("submission-one", "dc_title");

        assertThat(formNames, is(singletonList("form-one")));
    }

    @Test
    public void fieldInTwoForms() {
        List<String> formNames = formNameLookup.formContainingField("submission-two", "dc_contributor_author");

        assertThat(formNames, is(asList("form-one", "form-three")));
    }

    @Test
    public void missingField() {
        List<String> formNames = formNameLookup.formContainingField("submission-two", "dc_missing_field");

        assertThat(formNames, is(emptyList()));
    }

    private List<SubmissionConfig> submissionConfigurations(String... submissionNames) {
        return Arrays.stream(submissionNames)
            .map(name -> new SubmissionConfig(false, name, emptyList()))
            .collect(Collectors.toList());
    }

    private DCInputSet inputSet(String formName, String... metadataFields) {
        DCInputSet inputSet = mock(DCInputSet.class);
        when(inputSet.getFormName()).thenReturn(formName);
        DCInput[][] fields = fields(metadataFields);
        when(inputSet.getFields()).thenReturn(fields);
        return inputSet;
    }

    private DCInput[][] fields(String... metadataFields) {
        DCInput[][] dcInputs = new DCInput[1][];
        dcInputs[0] =
            Arrays.stream(metadataFields).map(this::dcInput).collect(Collectors.toList()).toArray(new DCInput[] {});
        return dcInputs;
    }

    private DCInput dcInput(String field) {
        DCInput dci = mock(DCInput.class);
        String[] split = field.split("_");
        when(dci.getSchema()).thenReturn(split[0]);
        when(dci.getElement()).thenReturn(split[1]);
        if (split.length > 2) {
            when(dci.getQualifier()).thenReturn(split[2]);
        }
        return dci;
    }

}