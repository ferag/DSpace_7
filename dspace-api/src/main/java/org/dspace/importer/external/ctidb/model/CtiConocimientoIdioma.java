/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.model;

/**
 * Knows Language.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiConocimientoIdioma extends BaseCtiEntity {

    private Integer nivelLectura;
    private Integer nivelEscritura;
    private Integer nivelConversacion;
    private Integer lenguaMaterna;
    private Integer idiomaId;
    private String idiomaNum;
    private String idiomaDescription;

    public Integer getNivelLectura() {
        return nivelLectura;
    }
    public void setNivelLectura(Integer nivelLectura) {
        this.nivelLectura = nivelLectura;
    }
    public Integer getNivelEscritura() {
        return nivelEscritura;
    }
    public void setNivelEscritura(Integer nivelEscritura) {
        this.nivelEscritura = nivelEscritura;
    }
    public Integer getNivelConversacion() {
        return nivelConversacion;
    }
    public void setNivelConversacion(Integer nivelConversacion) {
        this.nivelConversacion = nivelConversacion;
    }
    public Integer getLenguaMaterna() {
        return lenguaMaterna;
    }
    public void setLenguaMaterna(Integer lenguaMaterna) {
        this.lenguaMaterna = lenguaMaterna;
    }
    public Integer getIdiomaId() {
        return idiomaId;
    }
    public void setIdiomaId(Integer idiomaId) {
        this.idiomaId = idiomaId;
    }
    public String getIdiomaNum() {
        return idiomaNum;
    }
    public void setIdiomaNum(String idiomaNum) {
        this.idiomaNum = idiomaNum;
    }
    public String getIdiomaDescription() {
        return idiomaDescription;
    }
    public void setIdiomaDescription(String idiomaDescription) {
        this.idiomaDescription = idiomaDescription;
    }


}
