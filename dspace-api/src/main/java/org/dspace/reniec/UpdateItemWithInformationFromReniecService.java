/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.reniec;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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

    @Override
    public boolean updateItem(Context context, Item item) {
        String dni = itemService.getMetadataFirstValue(item, "perucris", "identifier", "dni", Item.ANY);
        ReniecDTO informationsFromReniec = reniecProvider.getReniecObject(dni);
        return updateCurrentItemWithInformationsFromReniec(context, item, informationsFromReniec);
    }

    private boolean updateCurrentItemWithInformationsFromReniec(Context context, Item currentItem,
            ReniecDTO informationsFromReniec) {
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
        if (!checkMetadata(
                itemService.getMetadataFirstValue(currentItem, "perucris", "domicilio", "ubigeoReniec", null),
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
        if (!checkMetadata(
                itemService.getMetadataFirstValue(currentItem, "perucris", "nacimiento", "ubigeoReniec", null),
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
        if (!checkSexIndex(itemService.getMetadataFirstValue(currentItem, "oairecerif", "person", "gender", null),
             informationsFromReniec.getIndexSex())) {
            return false;
        }
        if (!checkBirthDate(itemService.getMetadataFirstValue(currentItem, "person", "birthDate", null, null),
                informationsFromReniec.getBirthDate())) {
            return false;
        }
        return true;
    }

    private boolean checkMetadata(String itemMetadata, String infoFromReniec) {
        if (StringUtils.isNotBlank(itemMetadata) && StringUtils.isNotBlank(infoFromReniec)) {
            if (itemMetadata.equals(infoFromReniec)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSexIndex(String sexIndex, int sexIndexFromReniec) {
        if (!StringUtils.isNotBlank(sexIndex)) {
            return false;
        } else {
            return Integer.parseInt(sexIndex) != sexIndexFromReniec;
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
                itemService.addMetadata(context, currentItem, "perucris", "domicilio", "ubigeoReniec", null,
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
                itemService.addMetadata(context, currentItem, "perucris", "nacimiento", "ubigeoReniec", null,
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
            if (dto.getIndexSex() == 1 | dto.getIndexSex() == 2) {
                itemService.addMetadata(context, currentItem, "oairecerif", "person", "gender", null,
                            String.valueOf(dto.getIndexSex()));
            }
            if (dto.getBirthDate() != null) {
                String birthDate = dto.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                itemService.addMetadata(context, currentItem, "person", "birthDate", null, null, birthDate);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
}
