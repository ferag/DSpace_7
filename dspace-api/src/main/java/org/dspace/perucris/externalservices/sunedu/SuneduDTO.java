/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.sunedu;

/**
 * The representation model object for SUNEDU Objects
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SuneduDTO {

    private String country;

    private String university;

    private String abreviaturaTitulo;

    private String professionalQualification;

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

    public String getAbreviaturaTitulo() {
        return abreviaturaTitulo;
    }

    public void setAbreviaturaTitulo(String abreviaturaTitulo) {
        this.abreviaturaTitulo = abreviaturaTitulo;
    }

    public String getProfessionalQualification() {
        return professionalQualification;
    }

    public void setProfessionalQualification(String professionalQualification) {
        this.professionalQualification = professionalQualification;
    }
}