/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.reniec;

import java.time.LocalDate;

/**
 * The representation model object for RENIEC Object
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ReniecDTO {

    // 1 Apellido Paterno
    private String fatherLastName;

    // 2 Apellido Materno
    private String maternalLastName;

    // 3 Apellido de Casada
    private String lastNameMarried;

    // 4 Nombres
    private String names;

    // 5, 6, 7 Código de (Región-Provincia-Distrito) de Domicilio
    private String homeCode;

    // 8 Descripción de Región de Domicilio
    private String regionOfResidence;

    // 9 Descripción de Provincia de Domicilio
    private String provinceOfResidence;

    // 10 Descripción de Distrito de Domicilio
    private String districtOfResidence;

    //11 Dirección domicilio
    private String homeAddress;

    // 13 Índice Sexo (Valores: 1=masculino ; 2=Femenino)
    private int indexSex;

    // 14, 15 16 Código de (Región-Provincia-Distrito) de Nacimiento
    private String nacimientoCode;

    // 17 Descripción de Región de Nacimiento
    private String regionOfBirth;

    // 18 Descripción de Provincia de Nacimiento
    private String provinceOfBirth;

    // 19 Descripción de Distrito de Nacimiento
    private String districtOfBirth;

    // 20 Fecha de Nacimiento. Formato yyyymmdd
    private LocalDate birthDate;

    // 22 Número de  DNI
    private String identifierDni;

    public String getFatherLastName() {
        return fatherLastName;
    }

    public void setFatherLastName(String fatherLastName) {
        this.fatherLastName = fatherLastName;
    }

    public String getMaternalLastName() {
        return maternalLastName;
    }

    public void setMaternalLastName(String maternalLastName) {
        this.maternalLastName = maternalLastName;
    }

    public String getLastNameMarried() {
        return lastNameMarried;
    }

    public void setLastNameMarried(String lastNameMarried) {
        this.lastNameMarried = lastNameMarried;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getHomeCode() {
        return homeCode;
    }

    public void setHomeCode(String homeCode) {
        this.homeCode = homeCode;
    }

    public String getRegionOfResidence() {
        return regionOfResidence;
    }

    public void setRegionOfResidence(String regionOfResidence) {
        this.regionOfResidence = regionOfResidence;
    }

    public String getProvinceOfResidence() {
        return provinceOfResidence;
    }

    public void setProvinceOfResidence(String provinceOfResidence) {
        this.provinceOfResidence = provinceOfResidence;
    }

    public String getDistrictOfResidence() {
        return districtOfResidence;
    }

    public void setDistrictOfResidence(String districtOfResidence) {
        this.districtOfResidence = districtOfResidence;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public int getIndexSex() {
        return indexSex;
    }

    public void setIndexSex(int indexSex) {
        this.indexSex = indexSex;
    }

    public String getNacimientoCode() {
        return nacimientoCode;
    }

    public void setNacimientoCode(String nacimientoCode) {
        this.nacimientoCode = nacimientoCode;
    }

    public String getRegionOfBirth() {
        return regionOfBirth;
    }

    public void setRegionOfBirth(String regionOfBirth) {
        this.regionOfBirth = regionOfBirth;
    }

    public String getProvinceOfBirth() {
        return provinceOfBirth;
    }

    public void setProvinceOfBirth(String provinceOfBirth) {
        this.provinceOfBirth = provinceOfBirth;
    }

    public String getDistrictOfBirth() {
        return districtOfBirth;
    }

    public void setDistrictOfBirth(String districtOfBirth) {
        this.districtOfBirth = districtOfBirth;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getIdentifierDni() {
        return identifierDni;
    }

    public void setIdentifierDni(String identifierDni) {
        this.identifierDni = identifierDni;
    }
}