/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.reniec;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of {@link MetadataFieldMapping} used to import metadata from the Reniec Database.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ReniecMetadataFieldMapping implements MetadataFieldMapping<ReniecDTO, MetadataContributor<ReniecDTO>> {

    private static Logger log = LogManager.getLogger(ReniecMetadataFieldMapping.class);

    private String[] identifierMetadata = "perucris.identifier.dni".split("\\.");

    private Map<Integer, String> genderMap;

    @Autowired
    public ReniecMetadataFieldMapping(
            CrisLayoutBoxService crisLayoutBoxService) {
        genderMap = new HashMap<>();
        genderMap.put(1, "m");
        genderMap.put(2, "f");
    }

    @Override
    public MetadatumDTO toDCValue(MetadataFieldConfig field, String value) {
        throw new UnsupportedOperationException("Single field mapping not implemented");
    }

    @Override
    public Collection<MetadatumDTO> resultToDCValueMapping(ReniecDTO record) {
        List<MetadatumDTO> metadataList = parseReniecDTO(record);
        return metadataList;
    }

    private MetadatumDTO toDto(String schema, String element, String qualifier, String value) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();

        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);

        return metadatumDTO;
    }

    public void setIdentifierMetadata(String identifierMetadata) {
        this.identifierMetadata = identifierMetadata.split("\\.");
    }

    private List<MetadatumDTO> parseReniecDTO(ReniecDTO dto) {
        List<MetadatumDTO> metadataList = new LinkedList<>();

        if (dto.getIdentifierDni() != null) {
            metadataList.add(toDto("perucris", "identifier", "dni", dto.getIdentifierDni()));
        }
        if (dto.getFatherLastName() != null) {
            metadataList.add(toDto("perucris", "apellidoPaterno", null, dto.getFatherLastName()));
        }
        if (dto.getMaternalLastName() != null) {
            metadataList.add(toDto("perucris", "apellidoMaterno", null, dto.getMaternalLastName()));
        }
        if (dto.getLastNameMarried() != null) {
            metadataList.add(toDto("perucris", "apellidoCasada", null, dto.getLastNameMarried()));
        }
        if (dto.getNames() != null) {
            metadataList.add(toDto("person", "givenName", null, dto.getNames()));
        }
        if (dto.getHomeCode() != null) {
            metadataList.add(toDto("perucris", "domicilio", "ubigeoReniec", dto.getHomeCode()));
        }
        if (dto.getRegionOfResidence() != null) {
            metadataList.add(toDto("perucris", "domicilio", "region", dto.getRegionOfResidence()));
        }
        if (dto.getProvinceOfResidence() != null) {
            metadataList.add(toDto("perucris", "domicilio", "provincia", dto.getProvinceOfResidence()));
        }
        if (dto.getDistrictOfResidence() != null) {
            metadataList.add(toDto("perucris", "domicilio", "distrito", dto.getDistrictOfResidence()));
        }
        if (dto.getHomeAddress() != null) {
            metadataList.add(toDto("perucris", "domicilio", "direccion", dto.getHomeAddress()));
        }
        if (dto.getNacimientoCode() != null) {
            metadataList.add(toDto("perucris", "nacimiento", "ubigeoReniec", dto.getNacimientoCode()));
        }
        if (dto.getRegionOfBirth() != null) {
            metadataList.add(toDto("perucris", "nacimiento", "region", dto.getRegionOfBirth()));
        }
        if (dto.getProvinceOfBirth() != null) {
            metadataList.add(toDto("perucris", "nacimiento", "provincia", dto.getProvinceOfBirth()));
        }
        if (dto.getDistrictOfBirth() != null) {
            metadataList.add(toDto("perucris", "nacimiento", "distrito", dto.getDistrictOfBirth()));
        }
        if (Objects.nonNull(gender(dto.getIndexSex()))) {
            metadataList.add(toDto("oairecerif", "person", "gender", gender(dto.getIndexSex())));
        }
        if (dto.getBirthDate() != null) {
            String birthDate = dto.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            metadataList.add(toDto("person", "birthDate", null, birthDate));
        }

        return metadataList;

    }

    private String gender(int indexSex) {
        //FIXME evaluate if this mapping can / needs to be configured
        if (!genderMap.containsKey(indexSex)) {
            log.warn("Unknown gender returned from RENIEC: {}", indexSex);
        }
        return genderMap.get(indexSex);
    }

}
