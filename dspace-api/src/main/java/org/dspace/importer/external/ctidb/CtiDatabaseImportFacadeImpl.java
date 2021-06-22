/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource.SourceId;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.ctidb.mapper.CtiPatentMapper;
import org.dspace.importer.external.ctidb.mapper.CtiPatentSuggestionMapper;
import org.dspace.importer.external.ctidb.mapper.CtiProfileMapper;
import org.dspace.importer.external.ctidb.mapper.CtiProjectMapper;
import org.dspace.importer.external.ctidb.mapper.CtiProjectSuggestionMapper;
import org.dspace.importer.external.ctidb.mapper.CtiPublicationMapper;
import org.dspace.importer.external.ctidb.mapper.CtiPublicationSuggestionMapper;
import org.dspace.importer.external.ctidb.model.BaseCtiEntity;
import org.dspace.importer.external.ctidb.model.CtiConocimientoIdioma;
import org.dspace.importer.external.ctidb.model.CtiDatosConfidenciales;
import org.dspace.importer.external.ctidb.model.CtiDatosLaborales;
import org.dspace.importer.external.ctidb.model.CtiDerechosPi;
import org.dspace.importer.external.ctidb.model.CtiFormacionAcademica;
import org.dspace.importer.external.ctidb.model.CtiInvestigador;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;
import org.dspace.importer.external.ctidb.model.CtiProyecto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDatabaseImportFacadeImpl implements CtiDatabaseImportFacade {

    private static final Logger log = LoggerFactory.getLogger(CtiDatabaseImportFacadeImpl.class);

    private CtiDatabaseDao ctiDatabaseDao;

    private CtiProfileMapper ctiProfileMapper;

    private CtiPublicationSuggestionMapper ctiPublicationSuggestionMapper;

    private CtiPatentSuggestionMapper ctiPatentSuggestionMapper;

    private CtiProjectSuggestionMapper ctiProjectSuggestionMapper;

    private CtiPublicationMapper ctiPublicationMapper;

    private CtiProjectMapper ctiProjectMapper;

    private CtiPatentMapper ctiPatentMapper;

    @Autowired
    public CtiDatabaseImportFacadeImpl(CtiDatabaseDao ctiDatabaseDao,
            CtiProfileMapper ctiProfileMapper,
            CtiPublicationSuggestionMapper ctiPublicationSuggestionMapper,
            CtiProjectSuggestionMapper ctiProjectSuggestionMapper,
            CtiPatentSuggestionMapper ctiPatentSuggestionMapper,
            CtiPublicationMapper ctiPublicationMapper,
            CtiProjectMapper ctiProjectMapper,
            CtiPatentMapper ctiPatentMapper) {
        this.ctiDatabaseDao = ctiDatabaseDao;
        this.ctiProfileMapper = ctiProfileMapper;
        this.ctiPublicationSuggestionMapper = ctiPublicationSuggestionMapper;
        this.ctiProjectSuggestionMapper = ctiProjectSuggestionMapper;
        this.ctiPatentSuggestionMapper = ctiPatentSuggestionMapper;
        this.ctiPublicationMapper = ctiPublicationMapper;
        this.ctiProjectMapper = ctiProjectMapper;
        this.ctiPatentMapper = ctiPatentMapper;
    }

    /**
     * Setter implemented for unit test purpose.
     * @param ctiDatabaseDao
     */
    public void setCtiDatabaseDao(CtiDatabaseDao ctiDatabaseDao) {
        this.ctiDatabaseDao = ctiDatabaseDao;
    }

    @Override
    public ExternalDataObject getCtiProfile(ResearcherProfileSource source) {

        Optional<Integer> investigadorId = findInvestigadorId(source);
        if (investigadorId.isEmpty()) {
            return null;
        }

        CtiInvestigador investigadorBaseInfo = ctiDatabaseDao.getInvestigadorBaseInfo(investigadorId.get());
        List<CtiDatosConfidenciales> datosConfidenciales = ctiDatabaseDao.getDatosConfidenciales(investigadorId.get());
        List<CtiDatosLaborales> datosLaborales = ctiDatabaseDao.getDatosLaborales(investigadorId.get());
        List<CtiFormacionAcademica> formacionAcademica = ctiDatabaseDao.getFormacionAcademica(investigadorId.get());
        List<CtiConocimientoIdioma> conocimientoIdioma = ctiDatabaseDao.getConocimientoIdioma(investigadorId.get());

        List<MetadataValueDTO> metadata = ctiProfileMapper.mapCtiProfile(investigadorBaseInfo,
                datosConfidenciales, datosLaborales, formacionAcademica, conocimientoIdioma);

        ExternalDataObject object = new ExternalDataObject();

        object.setMetadata(metadata);

        return object;
    }

    @Override
    public ExternalDataObject getCtiPublication(String ctiId) {

        CtiProduccionBibliografica produccionBibliografica = ctiDatabaseDao
                .getProduccionBibliografica(Integer.parseInt(ctiId));

        List<MetadataValueDTO> metadata = ctiPublicationMapper.mapCtiPublication(produccionBibliografica);

        ExternalDataObject object = new ExternalDataObject();

        object.setMetadata(metadata);

        return object;
    }

    @Override
    public ExternalDataObject getCtiProject(String ctiId) {

        CtiProyecto proyecto = ctiDatabaseDao.getProyecto(Integer.parseInt("10701"));

        List<MetadataValueDTO> metadata = ctiProjectMapper.mapCtiProject(proyecto);

        ExternalDataObject object = new ExternalDataObject();

        object.setMetadata(metadata);

        return object;

    }

    @Override
    public ExternalDataObject getCtiPatent(String ctiId) {

        CtiDerechosPi derechosPi = ctiDatabaseDao.getDerechosPi(Integer.parseInt(ctiId));

        List<MetadataValueDTO> metadata = ctiPatentMapper.mapCtiPatent(derechosPi);

        ExternalDataObject object = new ExternalDataObject();

        object.setMetadata(metadata);

        return object;
    }



    @Override
    public List<ExternalDataObject> getCtiSuggestions(ResearcherProfileSource source) {

        Optional<Integer> investigadorId = findInvestigadorId(source);
        if (investigadorId.isEmpty()) {
            return null;
        }

        List<ExternalDataObject> suggestions = new ArrayList<ExternalDataObject>();

        suggestions.addAll(ctiDatabaseDao.getAllProduccionesBibliograficas(investigadorId.get()).stream()
                .map(pb -> this.createCtiPublicationSuggestion(pb))
                .filter(pb -> pb.isPresent())
                .map(pb -> pb.get())
                .collect(Collectors.toList()));

        suggestions.addAll(ctiDatabaseDao.getAllProyectos(investigadorId.get()).stream()
                .map(pr -> this.createCtiProjectSuggestion(pr))
                .filter(pb -> pb.isPresent())
                .map(pb -> pb.get())
                .collect(Collectors.toList()));

        suggestions.addAll(ctiDatabaseDao.getAllPropriedadIntelectual(investigadorId.get()).stream()
                .map(pi -> this.createCtiPatentSuggestion(pi))
                .filter(pb -> pb.isPresent())
                .map(pb -> pb.get())
                .collect(Collectors.toList()));

        return suggestions;

    }

    private ExternalDataObject createSuggestion(String source, BaseCtiEntity entity, List<MetadataValueDTO> metadata) {
        ExternalDataObject object = new ExternalDataObject();
        object.setId(entity.getCtiId().toString());
        object.setMetadata(metadata);
        object.setSource("ctiPublication");

        return object;
    }

    private Optional<ExternalDataObject> createCtiPublicationSuggestion(CtiProduccionBibliografica entity) {
        if (Strings.isBlank(entity.getTitulo())) {
            log.warn("Can't create suggestion from ctiProduccionBibliografica with id=" + entity.getCtiId());
            return Optional.empty();
        }

        List<MetadataValueDTO> metadata = ctiPublicationSuggestionMapper.mapCtiPublicationSuggestion(entity);

        return Optional.of(createSuggestion("ctiPublication", entity, metadata));
    }

    private Optional<ExternalDataObject> createCtiProjectSuggestion(CtiProyecto entity) {
        if (Strings.isBlank(entity.getTitulo())) {
            log.warn("Can't create suggestion from ctiProject with id=" + entity.getCtiId());
            return Optional.empty();
        }

        List<MetadataValueDTO> metadata = ctiProjectSuggestionMapper.mapCtiProjectSuggestion(entity);

        return Optional.of(createSuggestion("ctiProject", entity, metadata));
    }

    private Optional<ExternalDataObject> createCtiPatentSuggestion(CtiDerechosPi entity) {
        if (Strings.isBlank(entity.getTituloPi())) {
            log.warn("Can't create suggestion from ctiDerechos with id=" + entity.getCtiId());
            return Optional.empty();
        }

        List<MetadataValueDTO> metadata = ctiPatentSuggestionMapper.mapCtiPatentSuggestion(entity);

        return Optional.of(createSuggestion("ctiProject", entity, metadata));
    }

    private Optional<Integer> findInvestigadorId(ResearcherProfileSource source) {

        for (SourceId sourceId : source.getSources()) {

            Integer investigadorId = getInvestigadorIdSupplier(sourceId).get();

            if (investigadorId != null) {
                return Optional.of(investigadorId);
            }
        }

        return Optional.empty();
    }

    private Supplier<Integer> getInvestigadorIdSupplier(SourceId sourceId) {
        return new Supplier<Integer>() {
            @Override
            public Integer get() {
                if ("dni".equals(sourceId.getSource())) {
                    return ctiDatabaseDao.getInvestigadorIdFromDni(sourceId.getId());
                }
                if ("orcid".equals(sourceId.getSource())) {
                    return ctiDatabaseDao.getInvestigadorIdFromOrcid(sourceId.getId());
                }
                return null;
            }
        };
    }


}
