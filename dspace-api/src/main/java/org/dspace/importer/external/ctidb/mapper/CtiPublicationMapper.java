/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.Strings;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.importer.external.ctidb.model.CtiPerson;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;

/**
 * Map Cti Entities to MetadataValueDTOs suitable for CvPublication.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CtiPublicationMapper extends AbstractCtiMapper {

    public List<MetadataValueDTO> mapCtiPublication(CtiProduccionBibliografica entity) {

        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();

        metadata.add(getCtiIdentifier(entity));

        metadata.add(new MetadataValueDTO("dc", "title", null, null, entity.getTitulo().trim()));

        if (entity.getFechaProduccion() != null) {
            metadata.add(new MetadataValueDTO("dc", "date", "issued", null, mapDate(entity.getFechaProduccion())));
        }

        // authors
        // this record is the source author, could be related to the ctiProfile.
        metadata.add(new MetadataValueDTO("dc", "contributor", "author", null, mapContributorAutor(
                entity.getInvestigador().getApellidoPaterno(),
                entity.getInvestigador().getApellidoMaterno(),
                entity.getInvestigador().getNombres())));

        for (CtiPerson autor : entity.getAutors()) {
            metadata.add(new MetadataValueDTO("dc", "contributor", "author", null, mapContributorAutor(
                    autor.getApellidoPaterno(),
                    autor.getApellidoMaterno(),
                    autor.getNombres())));
        }

        return metadata;

    }

    protected String mapContributorAutor(String apellidoPaterno, String apellidoMaterno, String nombres) {
        return Stream.of(apellidoPaterno, apellidoMaterno, nombres)
                .filter(s -> !Strings.isBlank(s)).map(s -> s.trim()).collect(Collectors.joining(" "));
    }

}
