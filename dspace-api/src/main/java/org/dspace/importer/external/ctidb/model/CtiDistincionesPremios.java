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
 * Researcher's Qualifications (?).
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDistincionesPremios extends BaseCtiEntity {

    private Integer institucionId;
    private Integer paisId;
    private String distincion;
    private String descripcion;
    private Date fechaInicio;
    private Date fechaFin;
    private String nombreInstitucion;

    public Integer getInstitucionId() {
        return institucionId;
    }
    public void setInstitucionId(Integer institucionId) {
        this.institucionId = institucionId;
    }
    public Integer getPaisId() {
        return paisId;
    }
    public void setPaisId(Integer paisId) {
        this.paisId = paisId;
    }
    public String getDistincion() {
        return distincion;
    }
    public void setDistincion(String distincion) {
        this.distincion = distincion;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
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
    public String getNombreInstitucion() {
        return nombreInstitucion;
    }
    public void setNombreInstitucion(String nombreInstitucion) {
        this.nombreInstitucion = nombreInstitucion;
    }

}
