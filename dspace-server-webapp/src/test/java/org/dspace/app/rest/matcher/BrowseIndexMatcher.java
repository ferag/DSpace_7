/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Utility class to construct a Matcher for a browse index
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class BrowseIndexMatcher {

    private BrowseIndexMatcher() { }

    public static Matcher<? super Object> subjectBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.subject.*", "perucris.subject.*")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                                "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> titleBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                        "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> contributorBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.contributor.*", "dc.creator")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                    "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> dateIssuedBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.issued")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> rodeptBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("oairecerif.author.affiliation", "oairecerif.editor.affiliation")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> typeBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.type")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                                "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> rpnameBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> rpdeptBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("person.affiliation.name")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> ounameBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> pjtitleBrowseIndex(final String order, final String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName + "/items"))
        );
    }

    public static Matcher<? super Object> eqtitleBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName +
                "/items"))
                    );
    }

    public static Matcher<? super Object> rotitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rotitle")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rotitle/items"))
                    );
    }

    public static Matcher<? super Object> rodatecreatedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.created")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rodatecreated")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rodatecreated/items"))
                    );
    }

    public static Matcher<? super Object> rodatemodifiedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("placeholder.placeholder.placeholder")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rodatemodified")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rodatemodified/items"))
                    );
    }

    public static Matcher<? super Object> rodateissuedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.issued")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rodateissued")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rodateissued/items"))
                    );
    }

    public static Matcher<? super Object> rpdatecreatedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.created")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpdatecreated")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpdatecreated/items"))
                    );
    }

    public static Matcher<? super Object> rpdatemodifiedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("placeholder.placeholder.placeholder")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpdatemodified")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpdatemodified/items"))
                    );
    }

    public static Matcher<? super Object> oudatecreatedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.created")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/oudatecreated")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/oudatecreated/items"))
                    );
    }

    public static Matcher<? super Object> oudatemodifiedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("placeholder.placeholder.placeholder")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/oudatemodified")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/oudatemodified/items"))
                    );
    }

    public static Matcher<? super Object> pftitleBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName +
                "/items"))
                    );
    }

    public static Matcher<? super Object> pfdatecreatedBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.created")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName +
                "/items"))
                    );
    }

    public static Matcher<? super Object> pfdatemodifiedBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("placeholder.placeholder.placeholder")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" +
                browseName + "/items"))
                    );
    }

    public static Matcher<? super Object> pfdatestartBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("oairecerif.project.startDate")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" +
                browseName + "/items"))
                    );
    }

    public static Matcher<? super Object> pfdateendBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("oairecerif.project.endDate")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" + browseName +
                "/items"))
                    );
    }

    public static Matcher<? super Object> eqdatecreatedBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.created")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" +
                browseName + "/items"))
                    );
    }

    public static Matcher<? super Object> eqdatemodifiedBrowseIndex(final String order, String browseName) {
        return allOf(
            hasJsonPath("$.metadata", contains("placeholder.placeholder.placeholder")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned",
                                                            "datemodified", "datecreated", "datestart", "dateend")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + browseName)),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/" +
                browseName + "/items"))
                    );
    }
}
