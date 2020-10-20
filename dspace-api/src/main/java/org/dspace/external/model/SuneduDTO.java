/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The representation model object for SUNEDU Objects
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SuneduDTO {

    private String id;

    private String country;

    private String university;

    private Map<String, List<String>> educationDegree = new HashMap<String, List<String>>();
    /**
     * Default constructor
     */
    public SuneduDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public Map<String, List<String>> getEducationDegree() {
        return educationDegree;
    }

    public void setEducationDegree(Map<String, List<String>> educationDegree) {
        this.educationDegree = educationDegree;
    }

}