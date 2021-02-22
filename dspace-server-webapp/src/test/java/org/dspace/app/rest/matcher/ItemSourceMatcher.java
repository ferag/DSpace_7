/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
//import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.hamcrest.Matcher;

/**
 * Provide convenient org.hamcrest.Matcher to verify a ItemSourceRest json response
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSourceMatcher {

    private ItemSourceMatcher() {}

    public static Matcher<? super Object> matchSource(String itemUuid,
                                                      String relationshipType,
                                                      String source,
                                                      List<String> metadata) {
        return allOf(
                hasJsonPath("$.itemUuid", is(itemUuid)),
                hasJsonPath("$.relationshipType", is(relationshipType)),
                hasJsonPath("$.source", is(source))
                //TODO
                // hasJsonPath("$.metadata", arrayContainingInAnyOrder(metadata))
                );
    }

}