/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * This class map the response from and OpenID Connect Introspect endpoint.
 * 
 * An expected response could be:
 * 
 * {
 *    "active":true,
 *    "sub":"4992cce9-5976-48bf-bb3b-5b2c5824f0b9",
 *    "scope":"email openid profile",
 *    "iat":1599216499,
 *    "exp":1599245299,
 *    "realmName":"OidcRealm",
 *    "uniqueSecurityName":"4992cce9-5976-48bf-bb3b-5b2c5824f0b9",
 *    "tokenType":"bearer",
 *    "aud":"http://localhost:8043/app2",
 *    "iss":"https://localhost:8082/cas/oidc",
 *    "client_id":"client2",
 *    "grant_type":"authorization_code"
 *  }
 * 
 * The description of these fields can be found here {@link https://tools.ietf.org/html/rfc7662#section-2.2}
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class OIDCIntrospectResponse {

    private boolean active;

    @JsonProperty("sub")
    private String subject;

    private String scope;

    @JsonProperty("iat")
    private Long issuedAt;

    @JsonProperty("exp")
    private Long expire;

    private String realmName;

    private String uniqueSecurityName;

    private String tokenType;

    @JsonProperty("aud")
    private String audience;

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty("pgc-roles")
    private String role;

    /**
     * Return the boolean indicator of whether or not the presented token
     * is currently active.  The specifics of a token's "active" state
     * will vary depending on the implementation of the authorization
     * server and the information it keeps about its tokens, but a "true"
     * value return for the "active" property will generally indicate
     * that a given token has been issued by this authorization server,
     * has not been revoked by the resource owner, and is within its
     * given time window of validity (e.g., after its issuance time and
     * before its expiration time).  See Section 4 for information on
     * implementation of such checks.
     * 
     * @return true if the token is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set the token status. It should never be used explicitly.
     * 
     * @param active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Subject of the token, as defined in JWT [RFC7519].
     * Usually a machine-readable identifier of the resource owner who
     * authorized this token.
     * @return the user identifier
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Set the token subject. It should never be used explicitly.
     * @param subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Get the scopes granted by the user
     * @return a blank separated list of scopes
     */
    public String getScope() {
        return scope;
    }

    /**
     * Set the token scopes. It should never be used explicitly.
     * @param scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Return the timestamp when the token was issued
     * @return a timestamp in milliseconds
     */
    public Long getIssuedAt() {
        return issuedAt;
    }

    /**
     * Set the token issued time. It should never be used explicitly.
     * @param issuedAt
     */
    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    /**
     * Get the timestamp when the token expire
     * @return A timestamp in milliseconds
     */
    public Long getExpire() {
        return expire;
    }

    /**
     * Set the token expire. It should never be used explicitly.
     * @param expire
     */
    public void setExpire(Long expire) {
        this.expire = expire;
    }

    /**
     * Get the realm name of the authorization server
     * @return String
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * Set the OIDC real. It should never be used explicitly.
     * @param realmName
     */
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * Custom field, it is related to the implementation and could be null. For example, in CAS OIDC implementation,
     * it maps the principal ID.
     * @return custom field value
     */
    public String getUniqueSecurityName() {
        return uniqueSecurityName;
    }

    /**
     * Set the securityName. It should never be used explicitly.
     * @param uniqueSecurityName
     */
    public void setUniqueSecurityName(String uniqueSecurityName) {
        this.uniqueSecurityName = uniqueSecurityName;
    }

    /**
     * The token type (e.g. "Bearer") as defined here {@link https://tools.ietf.org/html/rfc6749#section-7.1}
     * @return the token type
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Set the token type. It should never be used explicitly.
     * @param tokenType
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * The audience provided by the OIDC authorization server
     * @return the audience string
     */
    public String getAudience() {
        return audience;
    }

    /**
     * Set the token audience. It should never be used explicitly.
     * @param audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    /**
     * The "iss" (issuer) claim identifies the principal that issued the
     * JWT.  The processing of this claim is generally application specific.
     * The "iss" value is a case-sensitive string containing a StringOrURI
     * value.  Use of this claim is OPTIONAL.
     * 
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Set the token issuer. It should never be used explicitly.
     * @param issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Client identifier for the OAuth 2.0 client that requested this token.
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Set the client id. It should never be used explicitly.
     * @param clientId
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * The grant_type used to obtain this token (e.g. authorization_code).
     * It is not a standard response field.
     * @return the grant type
     */
    public String getGrantType() {
        return grantType;
    }

    /**
     * Set the grant type. It should never be used explicitly.
     * @param grantType
     */
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * The role associated to this token. It is not a standard field.
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Set the user role. It should never be used explicitly.
     * @param role
     */
    public void setRole(String role) {
        this.role = role;
    }
}
