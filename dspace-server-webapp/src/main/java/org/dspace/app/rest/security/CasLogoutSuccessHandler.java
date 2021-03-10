package org.dspace.app.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

@Service
public class CasLogoutSuccessHandler implements LogoutSuccessHandler {

    private final static String CAS_LOGOUT_HEADER = "Cas-Logout";

    @Value("${auth.cas.logoutUrl}")
    private String casLogoutUrl;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) {
        response.setStatus(204);
        response.setHeader(getCasLogoutHeader(), getLogoutUrl());
    }

    public String getLogoutUrl() {
        return casLogoutUrl;
    }

    public String getCasLogoutHeader() {
        return CAS_LOGOUT_HEADER;
    }


}
