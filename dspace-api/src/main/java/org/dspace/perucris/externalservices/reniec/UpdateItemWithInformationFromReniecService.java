/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.reniec;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
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
    private ExternalDataService externalDataService;

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
        Optional<ExternalDataObject> externalDataObject = externalDataService.getExternalDataObject("reniec", dni);
        return updateCurrentItemWithInformationsFromReniec(context, item, externalDataObject);
    }

    private boolean updateCurrentItemWithInformationsFromReniec(Context context, Item currentItem,
            Optional<ExternalDataObject> externalDataObject) {

        if (externalDataObject.isEmpty()) {
            return false;
        }
        if (!checkCurrentItemWithInformationFromReniec(currentItem, externalDataObject.get())) {
            cleanMetadata(context, currentItem);
            addMetadata(context, currentItem, externalDataObject.get());
            return true;
        }
        return false;
    }

    private boolean checkCurrentItemWithInformationFromReniec(Item currentItem, ExternalDataObject externalDataObject) {
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "apellidoPaterno", null, null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "apellidoMaterno", null, null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "apellidoCasada", null, null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "givenName", null, null)) {
            return false;
        }
        if (!checkMetadataAuthority2(currentItem, externalDataObject, "perucris", "domicilio", "ubigeoReniec", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "domicilio", "region", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "domicilio", "provincia", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "domicilio", "distrito", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "domicilio", "direccion", null)) {
            return false;
        }
        if (!checkMetadataAuthority2(currentItem, externalDataObject, "perucris", "nacimiento", "ubigeoReniec", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "nacimiento", "region", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "nacimiento", "provincia", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "perucris", "nacimiento", "distrito", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "oairecerif", "person", "gender", null)) {
            return false;
        }
        if (!checkMetadata2(currentItem, externalDataObject, "person", "birthDate", null, null)) {
            return false;
        }
        return true;
    }

    private String getFirstMetadataValue(ExternalDataObject object,
            String schema, String element, String qualifier, String language) {

        Optional<MetadataValueDTO> firstValue =
                object.getMetadata().stream().filter(m -> StringUtils.equals(m.getSchema(), schema)
                && StringUtils.equals(m.getElement(), element)
                && StringUtils.equals(m.getQualifier(), qualifier)
        ).findFirst();

        return firstValue.isPresent() ? firstValue.get().getValue() : null;
    }

    private boolean checkMetadata2(Item currentItem, ExternalDataObject externalDataObject,
            String schema, String element, String qualifier, String language) {

        return StringUtils.equals(
                itemService.getMetadataFirstValue(currentItem, schema, element, qualifier, language),
                getFirstMetadataValue(externalDataObject, schema, element, qualifier, language));

    }

    private boolean checkMetadataAuthority2(Item currentItem, ExternalDataObject externalDataObject,
            String schema, String element, String qualifier, String language) {

        List<MetadataValue> itemMetadata = itemService.getMetadata(currentItem,  schema, element, qualifier, language);
        if (Objects.isNull(itemMetadata) || itemMetadata.isEmpty()) {
            return false;
        }

        String authorityFromReniec = getFirstMetadataValue(externalDataObject, schema, element, qualifier, language);

        return StringUtils.equals(itemMetadata.get(0).getAuthority(), authorityFromReniec);
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

    private void addMetadata(Context context, Item currentItem, ExternalDataObject externalDataObject) {
        try {
            addMetadata(context, currentItem, externalDataObject, "perucris", "apellidoPaterno", null, null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "apellidoMaterno", null, null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "apellidoCasada", null, null);
            addMetadata(context, currentItem, externalDataObject, "person", "givenName", null, null);
            addMappedUbigeoValue(context, currentItem, externalDataObject, "perucris", "domicilio", "ubigeoReniec");
            addMetadata(context, currentItem, externalDataObject, "perucris", "domicilio", "region", null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "domicilio", "provincia", null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "domicilio", "distrito", null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "domicilio", "direccion", null);
            addMappedUbigeoValue(context, currentItem, externalDataObject, "perucris", "nacimiento", "ubigeoReniec");
            addMetadata(context, currentItem, externalDataObject, "perucris", "nacimiento", "region", null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "nacimiento", "provincia", null);
            addMetadata(context, currentItem, externalDataObject, "perucris", "nacimiento", "distrito", null);
            addMetadata(context, currentItem, externalDataObject, "oairecerif", "person", "gender", null);
            addMetadata(context, currentItem, externalDataObject, "person", "birthDate", null, null);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addMetadata(Context context, Item currentItem, ExternalDataObject externalDataObject,
            String schema, String element, String qualifier, String language) throws SQLException {
        final String value = getFirstMetadataValue(externalDataObject, schema, element, qualifier, language);
        if (value != null) {
            itemService.addMetadata(context, currentItem, schema, element, qualifier, language, value);
        }
    }

    private void addMappedUbigeoValue(Context context, Item item, ExternalDataObject externalDataObject,
            String schema, String element, String qualifier) throws SQLException, AuthorizeException {

        String ubigeoValue = getFirstMetadataValue(externalDataObject, schema, element, qualifier, null);

        MetadataValueVO metadataValueVO = ubigeoMapping.convert("reniec", ubigeoValue);
        MetadataValue metadata =
            itemService.addMetadata(context, item, schema, element, qualifier, null, metadataValueVO.getValue());

        metadata.setAuthority(metadataValueVO.getAuthority());
        metadata.setConfidence(metadataValueVO.getConfidence());

        itemService.update(context, item);
    }
}
