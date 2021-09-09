/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.configuration;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.logging.log4j.Logger;
import org.dspace.pgc.service.impl.PgcApiDataProviderServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class SignatureValidationUtil {
    private static final Logger log = getLogger(PgcApiDataProviderServiceImpl.class);
    private static final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    private static String token;

    public static JWTClaimsSet validateSignature() throws BadJOSEException,
            ParseException, JOSEException, MalformedURLException {
        try {
            String key = getKey();
            String token = getToken();
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS512;
            JWK jwk = JWK.parse(key);
            List<JWK> jwkList = new ArrayList<>();
            jwkList.add(jwk);
            JWKSet jwkSet = new JWKSet(jwkList);
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
            JWSKeySelector<SecurityContext> jwsKeySelector =
                    new JWSVerificationKeySelector<>(expectedJWSAlg, jwkSource);
            DefaultJWTProcessor<SecurityContext> jwtProcessor1 = new DefaultJWTProcessor<>();
            jwtProcessor1.setJWSKeySelector(jwsKeySelector);
            SecurityContext ctx = null; // optional context parameter, not required here
            JWTClaimsSet claimsSet = jwtProcessor1.process(token, ctx);
            return claimsSet;
            // Print out the token claims set
        } catch (BadJOSEException | ParseException | JOSEException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    public static String getKey() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response
                = restTemplate.getForEntity(configurationService.getProperty("jwks-url"), String.class);
        return response.getBody().replace("{\"keys\":[", "").replace("]}", "");
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SignatureValidationUtil.token = token;
    }

}
