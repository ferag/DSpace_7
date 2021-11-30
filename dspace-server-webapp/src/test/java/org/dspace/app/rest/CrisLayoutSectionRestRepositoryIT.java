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
            .andExpect(jsonPath("$._embedded.sections", hasSize(6)))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("researchoutputs", 0, 0, "col-md-4", expectedBrowseNames))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researchoutputs", 0, 1, "col-md-8", "researchoutputs"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("researchoutputs", 1, 0, "col-md-6",
                    "researchoutputs", "dc.date.accessioned", "desc"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTopComponent("researchoutputs", 1, 1, "col-md-6",
                                    "researchoutputs", "dc.title", "asc"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("researchoutputs", 2, 0, "col-md-12", "researchoutputs"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("researcherprofiles", 0, 0, "col-md-4",
                                                 "rpname", "rpdatecreated", "rpdatemodified"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("researcherprofiles", 0, 1, "col-md-8", "person"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("fundings_and_projects", 0, 0, "col-md-4", "pftitle",
                                                 "pfdatecreated", "pfdatemodified", "pfdatestart", "pfdateend"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("fundings_and_projects", 0, 1, "col-md-8", "project_funding"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("orgunits", 0, 0, "col-md-12 h2 d-flex justify-content-center p-4",
                    "text-key"))))

            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndCountersComponent("orgunits", 1, 0, "col-md-12 py-4",
                    Arrays.asList("RESUME.orgunit.universities", "RESUME.orgunit.ipis",
                        "RESUME.orgunit.investigation_innovation_unities",
                        "RESUME.orgunit.investigation_groups",
                        "RESUME.orgunit.investigation_lines",
                        "RESUME.orgunit.enterprises",
                        "RESUME.orgunit.cti_funders",
                        "RESUME.orgunit.renacyt_centres")))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("orgunits", 2, 0, "col-md-9 p-5", "orgunit"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndMultiColumnTopComponent("orgunits", 3, 0, "col-md-8 pt-5", "orgunit",
                    "dc.date.accessioned", "desc",
                    Arrays.asList("dc.title", "oairecerif.acronym", "perucris.type.orgunit",
                        "perucris.ubigeo")))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("orgunits", 3, 1, "col-md-4 pt-5", "orgunit"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndBrowseComponent("infrastructure", 0, 0, "col-md-4", "eqtitle", "eqdatecreated",
                                                 "eqdatemodified"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("infrastructure", 0, 1, "col-md-8", "infrastructure"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndFacetComponent("infrastructure", 1, 0, "col-md-12", "infrastructure"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("directorios", 0, 0, "col-md-12 py-5 w-50 center", "image"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("directorios", 0, 1,
                    "col-md-12 h2 d-flex justify-content-center py-3","text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndTextRowComponent("directorios", 0, 2,
                    "col-md-12 d-flex justify-content-center py-2",
                    "text-key"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndSearchComponent("directorios", 1, 0, "col-md-12", "site"))))
            .andExpect(jsonPath("$._embedded.sections",
                hasItem(withIdAndCountersComponent("directorios", 2, 0, "col-md-12 py-4",
                    Arrays.asList("RESUME.rprofiles","RESUME.orgunits", "RESUME.researchoutputs",
                        "RESUME.project_funding", "RESUME.infrastructure")))));
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

        String[] expectedBrowseNames = new String[] { "rotitle", "rodatecreated", "rodatemodified", "rodateissued" };

        getClient().perform(get("/api/layout/sections/{id}", "researchoutputs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("researchoutputs")))
            .andExpect(jsonPath("$", withBrowseComponent(0, 0, "col-md-4", expectedBrowseNames)))
            .andExpect(jsonPath("$", withSearchComponent(0, 1, "col-md-8", "researchoutputs")))
            .andExpect(jsonPath("$", withTopComponent(1, 0, "col-md-6", "researchoutputs",
                                                      "dc.date.accessioned", "desc")))
            .andExpect(jsonPath("$", withTopComponent(1, 1, "col-md-6", "researchoutputs", "dc.title", "asc")))
            .andExpect(jsonPath("$", withFacetComponent(2, 0, "col-md-12", "researchoutputs")));
    }

    @Test
    public void testFindOneWithUnknownSectionId() throws Exception {

        getClient().perform(get("/api/layout/sections/{id}", "unknown-section-id"))
            .andExpect(status().isNotFound());
    }
}
