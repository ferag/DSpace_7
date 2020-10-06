/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles redirect after OIDC authentication succeded
 *
 * @author Pasquale Cavallo (pasquale dot cavallo at 4science dot it)
 */
@RestController
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/oidc")
public class OidcRestController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(OidcRestController.class);

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/" + AuthnRest.CATEGORY, "oidc")));
    }

    @RequestMapping(method = RequestMethod.GET)
    public void oidc(HttpServletResponse response,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl) throws IOException {
        if (StringUtils.isBlank(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }
        log.info("Redirecting to " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
