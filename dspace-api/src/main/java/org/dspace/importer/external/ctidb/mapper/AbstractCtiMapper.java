/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.importer.external.ctidb.model.BaseCtiEntity;
import org.dspace.vocabulary.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common implementation and utils to Map Cti Entities to MetadataValueDTOs.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public abstract class AbstractCtiMapper {

    public static final String EMPTY = "#PLACEHOLDER_PARENT_METADATA_VALUE#";

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    @Autowired
    private CtiMappingUtils ctiMappingUtils;

    protected String mapDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    protected String validateAndFormatUbigeo(String department, String province, String district) {
        ControlledVocabulary ubigeoVocabulary = this.getCtiMappingUtils().getUbigeoVocabulary();
        if (ubigeoVocabulary == null) {
            return null;
        }
        Optional<ControlledVocabulary> resolvedDepartment = ubigeoVocabulary.getChildNodes()
                .stream().filter(voc -> voc.getLabel().toUpperCase().equals(department.toUpperCase())).findFirst();
        if (resolvedDepartment.isEmpty()) {
            return null;
        }
        Optional<ControlledVocabulary> resolvedProvince = resolvedDepartment.get().getChildNodes()
                .stream().filter(voc -> voc.getLabel().toUpperCase().equals(province.toUpperCase())).findFirst();
        if (resolvedProvince.isEmpty()) {
            return null;
        }
        Optional<ControlledVocabulary> resolvedDistrict = resolvedProvince.get().getChildNodes()
                .stream().filter(voc -> voc.getLabel().toUpperCase().equals(district.toUpperCase())).findFirst();
        if (resolvedDistrict.isEmpty()) {
            return null;
        }
        return Stream.of(
                "Ubigeo",
                resolvedDepartment.get().getLabel(),
                resolvedProvince.get().getLabel(),
                resolvedDistrict.get().getLabel())
                .filter(s -> s != null)
                .map(s -> s.toString())
                .collect(Collectors.joining("::"));
    }

    protected MetadataValueDTO getCtiIdentifier(BaseCtiEntity entity) {
        return new MetadataValueDTO("perucris", "identifier", "cti", null, entity.getCtiId().toString());
    }

    public CtiMappingUtils getCtiMappingUtils() {
        return ctiMappingUtils;
    }

    public void setCtiMappingUtils(CtiMappingUtils ctiMappingUtils) {
        this.ctiMappingUtils = ctiMappingUtils;
    }

}
