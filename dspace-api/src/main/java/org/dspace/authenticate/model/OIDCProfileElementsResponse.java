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

}
