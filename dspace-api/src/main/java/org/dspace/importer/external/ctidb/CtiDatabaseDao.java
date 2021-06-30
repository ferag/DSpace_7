/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb;

import java.util.List;

import org.dspace.importer.external.ctidb.model.CtiConocimientoIdioma;
import org.dspace.importer.external.ctidb.model.CtiDatosConfidenciales;
import org.dspace.importer.external.ctidb.model.CtiDatosLaborales;
import org.dspace.importer.external.ctidb.model.CtiDerechosPi;
import org.dspace.importer.external.ctidb.model.CtiDistincionesPremios;
import org.dspace.importer.external.ctidb.model.CtiFormacionAcademica;
import org.dspace.importer.external.ctidb.model.CtiInvestigador;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;
import org.dspace.importer.external.ctidb.model.CtiProyecto;

/**
 * Provides methods to retrieve information from the CtiDatabase.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface CtiDatabaseDao {

    /**
     * Get the cti investigador id by dni.
     * @param dni the dni
     * @return the investigador id if the dni exists, null otherwise
     */
    public Integer getInvestigadorIdFromDni(String dni);

    /**
     * Get the cti investigador id by orcid.
     * @param orcid the orcid
     * @return the investigador id if the orcid exists, null otherwise
     */
    public Integer getInvestigadorIdFromOrcid(String orcid);

    /**
     * Get the basic information of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public CtiInvestigador getInvestigadorBaseInfo(Integer investigadorId);

    /**
     * Get the known languages of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiConocimientoIdioma> getConocimientoIdioma(Integer investigadorId);

    /**
     * Get confidential data of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiDatosConfidenciales> getDatosConfidenciales(Integer investigadorId);

    /**
     * Get formacion academica data of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiFormacionAcademica> getFormacionAcademica(Integer investigadorId);

    /**
     * Get datos laborales data of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiDatosLaborales> getDatosLaborales(Integer investigadorId);

    /**
     * Get all producciones bibliograficas of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiProduccionBibliografica> getAllProduccionesBibliograficas(Integer investigadorId);

    /**
     * Get all proyectos of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiProyecto> getAllProyectos(Integer investigadorId);

    /**
     * Get all propriedad intelectual of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiDerechosPi> getAllPropriedadIntelectual(Integer investigadorId);

    /**
     * Get premios of the investigador.
     * @param investigadorId cti investigador id
     * @return
     */
    public List<CtiDistincionesPremios> getDistincionesPremios(Integer investigadorId);

    /**
     * Get the produccion bibliografica with the given ctiId.
     * @param ctiProduccionBibliografica cti id
     * @return
     */
    public CtiProduccionBibliografica getProduccionBibliografica(Integer ctiProduccionBibliograficaId);

    /**
     * Get the proyecto with the given ctiId.
     * @param ctiProyectoId cti id
     * @return
     */
    public CtiProyecto getProyecto(Integer ctiProyectoId);

    /**
     * Get the derechosPi with the given ctiId.
     * @param ctiDerechoPi cti id
     * @return
     */
    public CtiDerechosPi getDerechosPi(Integer ctiDerechoPi);

}
