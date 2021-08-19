/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.postprocessors;

import java.util.List;
import java.util.ListIterator;



/**
 * This postprocessor provides all functionalities available in {@link JsonPostProcessor}
 * plus the possibility of filling empty arrays with at least an empty string.
 *
 * This is required for Jsons pushed to Elasticsearch.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ElasticsearchJsonPostProcessor extends JsonPostProcessor {

    @Override
    public void accept(final List<String> lines) {
        super.accept(lines);

        ListIterator<String> iterator = lines.listIterator();

        while (iterator.hasNext()) {
            String current = iterator.next();
            if (!(current.trim().endsWith("["))) {
                continue;
            }
            final String next = cleanUpString(iterator.next());
            if (next.startsWith("]")) {
                iterator.previous();
                iterator.previous();
                iterator.set(current.replaceAll("\\[", "[\"\""));
            }
        }
    }

    private String cleanUpString(String str) {
        return str.replace("\r", "").replace("\n", "").replace("\t", "").trim();
    }
}
