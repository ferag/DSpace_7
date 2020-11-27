/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.dspace;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.RequestService;


/**
 * Implementation of {@link MetadataFieldMapping} used to import metadata from a DSpace Item to another.
 * Only public metadata are extracted.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceInternalMetadataFieldMapping implements MetadataFieldMapping<Item, MetadataContributor<Item>> {

    private final ItemService itemService;
    private final RequestService requestService;
    private final CrisLayoutBoxService crisLayoutBoxService;

    public DSpaceInternalMetadataFieldMapping(ItemService itemService, RequestService requestService,
                                              CrisLayoutBoxService crisLayoutBoxService) {
        this.itemService = itemService;
        this.requestService = requestService;
        this.crisLayoutBoxService = crisLayoutBoxService;
    }

    @Override
    public MetadatumDTO toDCValue(MetadataFieldConfig field, String value) {
        throw new UnsupportedOperationException("Single field mapping not implemented");
    }

    @Override
    public Collection<MetadatumDTO> resultToDCValueMapping(Item record) {

        try {
            Context context = (Context) requestService.getCurrentRequest().getAttribute("context");
            List<MetadataField> publicMetadataFields = publicMetadataFields(context, record);

            List<MetadataValue> fullList = itemService.getMetadata(record, Item.ANY, Item.ANY,
                    Item.ANY, Item.ANY, false);

            return fullList.stream()
                    .filter(mv -> publicMetadataFields.contains(mv.getMetadataField()))
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MetadatumDTO toDto(MetadataValue metadataValue) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();

        metadatumDTO.setSchema(metadataValue.getSchema());
        metadatumDTO.setElement(metadataValue.getElement());
        metadatumDTO.setQualifier(metadataValue.getQualifier());
        metadatumDTO.setValue(metadataValue.getValue());

        return metadatumDTO;
    }

    private List<MetadataField> publicMetadataFields(Context context, Item record) throws SQLException {
        String entityType = itemService.getMetadataFirstValue(record, MetadataSchemaEnum.RELATIONSHIP.getName(),
                "type", null, Item.ANY);
        List<CrisLayoutBox> boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);

        return boxes.stream()
                .filter(b -> LayoutSecurity.valueOf(b.getSecurity()).equals(LayoutSecurity.PUBLIC))
                .flatMap(b -> b.getLayoutFields().stream())
                .map(CrisLayoutField::getMetadataField)
                .collect(Collectors.toList());
    }
}
