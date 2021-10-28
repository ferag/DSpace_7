/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.AbstractDSpaceIntegrationTest;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class InstitutionEntitiesSubmissionFormsTest extends AbstractDSpaceIntegrationTest {

    private SubmissionConfigReader submissionConfigReader;
    private DCInputsReader dcInputsReader;

    // some submissions are known to be different
    private static final List<String> SKIP_SUBMISSION_CHECKS = Arrays.asList("institution-person" ,
        "institution-person-edit");

    @Before
    public void setUp() throws Exception {
        submissionConfigReader = new SubmissionConfigReader("../dspace/config/item-submission.xml");
        dcInputsReader = new DCInputsReader("../dspace/config/submission-forms.xml");
    }

    @Test
    public void institutionEntitiesSubmissionSettingsSameAsStandard() throws Exception {

        List<SubmissionConfig> institutionEntitySubmissions =
            submissionConfigReader.getAllSubmissionConfigs(Integer.MAX_VALUE, 0)
                .stream()
                .filter(sc -> sc.getSubmissionName().startsWith("institution-"))
                .filter(sc -> !SKIP_SUBMISSION_CHECKS.contains(sc.getSubmissionName()))
                .collect(Collectors.toList());

        for (SubmissionConfig institutionEntitySubmission : institutionEntitySubmissions) {
            String standardEntitySubmissionName = institutionEntitySubmission.getSubmissionName().substring(12);
            SubmissionConfig standardSubmission =
                submissionConfigReader.getSubmissionConfigByName(standardEntitySubmissionName);

            assertThat("Institution submission " + institutionEntitySubmission.getSubmissionName() +
                    " is different than it's related standard " +
                    standardEntitySubmissionName,
                institutionEntitySubmission, is(new SubmissionMatcher(dcInputsReader, standardSubmission)));
        }
    }

    @Test
    public void institutionPublicationSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-publication");
    }

    @Test
    public void institutionPublicationEditTest() {

        assertThatSubmissionsAreEquals("institution-publication-edit");
    }

//    @Test
//    public void institutionProductSubmissionTest() {
//
//        assertThatSubmissionsAreEquals("institution-product");
//    }
//
//    @Test
//    public void institutionProductEditTest() {
//
//        assertThatSubmissionsAreEquals("institution-product-edit");
//    }

    @Test
    public void institutionOrgUnitSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-orgunit");
    }

    @Test
    public void institutionOrgUnitEditTest() {

        assertThatSubmissionsAreEquals("institution-orgunit-edit");
    }

    @Test
    public void institutionProjectSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-project");
    }

    @Test
    public void institutionProjectEditTest() {

        assertThatSubmissionsAreEquals("institution-project-edit");
    }

//    @Test
//    public void institutionPersonSubmissionTest() {
//
//        assertThatSubmissionsAreEquals("institution-person");
//    }

//    @Test
//    public void institutionPersonEditTest() {
//
//        assertThatSubmissionsAreEquals("institution-person-edit");
//    }

    @Test
    public void institutionEquipmentSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-equipment");
    }

    @Test
    public void institutionEquipmentEditTest() {

        assertThatSubmissionsAreEquals("institution-equipment-edit");
    }

    @Test
    public void institutionPatentSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-patent");
    }

    @Test
    public void institutionPatentEditTest() {

        assertThatSubmissionsAreEquals("institution-patent-edit");
    }

    @Test
    public void institutionFundingSubmissionTest() {

        assertThatSubmissionsAreEquals("institution-funding");
    }

    @Test
    public void institutionFundingEditTest() {

        assertThatSubmissionsAreEquals("institution-funding-edit");
    }

    private void assertThatSubmissionsAreEquals(String submissionName) {
        SubmissionConfig submissionConfig =
            submissionConfigReader.getSubmissionConfigByName(submissionName);

        String standardEntitySubmissionName = submissionConfig.getSubmissionName().substring(12);
        SubmissionConfig standardSubmission =
            submissionConfigReader.getSubmissionConfigByName(standardEntitySubmissionName);

        assertThat("Institution submission " + submissionConfig.getSubmissionName() +
                " is different than it's related standard " +
                standardEntitySubmissionName,
            submissionConfig, is(new SubmissionMatcher(dcInputsReader, standardSubmission)));
    }

    private static class SubmissionMatcher extends TypeSafeMatcher<SubmissionConfig> {

        private static final Logger log = LoggerFactory.getLogger(SubmissionMatcher.class);

        private final DCInputsReader dcInputsReader;
        private final SubmissionConfig standardSubmission;

        private final Map<String, List<String>> differences = new HashMap<>();

        public SubmissionMatcher(DCInputsReader dcInputsReader, SubmissionConfig standardSubmission) {

            this.dcInputsReader = dcInputsReader;
            this.standardSubmission = standardSubmission;
        }

        @Override
        protected boolean matchesSafely(SubmissionConfig submissionConfig) {
            try {
                List<String> fields = metadataFields(submissionConfig);
                List<String> standardFields = metadataFields(standardSubmission);

                findDifferences(submissionConfig.getSubmissionName(), fields, standardFields);
                findDifferences(standardSubmission.getSubmissionName(), standardFields, fields);

                return differences.values().stream().allMatch(l -> l.isEmpty());

            } catch (DCInputsReaderException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }

        private void findDifferences(String submissionName, List<String> submissionFields,
                                     List<String> otherSubmissionFields) {
            List<String> delta = submissionFields.stream()
                .filter(s -> !otherSubmissionFields.contains(s))
                .collect(Collectors.toList());

            differences.put(submissionName, delta);
        }

        private List<String> metadataFields(SubmissionConfig submissionConfig) throws DCInputsReaderException {
            int steps = submissionConfig.getNumberOfSteps();
            List<String> result = new ArrayList<>();
            for (int i = 0; i < steps; i++) {
                SubmissionStepConfig step = submissionConfig.getStep(i);
                if (SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType())) {
                    Arrays.stream(dcInputsReader.getInputsByFormName(step.getId()).getFields())
                        .flatMap(Arrays::stream)
                        .map(dcInput -> (StringUtils
                            .join(Arrays.asList(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier()),
                                "_")))
                        .forEach(result::add);

                }
            }
            return result;
        }

        @Override
        public void describeTo(Description description) {
            differences.entrySet()
                .stream().filter(es -> !es.getValue().isEmpty())
                .map(e -> String.format("Following metadata are configured only in %s submission: %s",
                    e.getKey(), String.join(",", e.getValue())))
                .forEach(description::appendText);
        }
    }
}
