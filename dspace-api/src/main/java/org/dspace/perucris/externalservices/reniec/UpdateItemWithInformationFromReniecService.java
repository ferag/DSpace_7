/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.reniec;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkimport.model.MetadataValueVO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.perucris.externalservices.PeruExternalService;
import org.dspace.perucris.externalservices.UbigeoMapping;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithInformationFromReniecService implements PeruExternalService {
    private static Logger log = LogManager.getLogger(UpdateItemWithInformationFromReniecService.class);

    @Autowired
    private ReniecProvider reniecProvider;

    @Autowired
    private ItemService itemService;

    private Map<Integer, String> genderMap;

    @Autowired
    private UbigeoMapping ubigeoMapping;

    public UpdateItemWithInformationFromReniecService() {
        //FIXME: evaluate if this map needs to / can be injected
        genderMap = new HashMap<>();
        genderMap.put(1, "m");
        genderMap.put(2, "f");
    }

    @Override
    public boolean updateItem(Context context, Item item) {
        String dni = itemService.getMetadataFirstValue(item, "perucris", "identifier", "dni", Item.ANY);
        ReniecDTO informationsFromReniec = reniecProvider.getReniecObject(dni);
        return updateCurrentItemWithInformationsFromReniec(context, item, informationsFromReniec);
    }

    private boolean updateCurrentItemWithInformationsFromReniec(Context context, Item currentItem,
            ReniecDTO informationsFromReniec) {
        if (Objects.isNull(informationsFromReniec)) {
            return false;
        }
        if (!checkCurrentItemWithInformationFromReniec(currentItem, informationsFromReniec)) {
            cleanMetadata(context, currentItem);
            addMetadata(context, currentItem, informationsFromReniec);
            return true;
        }
        return false;
    }

    private boolean checkCurrentItemWithInformationFromReniec(Item currentItem, ReniecDTO informationsFromReniec) {
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "apellidoPaterno", null, null),
                informationsFromReniec.getFatherLastName())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "apellidoMaterno", null, null),
                informationsFromReniec.getMaternalLastName())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "apellidoCasada", null, null),
                informationsFromReniec.getLastNameMarried())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "person", "givenName", null, null),
                informationsFromReniec.getNames())) {
            return false;
        }
//        if (!checkMetadata(
//                itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "ubigeoReniec", null),
//                informationsFromReniec.getHomeCode())) {
//            return false;
//        }
        if (!checkMetadataAuthority(
            itemService.getMetadata(currentItem, "perucris", "domicilio", "ubigeoReniec", null),
            informationsFromReniec.getHomeCode())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "region", null),
                informationsFromReniec.getRegionOfResidence())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "provincia", null),
                informationsFromReniec.getProvinceOfResidence())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "distrito", null),
                informationsFromReniec.getDistrictOfResidence())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "direccion", null),
                informationsFromReniec.getHomeAddress())) {
            return false;
        }
//        if (!checkMetadata(
//                itemService.getMetadataFirstValue(currentItem, "perucris", "nacimiento", "ubigeoReniec", null),
//                informationsFromReniec.getNacimientoCode())) {
//            return false;
//        }
        if (!checkMetadataAuthority(
            itemService.getMetadata(currentItem, "perucris", "nacimiento", "ubigeoReniec", null),
            informationsFromReniec.getNacimientoCode())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "nacimiento", "region", null),
                informationsFromReniec.getRegionOfBirth())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "nacimiento", "provincia", null),
                informationsFromReniec.getProvinceOfBirth())) {
            return false;
        }
        if (!checkMetadata(itemService.getMetadataFirstValue(currentItem, "perucris", "nacimiento", "distrito", null),
                informationsFromReniec.getDistrictOfBirth())) {
            return false;
        }
        if (!checkGender(itemService.getMetadataFirstValue(currentItem, "oairecerif", "person", "gender", null),
            gender(informationsFromReniec.getIndexSex()))) {
            return false;
        }
        return checkBirthDate(itemService.getMetadataFirstValue(currentItem, "person", "birthDate", null, null),
            informationsFromReniec.getBirthDate());
    }

    private String gender(int indexSex) {
        //FIXME evaluate if this mapping can / needs to be configured
        if (!genderMap.containsKey(indexSex)) {
            log.warn("Unknown gender returned from RENIEC: {}", indexSex);
        }
        return genderMap.get(indexSex);
    }

    private boolean checkMetadata(String itemMetadata, String infoFromReniec) {
        return StringUtils.equals(itemMetadata, infoFromReniec);
    }

    private boolean checkMetadataAuthority(List<MetadataValue> itemMetadata, String authorityFromReniec) {
        if (Objects.isNull(itemMetadata) || itemMetadata.isEmpty()) {
            return false;
        }
        return StringUtils.equals(itemMetadata.get(0).getAuthority(), authorityFromReniec);
    }

    private boolean checkGender(String gender, String genderFromReniec) {
        if (StringUtils.isBlank(gender)) {
            return false;
        } else {
            return !gender.equals(genderFromReniec);
        }
    }

    private boolean checkBirthDate(String birthDate, LocalDate birthDateFromReniec) {
        if (birthDate != null && birthDateFromReniec != null) {
            String bd = birthDateFromReniec.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return bd.equals(birthDate);
        }
        return false;
    }

    private void cleanMetadata(Context context, Item currentItem) {
        try {
            itemService.clearMetadata(context, currentItem, "perucris", "apellidoPaterno", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "apellidoMaterno", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "apellidoCasada", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "domicilio", "ubigeoReniec", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "domicilio", "region", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "domicilio", "provincia", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "domicilio", "distrito", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "nacimiento", "ubigeoReniec", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "nacimiento", "region", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "nacimiento", "provincia", Item.ANY);
            itemService.clearMetadata(context, currentItem, "perucris", "nacimiento", "distrito", Item.ANY);
            itemService.clearMetadata(context, currentItem, "person", "givenName", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "person", "birthDate", null, Item.ANY);
            itemService.clearMetadata(context, currentItem, "oairecerif", "person", "gender", Item.ANY);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addMetadata(Context context, Item currentItem, ReniecDTO dto) {
        try {
            if (dto.getFatherLastName() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "apellidoPaterno", null, null,
                        dto.getFatherLastName());
            }
            if (dto.getMaternalLastName() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "apellidoMaterno", null, null,
                        dto.getMaternalLastName());
            }
            if (dto.getLastNameMarried() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "apellidoCasada", null, null,
                        dto.getLastNameMarried());
            }
            if (dto.getNames() != null) {
                itemService.addMetadata(context, currentItem, "person", "givenName", null, null,
                        dto.getNames());
            }
            if (dto.getHomeCode() != null) {
                addMappedUbigeoValue(context, currentItem, "perucris", "domicilio", "ubigeoReniec",
                    dto.getHomeCode());
            }
            if (dto.getRegionOfResidence() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "domicilio", "region", null,
                        dto.getRegionOfResidence());
            }
            if (dto.getProvinceOfResidence() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "domicilio", "provincia", null,
                        dto.getProvinceOfResidence());
            }
            if (dto.getDistrictOfResidence() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "domicilio", "distrito", null,
                        dto.getDistrictOfResidence());
            }
            if (dto.getHomeAddress() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "domicilio", "direccion", null,
                        dto.getHomeAddress());
            }
            if (dto.getNacimientoCode() != null) {
                addMappedUbigeoValue(context, currentItem, "perucris", "nacimiento", "ubigeoReniec",
                    dto.getNacimientoCode());
            }
            if (dto.getRegionOfBirth() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "nacimiento", "region", null,
                        dto.getRegionOfBirth());
            }
            if (dto.getProvinceOfBirth() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "nacimiento", "provincia", null,
                        dto.getProvinceOfBirth());
            }
            if (dto.getDistrictOfBirth() != null) {
                itemService.addMetadata(context, currentItem, "perucris", "nacimiento", "distrito", null,
                        dto.getDistrictOfBirth());
            }
            if (Objects.nonNull(gender(dto.getIndexSex()))) {
                itemService.addMetadata(context, currentItem, "oairecerif", "person", "gender", null,
                            gender(dto.getIndexSex()));
            }
            if (dto.getBirthDate() != null) {
                String birthDate = dto.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                itemService.addMetadata(context, currentItem, "person", "birthDate", null, null, birthDate);
            }
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addMappedUbigeoValue(Context context, Item item, String schema, String element, String qualifier,
                                      String ubigeoValue) throws SQLException, AuthorizeException {
        MetadataValueVO metadataValueVO = ubigeoMapping.convert("reniec", ubigeoValue);
        MetadataValue metadata =
            itemService.addMetadata(context, item, schema, element, qualifier, null, metadataValueVO.getValue());

        metadata.setAuthority(metadataValueVO.getAuthority());
        metadata.setConfidence(metadataValueVO.getConfidence());

        itemService.update(context, item);
    }
}
