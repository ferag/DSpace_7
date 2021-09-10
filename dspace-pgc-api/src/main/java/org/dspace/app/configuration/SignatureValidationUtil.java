/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.configuration;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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


public class SignatureValidationUtil {
    private static final Logger log = getLogger(PgcApiDataProviderServiceImpl.class);
    private static final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    private static String token;

    public static JWTClaimsSet validateSignature() throws BadJOSEException,
            ParseException, JOSEException {
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
            JWTClaimsSet claimsSet = jwtProcessor1.process(token, null);
            return claimsSet;
            // Print out the token claims set
        } catch (BadJOSEException | ParseException | JOSEException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    public static String getKey() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response
                    = restTemplate.getForEntity(configurationService.getProperty("jwks-url"), String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(response.getBody(), JsonNode.class);
            ArrayNode keys = (ArrayNode) rootNode.get("keys");
            return keys.get(0).toString();
        } catch (JsonProcessingException jsonMappingException) {
            log.error(jsonMappingException);
            return null;
        }
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SignatureValidationUtil.token = token;
    }

    private SignatureValidationUtil() {
        // Throw an exception if this ever *is* called
        throw new AssertionError("Instantiating utility class.");
    }

}
