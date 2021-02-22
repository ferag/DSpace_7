/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.util.Arrays;

/**
 * Enum that model the Concytec feedback regarding one item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public enum ConcytecFeedback {

    APPROVE,
    REJECT,
    NONE;

    public static final String FEEDBACK_SEPARATOR = "::";

    /**
     * Returns an instance of {@link ConcytecFeedback} with the given name.
     *
     * @param feedback the feedback value
     * @return an instance of {@link ConcytecFeedback}
     */
    public static ConcytecFeedback fromString(String feedback) {
        String valueToSearch = feedback.contains(FEEDBACK_SEPARATOR)
            ? feedback.substring(feedback.indexOf(FEEDBACK_SEPARATOR) + FEEDBACK_SEPARATOR.length())
            : feedback;
        return Arrays.stream(ConcytecFeedback.values())
            .filter(value -> value.name().equals(valueToSearch))
            .findFirst()
            .orElse(NONE);
    }
}
