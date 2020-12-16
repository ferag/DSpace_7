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

    /**
     * Returns an instance of {@link ConcytecFeedback} with the given name.
     *
     * @param feedback the feedback value
     * @return an instance of {@link ConcytecFeedback}
     */
    public static ConcytecFeedback fromString(String feedback) {
        return Arrays.stream(ConcytecFeedback.values())
            .filter(value -> value.name().equals(feedback))
            .findFirst()
            .orElse(NONE);
    }
}
