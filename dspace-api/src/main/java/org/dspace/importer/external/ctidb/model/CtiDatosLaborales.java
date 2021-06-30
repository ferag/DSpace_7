/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.model;

import java.util.Date;

/**
 * Affiliation.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDatosLaborales extends BaseCtiEntity {

    private Date fechaInicio;
    private Date fechaFin;
    private String cargo;
    private Integer institucionId;
    private String institucionRazonSocial;

    public Date getFechaInicio() {
        return fechaInicio;
    }
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public Date getFechaFin() {
        return fechaFin;
    }
    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }
    public String getCargo() {
        return cargo;
    }
    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
    public Integer getInstitucionId() {
        return institucionId;
    }
    public void setInstitucionId(Integer institucionId) {
        this.institucionId = institucionId;
    }
    public String getInstitucionRazonSocial() {
        return institucionRazonSocial;
    }
    public void setInstitucionRazonSocial(String institucionRazonSocial) {
        this.institucionRazonSocial = institucionRazonSocial;
    }
}
