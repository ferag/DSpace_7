/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.apache.log4j.Logger;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.service.EPersonService;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.perucris.externalservices.reniec.ReniecProvider;
import org.dspace.perucris.externalservices.reniec.ReniecRestConnector;
import org.dspace.perucris.registration.DniRegistrationServiceImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Tests against the /api/perucris/dnis endpoint
 */
public class DniRestControllerIT extends AbstractControllerIntegrationTest {

    private static final Logger log = Logger.getLogger(DniRestControllerIT.class);

    @Autowired
    private DniRegistrationServiceImpl dniRegistrationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ReniecProvider reniecProvider;

    @Autowired
    private ReniecRestConnector reniecRestConnector;

    /**
     * The dni registration validation endPoint forward the result computed by the dniRegistrationService.
     * All the possible cases are covered by unit tests.
     * As integration we assure that the expected code is returned as http status.
     * @throws Exception
     */
    @Test
    public void dniRegistrationValidateEndpoint() throws Exception {
        context.turnOffAuthorisationSystem();

        ReniecProvider originalReniecProvider = dniRegistrationService.getReniecProvider();
        ReniecProvider reniecProvider = Mockito.mock(ReniecProvider.class);

        dniRegistrationService.setReniecProvider(reniecProvider);
        ReniecDTO reniecDTO = new ReniecDTO();
        reniecDTO.setBirthDate(LocalDate.of(1982, 11, 9));
        when(reniecProvider.getReniecObject("41918999")).thenReturn(reniecDTO);

        try {

            getClient().perform(get("/api/perucris/dnis/41918999:1982-11-09"))
                    .andExpect(status().is(204));

        } finally {
            dniRegistrationService.setReniecProvider(originalReniecProvider);
        }
    }

}
