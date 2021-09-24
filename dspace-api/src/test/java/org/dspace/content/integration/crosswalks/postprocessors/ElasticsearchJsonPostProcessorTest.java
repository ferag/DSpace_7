/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.postprocessors;


import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.Is;
import org.junit.Test;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ElasticsearchJsonPostProcessorTest {

    @Test
    public void emptyArrayFilled() {
        final List<String> lines = Arrays.asList("foo: [", "]");
        new ElasticsearchJsonPostProcessor().accept(
            lines);
        assertThat(lines, Is.is(Arrays.asList("foo: [\"\"", "]")));
    }

    @Test
    public void notEmptyArrayIgnored() {
        final List<String> lines = Arrays.asList("foo: [\"bar\", \"baz\", \"boo\",",
                                                     "]");
        new ElasticsearchJsonPostProcessor().accept(
            lines);
        assertThat(lines, Is.is(Arrays.asList("foo: [\"bar\", \"baz\", \"boo\"",
                                              "]")));
    }

    @Test
    public void complexJson() {
        final List<String> lines = Arrays.asList("foo: [\"bar\", \"baz\", \"boo\",",
                                                 "]",
                                                 "boo: car",
                                                 "test : [     ",
                                                 "    ]     ",
                                                 "test 1: asdf");
        new ElasticsearchJsonPostProcessor().accept(
            lines);
        assertThat(lines, Is.is(Arrays.asList("foo: [\"bar\", \"baz\", \"boo\"",
                                              "]",
                                              "boo: car",
                                              "test : [\"\"     ",
                                              "    ]     ",
                                              "test 1: asdf")));
    }
}
