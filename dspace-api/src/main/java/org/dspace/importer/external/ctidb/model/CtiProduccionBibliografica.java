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
 * Publication.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiProduccionBibliografica extends BaseCtiEntity {

    private String titulo;
    private String palabrasClave;
    private String autor;
    private String coautor;
    private String colaboradores;
    private Date fechaProduccion;
    private Integer tipoProduccionBibliograficaId;
    private String tipoProduccionBibliograficadescripcion;
    private CtiInvestigador investigador;
    private List<CtiPerson> autors = new ArrayList<CtiPerson>();

    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getPalabrasClave() {
        return palabrasClave;
    }
    public void setPalabrasClave(String palabrasClave) {
        this.palabrasClave = palabrasClave;
    }
    public String getAutor() {
        return autor;
    }
    public void setAutor(String autor) {
        this.autor = autor;
    }
    public String getCoautor() {
        return coautor;
    }
    public void setCoautor(String coautor) {
        this.coautor = coautor;
    }
    public String getColaboradores() {
        return colaboradores;
    }
    public void setColaboradores(String colaboradores) {
        this.colaboradores = colaboradores;
    }
    public Integer getTipoProduccionBibliograficaId() {
        return tipoProduccionBibliograficaId;
    }
    public void setTipoProduccionBibliograficaId(Integer tipoProduccionBibliograficaId) {
        this.tipoProduccionBibliograficaId = tipoProduccionBibliograficaId;
    }
    public String getTipoProduccionBibliograficadescripcion() {
        return tipoProduccionBibliograficadescripcion;
    }
    public void setTipoProduccionBibliograficadescripcion(String tipoProduccionBibliograficadescripcion) {
        this.tipoProduccionBibliograficadescripcion = tipoProduccionBibliograficadescripcion;
    }
    public Date getFechaProduccion() {
        return fechaProduccion;
    }
    public void setFechaProduccion(Date fechaProduccion) {
        this.fechaProduccion = fechaProduccion;
    }
    public List<CtiPerson> getAutors() {
        return autors;
    }
    public void setAutors(List<CtiPerson> autors) {
        this.autors = autors;
    }
    public CtiInvestigador getInvestigador() {
        return investigador;
    }
    public void setInvestigador(CtiInvestigador investigador) {
        this.investigador = investigador;
    }


}
