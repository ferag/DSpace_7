/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility singleton class that permits lookup into submission configuration definition.
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class FormNameLookup {

    private static final Logger log = LoggerFactory.getLogger(FormNameLookup.class);

    private Map<String, Map<String, List<String>>> formNamesMap = new HashMap<>();

    private static FormNameLookup instance;

    private final DCInputsReader dcInputsReader;
    private final SubmissionConfigReader submissionConfigReader;

    private FormNameLookup() throws DCInputsReaderException, SubmissionConfigReaderException {
        this(new DCInputsReader(), new SubmissionConfigReader());
    }

    /**
     * Standard singleton behaviour can be overridden by this constructor, in case for several reasons, i.e. testing
     * purposes, particular {@link DCInputsReader} or {@link SubmissionConfigReader} instances must be used
     * @param dcInputsReader
     * @param submissionConfigReader
     * @throws DCInputsReaderException
     */
    FormNameLookup(DCInputsReader dcInputsReader, SubmissionConfigReader submissionConfigReader)
        throws DCInputsReaderException {
        this.dcInputsReader = dcInputsReader;
        this.submissionConfigReader = submissionConfigReader;
        init();
    }

    public static FormNameLookup getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            instance = new FormNameLookup();
            instance.init();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return instance;
    }

    /**
     * Given a submission name and a field key with underscore as separator (i.e. dc_title) returns the name
     * of the forms containing such field for the given submission
     * @param submissionName
     * @param fieldKey
     * @return a list of all forms part of the submission containing given field
     */
    public List<String> formContainingField(String submissionName, String fieldKey) {
        return formNamesMap.getOrDefault(submissionName, Collections.emptyMap())
            .getOrDefault(fieldKey, Collections.emptyList());
    }

    private void init() throws DCInputsReaderException {
        for (SubmissionConfig submissionConfig : submissionConfigReader
            .getAllSubmissionConfigs(Integer.MAX_VALUE, 0)) {
            formNamesMap.put(submissionConfig.getSubmissionName(),
                fillFieldKeyFormNameMap(submissionConfig));
        }
    }

    private Map<String, List<String>> fillFieldKeyFormNameMap(SubmissionConfig sc) throws DCInputsReaderException {
        Map<String, List<String>> result = new HashMap<>();
        for (DCInputSet dcInputSet : dcInputsReader.getInputsBySubmissionName(sc.getSubmissionName())) {
            String formName = dcInputSet.getFormName();

            Arrays.stream(dcInputSet.getFields())
                .flatMap(Arrays::stream)
                .forEach(dci -> {
                    String fieldKey = fieldKey(dci);
                    if (!result.containsKey(fieldKey)) {
                        result.put(fieldKey, new ArrayList<>());
                    }
                    result.get(fieldKey).add(formName);
                });
        }
        return result;
    }

    private String fieldKey(DCInput dcInput) {
        return Utils.standardize(dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier(), "_");
    }


}
