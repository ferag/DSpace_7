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
 * Education.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiFormacionAcademica extends BaseCtiEntity {

    private String titulo;

    private Date fechaInicio;

    private Date fechaFin;

    private String centroEstudiosNombre;

    private Integer centroEstudiosId; // institucion_id

    private Integer gradoAcademicoId;

    private String gradoAcademicoDescripcion;

    private String centroEstudiosPaisNombre;

    private Integer centroEstudiosPaisId;

    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
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
    public Integer getGradoAcademicoId() {
        return gradoAcademicoId;
    }
    public void setGradoAcademicoId(Integer gradoAcademicoId) {
        this.gradoAcademicoId = gradoAcademicoId;
    }
    public String getGradoAcademicoDescripcion() {
        return gradoAcademicoDescripcion;
    }
    public void setGradoAcademicoDescripcion(String gradoAcademicoDescripcion) {
        this.gradoAcademicoDescripcion = gradoAcademicoDescripcion;
    }
    public String getCentroEstudiosNombre() {
        return centroEstudiosNombre;
    }
    public void setCentroEstudiosNombre(String centroEstudiosNombre) {
        this.centroEstudiosNombre = centroEstudiosNombre;
    }
    public Integer getCentroEstudiosId() {
        return centroEstudiosId;
    }
    public void setCentroEstudiosId(Integer centroEstudiosId) {
        this.centroEstudiosId = centroEstudiosId;
    }
    public String getCentroEstudiosPaisNombre() {
        return centroEstudiosPaisNombre;
    }
    public void setCentroEstudiosPaisNombre(String centroEstudiosPaisNombre) {
        this.centroEstudiosPaisNombre = centroEstudiosPaisNombre;
    }
    public Integer getCentroEstudiosPaisId() {
        return centroEstudiosPaisId;
    }
    public void setCentroEstudiosPaisId(Integer centroEstudiosPaisId) {
        this.centroEstudiosPaisId = centroEstudiosPaisId;
    }



}
