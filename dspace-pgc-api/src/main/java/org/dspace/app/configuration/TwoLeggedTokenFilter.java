/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.IOException;
import java.text.ParseException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * @author Alba Aliu
 *
 * Filters all request to the path /pgc-api/{scope}/*
 */

public class TwoLeggedTokenFilter implements Filter {
    private static final Logger log = getLogger(TwoLeggedTokenFilter.class);
    private final org.dspace.services.ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {
        String token = resolveToken((HttpServletRequest) req);
        try {
            if (token == null) {
                throwUNAUTHORIZED((HttpServletResponse) res);
                return;
            }
            SignatureValidationUtil.setToken(token);
            JWTClaimsSet claimsSet = SignatureValidationUtil.validateSignature();
            if (!validateScopes(claimsSet)) {
                throwUNAUTHORIZED((HttpServletResponse) res);
                return;
            }

        } catch (BadJOSEException | ParseException | JOSEException e) {
            log.error(e.getMessage());
            throwUNAUTHORIZED((HttpServletResponse) res);
            return;
        }
        filterChain.doFilter(req, res);
    }

    /**
     * Resolve token from the request done
     *
     * @param req Servlet Request used to resolve token from
     */
    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Adds error code to the response
     *
     * @param res HttpServletResponse to put the error code
     */
    public void throwUNAUTHORIZED(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Validates the token from request
     *
     * @param claimsSet Claims set
     */
    public boolean validateScopes(JWTClaimsSet claimsSet) {
        Object scope = claimsSet.getClaim("scope");
        if (scope != null) {
            String publicScope = this.configurationService.getProperty("public-scope");
            if (scope.toString().contains(publicScope)) {
                return true;
            }
        }
        return false;
    }

}
