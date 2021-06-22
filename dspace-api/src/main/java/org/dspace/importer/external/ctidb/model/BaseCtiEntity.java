/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.model;

/**
 * Base class to represent a record of the Cti Database.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class BaseCtiEntity {

    private Integer ctiId;

    public Integer getCtiId() {
        return ctiId;
    }

    public void setCtiId(Integer ctiId) {
        this.ctiId = ctiId;
    }

}
