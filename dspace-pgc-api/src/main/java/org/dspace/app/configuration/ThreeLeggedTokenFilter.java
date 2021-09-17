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
import javax.servlet.http.HttpSession;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * @author Alba Aliu
 *
 * Filters all request to the path /pgc-api/ctivitae/**
 */

public class ThreeLeggedTokenFilter implements Filter {
    private static final Logger log = getLogger(ThreeLeggedTokenFilter.class);
    private final org.dspace.services.ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {
        JWTClaimsSet claimsSet = null;
        String token = resolveToken((HttpServletRequest) req);
        try {
            if (token == null) {
                throwUNAUTHORIZED((HttpServletResponse) res);
                return;
            }
            claimsSet = SignatureValidationUtil.claimsSetExtract(token);
            if (!validateScopes(claimsSet)) {
                throwUNAUTHORIZED((HttpServletResponse) res);
                return;
            }
        } catch (BadJOSEException | JOSEException | ParseException e) {
            log.error(e.getMessage());
            throwUNAUTHORIZED((HttpServletResponse) res);
            return;
        }
        if (claimsSet != null) {
            computeNodeBasedOnScopeToken((HttpServletRequest) req, claimsSet);
            filterChain.doFilter(req, res);
        } else {
            log.error("Missing claims");
            throwUNAUTHORIZED((HttpServletResponse) res);
            return;
        }

    }

    /**
     * Resolve token from the request done
     *
     * @param req Servlet Request used to resolve token from
     */
    private String resolveToken(HttpServletRequest req) {
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
    private void throwUNAUTHORIZED(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Validates the token from request
     *
     * @param claimsSet JWTClaimsSet
     */
    private boolean validateScopes(JWTClaimsSet claimsSet) {
        Object scope = claimsSet.getClaim("scope");
        if (scope.toString().contains("pgc-restricted") || scope.toString().contains("pgc-public")) {
            return true;
        }
        return false;
    }

    /**
     * Gets scope and sub from request  and sets them to the request
     *
     * @param request HttpServletRequest in which to add attributes for scope and sub
     */
    private void computeNodeBasedOnScopeToken(HttpServletRequest request, JWTClaimsSet claimsSet) {
        Object scope = claimsSet.getClaim("scope");
        Object sub = claimsSet.getClaim("sub");
        if (scope != null) {
            HttpSession session = request.getSession();
            String restrictedScope = this.configurationService.getProperty("restricted-scope");
            if (scope.toString().contains(restrictedScope)) {
                session.setAttribute("pgc/auth.scope", restrictedScope);
                session.setAttribute("pgc/auth.sub", sub.toString());
            } else {
                String publicScope = this.configurationService.getProperty("public-scope");
                if (scope.toString().contains(publicScope)) {
                    session.setAttribute("pgc/auth.scope", publicScope);
                }
            }
        }
    }
}
