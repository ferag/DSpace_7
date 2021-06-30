/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.Strings;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.importer.external.ctidb.model.CtiConocimientoIdioma;
import org.dspace.importer.external.ctidb.model.CtiDatosConfidenciales;
import org.dspace.importer.external.ctidb.model.CtiDatosLaborales;
import org.dspace.importer.external.ctidb.model.CtiFormacionAcademica;
import org.dspace.importer.external.ctidb.model.CtiInvestigador;

/**
 * Map Cti Entities to MetadataValueDTOs suitable for CvProfile.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CtiProfileMapper extends AbstractCtiMapper {

    public List<MetadataValueDTO> mapCtiProfile(
            CtiInvestigador investigadorBaseInfo,
            List<CtiDatosConfidenciales> datosConfidenciales,
            List<CtiDatosLaborales> datosLaborales,
            List<CtiFormacionAcademica> formacionAcademica,
            List<CtiConocimientoIdioma> conocimientoIdioma) {

        List<MetadataValueDTO> parseInvestigador = this.parseInvestigador(investigadorBaseInfo);

        List<MetadataValueDTO> parseDatosConfidenciales = this.parseDatosConfidenciales(datosConfidenciales);

        List<MetadataValueDTO> parseDatosLaborales = this.parseDatosLaborales(datosLaborales);

        List<MetadataValueDTO> parseMainAffiliation = this.parseMainAffiliation(datosLaborales,
                investigadorBaseInfo.getInstitucionLaboralId());

        List<MetadataValueDTO> parseConocimientoIdioma = this.parseConocimientoIdioma(conocimientoIdioma);

        List<MetadataValueDTO> parseFormacionAcademica = this.parseFormacionAcademica(formacionAcademica);

        List<MetadataValueDTO> metadata = Stream.of(
                parseInvestigador,
                parseDatosConfidenciales,
                parseDatosLaborales,
                parseMainAffiliation,
                parseConocimientoIdioma,
                parseFormacionAcademica)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        metadata.add(getCtiIdentifier(investigadorBaseInfo));

        return metadata;

    }

    private List<MetadataValueDTO> parseFormacionAcademica(List<CtiFormacionAcademica> ctiFormacionAcademica) {
        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();
        if (ctiFormacionAcademica.isEmpty()) {
            return metadata;
        }

        List<CtiFormacionAcademica> sorted = ctiFormacionAcademica.stream()
                .filter(o -> o.getFechaInicio() == null)
                .collect(Collectors.toList());

        sorted.addAll(ctiFormacionAcademica.stream()
                .filter(o -> o.getFechaInicio() != null)
                .sorted((o1, o2) -> o1.getFechaInicio().compareTo(o2.getFechaInicio()))
                .collect(Collectors.toList()));

        for (CtiFormacionAcademica formacionAcademica : sorted) {

            if (!Strings.isBlank(formacionAcademica.getTitulo())) {

                final String fechaInicio = formacionAcademica.getFechaInicio() != null ?
                        mapDate(formacionAcademica.getFechaInicio()) : EMPTY;

                final String fechaFin = formacionAcademica.getFechaFin() != null ?
                        mapDate(formacionAcademica.getFechaFin()) : EMPTY;

                final String gradoAcademico = !Strings.isBlank(formacionAcademica.getGradoAcademicoDescripcion()) ?
                        formacionAcademica.getGradoAcademicoDescripcion().trim() : EMPTY;

                final String centroEstudiosNombre = !Strings.isBlank(formacionAcademica.getCentroEstudiosNombre()) ?
                        formacionAcademica.getCentroEstudiosNombre().trim() : EMPTY;

                final String centroEstudioPaisNombre =
                        !Strings.isBlank(formacionAcademica.getCentroEstudiosPaisNombre()) ?
                        formacionAcademica.getCentroEstudiosPaisNombre().trim() : EMPTY;

                metadata.add(new MetadataValueDTO("crisrp", "education", null, null, formacionAcademica.getTitulo()));
                metadata.add(new MetadataValueDTO("crisrp", "education", "start", null, fechaInicio));
                metadata.add(new MetadataValueDTO("crisrp", "education", "end", null, fechaFin));
                metadata.add(new MetadataValueDTO("crisrp", "education", "role", null, gradoAcademico));
                metadata.add(new MetadataValueDTO("perucris", "education", "grantor", null, centroEstudiosNombre));
                metadata.add(new MetadataValueDTO("perucris", "education", "country", null, centroEstudioPaisNombre));

            }
        }

        return metadata;
    }

    private List<MetadataValueDTO> parseConocimientoIdioma(List<CtiConocimientoIdioma> ctiConocimientoIdioma) {
        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();
        if (ctiConocimientoIdioma.isEmpty()) {
            return metadata;
        }

        for (CtiConocimientoIdioma conocimientoIdioma : ctiConocimientoIdioma) {
            metadata.add(new MetadataValueDTO("person", "knowsLanguage", null, null,
                    conocimientoIdioma.getIdiomaDescription()));
        }

        return metadata;
    }

    private List<MetadataValueDTO> parseDatosLaborales(List<CtiDatosLaborales> ctiDatosLaborales) {
        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();
        if (ctiDatosLaborales.isEmpty()) {
            return metadata;
        }

        List<CtiDatosLaborales> sorted = ctiDatosLaborales.stream()
            .sorted((o1, o2) -> o1.getFechaInicio().compareTo(o2.getFechaInicio()))
            .collect(Collectors.toList());

        for (CtiDatosLaborales datosLaborales : sorted) {
            if (!Strings.isBlank(datosLaborales.getInstitucionRazonSocial())
                    && !Strings.isBlank(datosLaborales.getCargo())) {

                final String razonSocial = datosLaborales.getInstitucionRazonSocial().trim();
                final String cargo = datosLaborales.getCargo().trim();

                final String fechaInicio = datosLaborales.getFechaInicio() != null ?
                        mapDate(datosLaborales.getFechaInicio()) : EMPTY;

                final String fechaFin = datosLaborales.getFechaFin() != null ?
                        mapDate(datosLaborales.getFechaFin()) : EMPTY;

                metadata.add(new MetadataValueDTO("oairecerif", "person", "affiliation", null, razonSocial));
                metadata.add(new MetadataValueDTO("oairecerif", "affiliation", "startDate", null, fechaInicio));
                metadata.add(new MetadataValueDTO("oairecerif", "affiliation", "endDate", null, fechaFin));
                metadata.add(new MetadataValueDTO("oairecerif", "affiliation", "role", null, cargo));
            }
        }

        return metadata;

    }

    private List<MetadataValueDTO> parseMainAffiliation(List<CtiDatosLaborales> ctiDatosLaborales,
            Integer institucionLaboralId) {

        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();
        if (ctiDatosLaborales.isEmpty() || institucionLaboralId == null) {
            return metadata;
        }

        Optional<CtiDatosLaborales> mainAffiliation = ctiDatosLaborales.stream()
                .filter(s -> !Strings.isBlank(s.getInstitucionRazonSocial()))
                .filter(s -> !Strings.isBlank(s.getCargo()))
                .filter(s -> s.getInstitucionId().equals(institucionLaboralId))
                .findFirst();

        // main affiliation
        if (mainAffiliation.isPresent()) {
            metadata.add(new MetadataValueDTO("person", "jobTitle", null, null,
                    mainAffiliation.get().getCargo().trim()));
            metadata.add(new MetadataValueDTO("person", "affiliation", "name", null,
                    mainAffiliation.get().getInstitucionRazonSocial().trim()));
        }

        return metadata;
    }

    private List<MetadataValueDTO> parseDatosConfidenciales(List<CtiDatosConfidenciales> ctiDatosConfidenciales) {

        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();

        if (ctiDatosConfidenciales.isEmpty()) {
            return metadata;
        }

        CtiDatosConfidenciales datosConfidenciales = ctiDatosConfidenciales.get(0);
        if (datosConfidenciales.getTipoDocumentoId() != null
                && datosConfidenciales.getTipoDocumentoId() == 1) { // dni
            metadata.add(new MetadataValueDTO("perucris", "identifier", "dni", null,
                    datosConfidenciales.getNroDocumento().trim()));
        }

        if (datosConfidenciales.getFechaNacimiento() != null) {
            metadata.add(new MetadataValueDTO("person", "birthDate", null, null,
                    mapDate(datosConfidenciales.getFechaNacimiento())));
        }

        if (!Strings.isBlank(datosConfidenciales.getTelefonoFijo())) {
            metadata.add(new MetadataValueDTO("perucris", "phone", null, null,
                    datosConfidenciales.getTelefonoFijo().trim()));
        }

        if (!Strings.isBlank(datosConfidenciales.getTelefonoCelular())) {
            metadata.add(new MetadataValueDTO("perucris", "mobilePhone", null, null,
                    datosConfidenciales.getTelefonoCelular().trim()));
        }

        return metadata;
    }


    private List<MetadataValueDTO> parseInvestigador(CtiInvestigador ctiInvestigador) {

        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();

        if (!Strings.isBlank(ctiInvestigador.getApellidoPaterno())) {
            metadata.add(new MetadataValueDTO("perucris", "apellidoPaterno", null, null,
                    ctiInvestigador.getApellidoPaterno().trim()));
        }
        if (!Strings.isBlank(ctiInvestigador.getApellidoMaterno())) {
            metadata.add(new MetadataValueDTO("perucris", "apellidoMaterno", null, null,
                    ctiInvestigador.getApellidoMaterno().trim()));
        }

        if (!Strings.isBlank(ctiInvestigador.getApellidoPaterno())
                || !Strings.isBlank(ctiInvestigador.getApellidoMaterno())) {
            final String familyName =
                    Stream.of(ctiInvestigador.getApellidoPaterno(), ctiInvestigador.getApellidoMaterno())
                    .filter(s -> !Strings.isBlank(s))
                    .map(s -> s.trim())
                    .collect(Collectors.joining(" "));
            metadata.add(new MetadataValueDTO("person", "familyName", null, null, familyName));
        }

        if (!Strings.isBlank(ctiInvestigador.getNombres())) {
            metadata.add(new MetadataValueDTO("person", "givenName", null, null, ctiInvestigador.getNombres().trim()));
        }

        final String dcTitle = Stream.of(
                ctiInvestigador.getApellidoPaterno(),
                ctiInvestigador.getApellidoMaterno(),
                ctiInvestigador.getNombres())
                .filter(s -> !Strings.isBlank(s))
                .map(s -> s.trim())
                .collect(Collectors.joining(" "));
        metadata.add(new MetadataValueDTO("dc", "title", null, null, dcTitle));

        final String crispName = Stream.of(
                ctiInvestigador.getApellidoPaterno(),
                ctiInvestigador.getApellidoMaterno(),
                ctiInvestigador.getNombres())
                .filter(s -> !Strings.isBlank(s))
                .map(s -> s.trim())
                .collect(Collectors.joining(" "));
        metadata.add(new MetadataValueDTO("crisrp", "name", null, null, crispName));

        if (!Strings.isBlank(ctiInvestigador.getEmail())) {
            metadata.add(new MetadataValueDTO("person", "email", null, null,
                    ctiInvestigador.getEmail().trim()));
        }

        if (!Strings.isBlank(ctiInvestigador.getDireccionWeb())) {
            metadata.add(new MetadataValueDTO("oairecerif", "identifier", "url", null,
                    ctiInvestigador.getDireccionWeb().trim()));
        }

        metadata.add(new MetadataValueDTO("perucris", "identifier", "dina", null,
                ctiInvestigador.getCtiId().toString()));
        if (!Strings.isBlank(ctiInvestigador.getIdOrcid())) {
            metadata.add(new MetadataValueDTO("person", "identifier", "orcid", null,
                    ctiInvestigador.getIdOrcid().trim()));
        }

        if (!Strings.isBlank(ctiInvestigador.getIdPerfilScopus())) {
            metadata.add(new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                    ctiInvestigador.getIdPerfilScopus().trim()));
        }



        if (ctiInvestigador.getSexo() != null && ctiInvestigador.getSexo().equals(1)) {
            metadata.add(new MetadataValueDTO("oairecerif", "person", "gender", null, "Femenino"));
        }

        if (ctiInvestigador.getSexo() != null && ctiInvestigador.getSexo().equals(2)) {
            metadata.add(new MetadataValueDTO("oairecerif", "person", "gender", null, "Masculino"));
        }

        if (ctiInvestigador.getDepartamentoDescr() != null
                || ctiInvestigador.getProvinciaDesc() != null
                || ctiInvestigador.getDistritoDesc() != null) {

            String ubigeo = validateAndFormatUbigeo(ctiInvestigador.getDepartamentoDescr(),
                    ctiInvestigador.getProvinciaDesc(),
                    ctiInvestigador.getDistritoDesc());

            if (ubigeo != null) {
                metadata.add(new MetadataValueDTO("perucris", "ubigeo", null, null, ubigeo));
            }

            // TODO create metadata
//            if (!Strings.isBlank(ctiInvestigador.getDepartamentoDescr())) {
//                metadata.add(new MetadataValueDTO("person", "address", "addressRegion", null,
//                        ctiInvestigador.getDepartamentoDescr().trim()));
//            }
//            if (!Strings.isBlank(ctiInvestigador.getProvinciaDesc())) {
//                metadata.add(new MetadataValueDTO("person", "address", "addressLocality", null,
//                        ctiInvestigador.getProvinciaDesc().trim()));
//            }
//            if (!Strings.isBlank(ctiInvestigador.getDistritoDesc())) {
//                metadata.add(new MetadataValueDTO("person", "address", "distrito", null,
//                        ctiInvestigador.getDistritoDesc().trim()));
//            }
        }

        if (!Strings.isBlank(ctiInvestigador.getPaisResidenciaNombre())) {
            metadata.add(new MetadataValueDTO("perucris", "address", "addressCountry", null,
                    ctiInvestigador.getPaisResidenciaNombre().trim()));
        }

        if (!Strings.isBlank(ctiInvestigador.getPaisNacimientoNombre())) {
            metadata.add(new MetadataValueDTO("crisrp", "country", null, null,
                    ctiInvestigador.getPaisNacimientoNombre().trim()));
        }

        return metadata;
    }

}
