/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Project.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiProyecto extends BaseCtiEntity {

    private String titulo;
    private String descripcion;
    private String palabrasClave;
    private Date fechaInicio;
    private Date fechaFin;
    private String responsable;
    private String colaboradores;
    private Integer institucionLaboralId;
    private Integer institucionColaboradoraId;
    private Integer institucionSubvencionId;
    private Integer institucionSubvencionadoraId;

    private String nombreInstitucionLaboral;
    private String nombreInstitucionColaboradora;
    private String nombreInstitucionSubvencionadora;

    private CtiInvestigador investigador;
    private List<CtiPerson> colaboradoresEntities = new ArrayList<CtiPerson>();

    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public String getPalabrasClave() {
        return palabrasClave;
    }
    public void setPalabrasClave(String palabrasClave) {
        this.palabrasClave = palabrasClave;
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
    public String getResponsable() {
        return responsable;
    }
    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }
    public String getColaboradores() {
        return colaboradores;
    }
    public void setColaboradores(String colaboradores) {
        this.colaboradores = colaboradores;
    }
    public Integer getInstitucionLaboralId() {
        return institucionLaboralId;
    }
    public void setInstitucionLaboralId(Integer institucionLaboralId) {
        this.institucionLaboralId = institucionLaboralId;
    }
    public Integer getInstitucionColaboradoraId() {
        return institucionColaboradoraId;
    }
    public void setInstitucionColaboradoraId(Integer institucionColaboradoraId) {
        this.institucionColaboradoraId = institucionColaboradoraId;
    }
    public Integer getInstitucionSubvencionId() {
        return institucionSubvencionId;
    }
    public void setInstitucionSubvencionId(Integer institucionSubvencionId) {
        this.institucionSubvencionId = institucionSubvencionId;
    }
    public Integer getInstitucionSubvencionadoraId() {
        return institucionSubvencionadoraId;
    }
    public void setInstitucionSubvencionadoraId(Integer institucionSubvencionadoraId) {
        this.institucionSubvencionadoraId = institucionSubvencionadoraId;
    }
    public String getNombreInstitucionLaboral() {
        return nombreInstitucionLaboral;
    }
    public void setNombreInstitucionLaboral(String nombreInstitucionLaboral) {
        this.nombreInstitucionLaboral = nombreInstitucionLaboral;
    }
    public String getNombreInstitucionColaboradora() {
        return nombreInstitucionColaboradora;
    }
    public void setNombreInstitucionColaboradora(String nombreInstitucionColaboradora) {
        this.nombreInstitucionColaboradora = nombreInstitucionColaboradora;
    }
    public String getNombreInstitucionSubvencionadora() {
        return nombreInstitucionSubvencionadora;
    }
    public void setNombreInstitucionSubvencionadora(String nombreInstitucionSubvencionadora) {
        this.nombreInstitucionSubvencionadora = nombreInstitucionSubvencionadora;
    }
    public CtiInvestigador getInvestigador() {
        return investigador;
    }
    public void setInvestigador(CtiInvestigador investigador) {
        this.investigador = investigador;
    }
    public List<CtiPerson> getColaboradoresEntities() {
        return colaboradoresEntities;
    }
    public void setColaboradoresEntities(List<CtiPerson> colaboradoresEntities) {
        this.colaboradoresEntities = colaboradoresEntities;
    }

}
