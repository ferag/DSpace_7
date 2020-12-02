/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.model;

public class OIDCProfileElementsResponse {

    private String sub;

    private String pgcRole;

    private String reniecDni;

    private String orcid;

    private String email;

    private String familyName;

    private String givenName;

    private String birthdate;

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getPgcRole() {
        return pgcRole;
    }

    public void setPgcRole(String pgcRole) {
        this.pgcRole = pgcRole;
    }


    public String getReniecDni() {
        return reniecDni;
    }

    public void setReniecDni(String reniecDni) {
        this.reniecDni = reniecDni;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

}
