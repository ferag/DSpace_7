/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.reniec;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.perucris.externalservices.reniec.ReniecDTO;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of {@link MetadataFieldMapping} used to import metadata from the Reniec Database.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ReniecMetadataFieldMapping implements MetadataFieldMapping<ReniecDTO, MetadataContributor<ReniecDTO>> {

    private final MetadataFieldService metadataFieldService;
    private final EPersonService ePersonService;
    private final RequestService requestService;

    @Autowired
    public ReniecMetadataFieldMapping(
            ItemService itemService,
            RequestService requestService,
            MetadataFieldService metadataFieldService,
            EPersonService ePersonService,
            CrisLayoutBoxService crisLayoutBoxService) {
        this.requestService = requestService;
        this.metadataFieldService = metadataFieldService;
        this.ePersonService = ePersonService;
    }

    @Override
    public MetadatumDTO toDCValue(MetadataFieldConfig field, String value) {
        throw new UnsupportedOperationException("Single field mapping not implemented");
    }

    @Override
    public Collection<MetadatumDTO> resultToDCValueMapping(ReniecDTO record) {

        try {

            Context context = (Context) requestService.getCurrentRequest().getAttribute("context");
            final MetadataField field = metadataFieldService.findByString(context, "perucris.eperson.dni", '.');
            EPerson eperson = ePersonService.findByEid(context, field, record.getIdentifierDni());
            // if it exists it means the user authenticated through reniec and oidc metadata have been saved.
            if (eperson == null) {
                throw new RuntimeException("EPerson must be verified through reniec oidc");
            }

            List<MetadatumDTO> metadataList = new LinkedList<>();

            metadataList.add(toDto("perucris", "eperson", "dni", record.getIdentifierDni()));
            metadataList.add(toDto("dc", "title", null, record.getNames() + record.getFatherLastName()));
            metadataList.add(toDto("perucris", "eperson", "birthdate", record.getBirthDate().toString()));

            // TODO: email? other info? geo-spatial info?
            return metadataList;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MetadatumDTO toDto(String schema, String element, String qualifier, String value) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();

        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);

        return metadatumDTO;
    }
}
