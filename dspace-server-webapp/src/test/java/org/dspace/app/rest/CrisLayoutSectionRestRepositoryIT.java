/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndBrowseComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndCountersComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndFacetComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndMultiColumnTopComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndTextRowComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withIdAndTopComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withSearchComponent;
import static org.dspace.app.rest.matcher.CrisLayoutSectionMatcher.withTopComponent;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.service.impl.CrisLayoutSectionServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CrisLayoutSectionRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutSectionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    CrisLayoutSectionServiceImpl crisLayoutSectionService;

    @Test
    public void testFindAll() throws Exception {

        String[] expectedBrowseNames = new String[] { "rotitle", "rodatecreated", "rodatemodified", "rodateissued"};

        getClient().perform(get("/api/layout/sections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.sections", hasSize(7)))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("researchoutputs", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndCountersComponent("researchoutputs", 1, 0,
                    "col-md-12 py-4",
                    Arrays.asList(
                        "RESUME.researchoutputs.articles",
                        "RESUME.researchoutputs.books",
                        "RESUME.researchoutputs.tesis",
                        "RESUME.researchoutputs.conferencepapers",
                        "RESUME.researchoutputs.patents",
                        "RESUME.researchoutputs.openaccess")))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researchoutputs", 2, 0, "col-md-9 p-5",
                    "researchoutputs"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("researchoutputs", 3, 0,
                    "col-md-8 pt-5", "researchoutputs",
                    "dc.date.accessioned", "desc"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("fundings_and_projects", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("researcherprofiles", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("orgunits", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("infrastructure", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("directorios", 0, 0,
                    "col-md-12 py-5 w-50 center",
                    "image"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("ctivitae", 0, 0,
                    "col-md-12 h2 d-flex justify-content-center text-section-heading py-3",
                    "text-key"))));
    }

    @Test
    public void testSearchVisibleTopBarSections() throws Exception {

        List<CrisLayoutSection> originalSections = new LinkedList<>();
        originalSections.addAll(crisLayoutSectionService.getComponents());

        List<List<CrisLayoutSectionComponent>> components = new ArrayList<List<CrisLayoutSectionComponent>>();

        components.add(new ArrayList<CrisLayoutSectionComponent>());
        components.get(0).add(new org.dspace.layout.CrisLayoutSearchComponent());

        List<CrisLayoutSection> sectionsForMock = new LinkedList<>();
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes1", true, components));
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes2", false, components));
        sectionsForMock.add(new CrisLayoutSection("CasualIdForTestingPurposes3", true, components));


        //MOCKING the sections
        crisLayoutSectionService.getComponents().clear();
        crisLayoutSectionService.getComponents().addAll(sectionsForMock);
        //end setting up the mock

        try {
            getClient().perform(get("/api/layout/sections/search/visibleTopBarSections"))
                .andExpect(status().isOk())
                // Only 2 sections are set up to be visible in the top bar
                .andExpect(jsonPath("$._embedded.sections", hasSize(2)))
                // One has id -> CasualIdForTestingPurposes1
                .andExpect(jsonPath("$._embedded.sections[0].id", is("CasualIdForTestingPurposes1")))
                // The other has id -> CasualIdForTestingPurposes3
                .andExpect(jsonPath("$._embedded.sections[1].id", is("CasualIdForTestingPurposes3")));
        } catch (Exception e) {
            // Test Failed
        } finally {
            // Restoring situation previous to mock
            crisLayoutSectionService.getComponents().clear();
            crisLayoutSectionService.getComponents().addAll(originalSections);
            // end restoring
        }

    }

    @Test
    public void testFindOne() throws Exception {

        getClient().perform(get("/api/layout/sections/{id}", "researchoutputs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("researchoutputs")))
            .andExpect(jsonPath("$",withIdAndTextRowComponent("researchoutputs", 0, 0,
                    "col-md-12 h2 d-flex text-section-heading justify-content-center p-4",
                    "text-key")))
            .andExpect(jsonPath("$", withIdAndCountersComponent("researchoutputs", 1, 0,
                    "col-md-12 py-4",
                    Arrays.asList(
                        "RESUME.researchoutputs.articles",
                        "RESUME.researchoutputs.books",
                        "RESUME.researchoutputs.tesis",
                        "RESUME.researchoutputs.conferencepapers",
                        "RESUME.researchoutputs.patents",
                        "RESUME.researchoutputs.openaccess"))))
            .andExpect(jsonPath("$", withIdAndSearchComponent("researchoutputs", 2, 0,
                "col-md-9 p-5", "researchoutputs")))
            .andExpect(jsonPath("$",withIdAndTopComponent("researchoutputs", 3, 0,
                    "col-md-8 pt-5", "researchoutputs",
                    "dc.date.accessioned", "desc")));
    }

    @Test
    public void testFindOneWithUnknownSectionId() throws Exception {

        getClient().perform(get("/api/layout/sections/{id}", "unknown-section-id"))
            .andExpect(status().isNotFound());
    }
}
