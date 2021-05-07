/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 *
 * Extension of {@link DiscoverySortFieldConfiguration} used to configure sorting
 * taking advantage of solr function feature.
 *
 * Order is evaluated by mean of function parameter value and passed in arguments as input.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DiscoverySortFunctionConfiguration extends DiscoverySortFieldConfiguration {

    public static final String SORT_FUNCTION = "sort_function";
    private String function;
    private List<String> arguments;

    public void setFunction(final String function) {
        this.function = function;
    }

    public void setArguments(final List<String> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String getType() {
        return SORT_FUNCTION;
    }

    @Override
    public String getMetadataField() {
        final String args = String.join(",",
                                        Optional.ofNullable(arguments).orElse(Collections.emptyList()));
        return function + "(" + args + ")";
    }
}
