/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.time.LocalDate;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.perucris.registration.DniRegistrationService;
import org.dspace.perucris.registration.DniValidationResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that verify if a dni:date is valid for registering a new account.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
@RequestMapping(value = "/api/" + RestModel.PERUCRIS + "/dnis")
@RestController
public class DniRestController implements InitializingBean {

    private static final Logger log = Logger.getLogger(DniRestController.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private DniRegistrationService dniRegistrationService;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/" + RestModel.PERUCRIS, "dnis")));
    }

    /**
     * This endpoint aims to validate whether dni and date are valid to create a new eperson.
     * 
     * @see #DniRegistrationService.validateDni(context, dni, date)
     */
    @RequestMapping(value = "/{dniAndDate}", method = RequestMethod.GET)
    public void checkValidDni(HttpServletRequest request, HttpServletResponse response,
            @PathVariable(name = "dniAndDate") String dniAndDate) throws Exception {

        Context context = ContextUtil.obtainContext(request);
        String dni = parseDni(dniAndDate);
        LocalDate date = parseDate(dniAndDate);

        DniValidationResult validateDni = dniRegistrationService.validateDni(context, dni, date);
        context.complete();

        if (validateDni.isError()) {
            response.sendError(validateDni.getStatusCode());
        } else {
            response.setStatus(validateDni.getStatusCode());
        }

    }

    private String parseDni(String dniAndDate) {
        try {
            return dniAndDate.split(":")[0];
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private LocalDate parseDate(String dniAndDate) {
        try {
            return LocalDate.parse(dniAndDate.split(":")[1]);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
