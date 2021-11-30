/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles redirect after CAS authentication succeded.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RestController
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/cas")
public class CasRestController {

    private static final Logger log = LoggerFactory.getLogger(CasRestController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @PostConstruct
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this, List.of(new Link("/api/" + AuthnRest.CATEGORY, "cas")));
    }

    @RequestMapping(method = RequestMethod.GET)
    public void cas(HttpServletResponse response,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl) throws IOException {
        if (StringUtils.isBlank(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }
        log.info("Redirecting to " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
