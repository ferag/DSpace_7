/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

@Service
public class CasLogoutSuccessHandler implements LogoutSuccessHandler {

    private final static String OIDC_LOGOUT_HEADER = "Oidc-Logout";

    @Value("${authentication-cas.logout-url}")
    private String logoutUrl;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) {
        response.setStatus(204);
        response.setHeader(getOidcLogoutHeader(), getLogoutUrl());
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public String getOidcLogoutHeader() {
        return OIDC_LOGOUT_HEADER;
    }


}
