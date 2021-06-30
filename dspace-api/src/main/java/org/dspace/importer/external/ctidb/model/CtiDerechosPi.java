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
 * Patent Entity.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDerechosPi extends BaseCtiEntity {

    private Integer tipoPiId;
    private String tituloPi;
    private String nombreProprietario;
    private Integer numeroRegistroPi;
    private Date fechaAceptacion;

    private CtiInvestigador investigador;

    public Integer getTipoPiId() {
        return tipoPiId;
    }
    public void setTipoPiId(Integer tipoPiId) {
        this.tipoPiId = tipoPiId;
    }
    public String getTituloPi() {
        return tituloPi;
    }
    public void setTituloPi(String tituloPi) {
        this.tituloPi = tituloPi;
    }
    public String getNombreProprietario() {
        return nombreProprietario;
    }
    public void setNombreProprietario(String nombreProprietario) {
        this.nombreProprietario = nombreProprietario;
    }
    public Integer getNumeroRegistroPi() {
        return numeroRegistroPi;
    }
    public void setNumeroRegistroPi(Integer numeroRegistroPi) {
        this.numeroRegistroPi = numeroRegistroPi;
    }
    public Date getFechaAceptacion() {
        return fechaAceptacion;
    }
    public void setFechaAceptacion(Date fechaAceptacion) {
        this.fechaAceptacion = fechaAceptacion;
    }
    public CtiInvestigador getInvestigador() {
        return investigador;
    }
    public void setInvestigador(CtiInvestigador investigador) {
        this.investigador = investigador;
    }

}
