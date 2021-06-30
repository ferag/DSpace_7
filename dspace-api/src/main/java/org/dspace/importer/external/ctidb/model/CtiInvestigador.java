/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.model;

/**
 * Researcher.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiInvestigador extends BaseCtiEntity {

    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer sexo;
    private String email;
    private String direccionWeb;
    private String descPersonal;
    private Integer paisNacimientoId;
    private String paisNacimientoNombre;
    private Integer paisResidenciaId;
    private String paisResidenciaNombre;
    private String idOrcid;
    private String idPerfilScopus;
    private Integer departamentoId;
    private String departamentoDescr;
    private Integer provinciaId;
    private String provinciaDesc;
    private Integer distritoId;
    private String distritoDesc;
    private Integer institucionLaboralId;


    public String getNombres() {
        return nombres;
    }
    public void setNombres(String nombres) {
        this.nombres = nombres;
    }
    public String getApellidoPaterno() {
        return apellidoPaterno;
    }
    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }
    public String getApellidoMaterno() {
        return apellidoMaterno;
    }
    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }
    public Integer getSexo() {
        return sexo;
    }
    public void setSexo(Integer sexo) {
        this.sexo = sexo;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getDireccionWeb() {
        return direccionWeb;
    }
    public void setDireccionWeb(String direccionWeb) {
        this.direccionWeb = direccionWeb;
    }
    public String getDescPersonal() {
        return descPersonal;
    }
    public void setDescPersonal(String descPersonal) {
        this.descPersonal = descPersonal;
    }
    public Integer getPaisNacimientoId() {
        return paisNacimientoId;
    }
    public void setPaisNacimientoId(Integer paisNacimientoId) {
        this.paisNacimientoId = paisNacimientoId;
    }
    public String getPaisNacimientoNombre() {
        return paisNacimientoNombre;
    }
    public void setPaisNacimientoNombre(String paisNacimientoNombre) {
        this.paisNacimientoNombre = paisNacimientoNombre;
    }
    public Integer getPaisResidenciaId() {
        return paisResidenciaId;
    }
    public void setPaisResidenciaId(Integer paisResidenciaId) {
        this.paisResidenciaId = paisResidenciaId;
    }
    public String getPaisResidenciaNombre() {
        return paisResidenciaNombre;
    }
    public void setPaisResidenciaNombre(String paisResidenciaNombre) {
        this.paisResidenciaNombre = paisResidenciaNombre;
    }
    public String getIdOrcid() {
        return idOrcid;
    }
    public void setIdOrcid(String idOrcid) {
        this.idOrcid = idOrcid;
    }
    public String getIdPerfilScopus() {
        return idPerfilScopus;
    }
    public void setIdPerfilScopus(String idPerfilScopus) {
        this.idPerfilScopus = idPerfilScopus;
    }
    public Integer getDepartamentoId() {
        return departamentoId;
    }
    public void setDepartamentoId(Integer departamentoId) {
        this.departamentoId = departamentoId;
    }
    public String getDepartamentoDescr() {
        return departamentoDescr;
    }
    public void setDepartamentoDescr(String departamentoDescr) {
        this.departamentoDescr = departamentoDescr;
    }
    public Integer getProvinciaId() {
        return provinciaId;
    }
    public void setProvinciaId(Integer provinciaId) {
        this.provinciaId = provinciaId;
    }
    public String getProvinciaDesc() {
        return provinciaDesc;
    }
    public void setProvinciaDesc(String provinciaDesc) {
        this.provinciaDesc = provinciaDesc;
    }
    public Integer getDistritoId() {
        return distritoId;
    }
    public void setDistritoId(Integer distritoId) {
        this.distritoId = distritoId;
    }
    public String getDistritoDesc() {
        return distritoDesc;
    }
    public void setDistritoDesc(String distritoDesc) {
        this.distritoDesc = distritoDesc;
    }
    public Integer getInstitucionLaboralId() {
        return institucionLaboralId;
    }
    public void setInstitucionLaboralId(Integer institucionLaboralId) {
        this.institucionLaboralId = institucionLaboralId;
    }


}
