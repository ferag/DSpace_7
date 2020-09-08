/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * { "active": true, "sub": "4992cce9-5976-48bf-bb3b-5b2c5824f0b9", "scope":
 * "email openid profile", "iat": 1599216499, "exp": 1599245299, "realmName":
 * "", "uniqueSecurityName": "4992cce9-5976-48bf-bb3b-5b2c5824f0b9",
 * "tokenType": "bearer", "aud": "http://localhost:8043/app2", "iss":
 * "https://localhost:8082/cas/oidc", "client_id": "client2", "grant_type":
 * "authorization_code" }
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getExpire() {
        return expire;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getUniqueSecurityName() {
        return uniqueSecurityName;
    }

    public void setUniqueSecurityName(String uniqueSecurityName) {
        this.uniqueSecurityName = uniqueSecurityName;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
