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
 * Confidential Data.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDatosConfidenciales extends BaseCtiEntity {

    private String telefonoFijo;
    private String telefonoCelular;
    private Integer tipoDocumentoId;
    private String nroDocumento;
    private Date fechaNacimiento;
    private String direccion;

    public String getTelefonoFijo() {
        return telefonoFijo;
    }
    public void setTelefonoFijo(String telefonoFijo) {
        this.telefonoFijo = telefonoFijo;
    }
    public String getTelefonoCelular() {
        return telefonoCelular;
    }
    public void setTelefonoCelular(String telefonoCelular) {
        this.telefonoCelular = telefonoCelular;
    }
    public Integer getTipoDocumentoId() {
        return tipoDocumentoId;
    }
    public void setTipoDocumentoId(Integer tipoDocumentoId) {
        this.tipoDocumentoId = tipoDocumentoId;
    }
    public String getNroDocumento() {
        return nroDocumento;
    }
    public void setNroDocumento(String nroDocumento) {
        this.nroDocumento = nroDocumento;
    }
    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }
    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

}
