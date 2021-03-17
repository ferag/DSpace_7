package org.dspace.app.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

@Service
public class OidcLogoutSuccessHandler implements LogoutSuccessHandler {

    private final static String OIDC_LOGOUT_HEADER = "Oidc-Logout";

    @Value("${authentication-oidc.logoutUrl}")
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
