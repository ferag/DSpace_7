/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.importer.external.ctidb.model.CtiConocimientoIdioma;
import org.dspace.importer.external.ctidb.model.CtiDatosConfidenciales;
import org.dspace.importer.external.ctidb.model.CtiDatosLaborales;
import org.dspace.importer.external.ctidb.model.CtiDerechosPi;
import org.dspace.importer.external.ctidb.model.CtiDistincionesPremios;
import org.dspace.importer.external.ctidb.model.CtiFormacionAcademica;
import org.dspace.importer.external.ctidb.model.CtiInvestigador;
import org.dspace.importer.external.ctidb.model.CtiPerson;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;
import org.dspace.importer.external.ctidb.model.CtiProyecto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiDatabaseDaoImpl implements CtiDatabaseDao {

    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CtiDatabaseDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Integer getInvestigadorIdFromDni(String dni) {

        Integer dni_tipo_documento_id = 1;

        String sql = "SELECT investigador.id_investigador as investigadorId "
                + "FROM directorio_cti.tbl_investigador investigador " +
                     " LEFT OUTER JOIN directorio_cti.tbl_datos_confidenciales datos_confidentiales " +
                     " ON investigador.id_investigador = datos_confidentiales.id_investigador " +
                     " WHERE datos_confidentiales.id_tipo_documento = ? AND datos_confidentiales.nro_documento = ? ";

        Integer investigadorId = jdbcTemplate
                .queryForObject(sql, new Object[]{dni_tipo_documento_id, dni}, Integer.class);

        return investigadorId;

    }

    @Override
    public Integer getInvestigadorIdFromOrcid(String orcid) {

        String sql = " SELECT id_investigador as investigadorId "
                   + " FROM directorio_cti.tbl_investigador "
                   + " WHERE id_orcid = ? ";

        Integer investigadorId = jdbcTemplate.queryForObject(sql, new Object[]{orcid}, Integer.class);

        return investigadorId;
    }

    // ===================================
    // @ Investigador
    // ===================================

    @Override
    public CtiInvestigador getInvestigadorBaseInfo(Integer investigadorId) {

        String sql =  "SELECT  \n" +
                "                        investigador.id_investigador as investigadorId,  \n" +
                "                        investigador.nombres as nombres,  \n" +
                "                        investigador.apellido_paterno as apellidoPaterno,  \n" +
                "                        investigador.apellido_materno as apellidoMaterno,  \n" +
                "                        investigador.sexo as sexo,  \n" +
                "                        investigador.email as email,  \n" +
                "                        investigador.direccion_web as direccionWeb,  \n" +
                "                        investigador.desc_personal as descPersonal,  \n" +
                "                        investigador.id_orcid as idOrcid,  \n" +
                "                        investigador.id_perfil_scopus as idPerfilScopus,  \n" +
                "                        investigador.id_departamento as departamentoId,  \n" +
                "                        investigador.id_provincia as provinciaId,  \n" +
                "                        investigador.id_distrito as distritoId,\n" +
                "                        investigador.id_institucion_laboral as institucionLaboralId,\n" +
                "                        departamento.nomdpt as departamentoDescr,\n" +
                "                        provincia.nomprov as provinciaDesc,\n" +
                "                        distrito.nomdist as distritoDesc,\n" +
                "                        paisNac.codpai as paisNacimientoId,\n" +
                "                        paisNac.nompai as paisNacimientoNombre,  \n" +
                "                        paisRes.codpai as paisResidenciaId,  \n" +
                "                        paisRes.nompai as paisResidenciaNombre\n" +
                "FROM directorio_cti.tbl_investigador investigador\n" +
                "    LEFT OUTER JOIN configuracion.tbl_pais paisNac\n" +
                "        ON investigador.id_pais_nacimiento = paisNac.codpai\n" +
                "    LEFT OUTER JOIN configuracion.tbl_pais paisRes\n" +
                "        ON investigador.id_pais_residencia = paisRes.codpai\n" +
                "    LEFT OUTER JOIN configuracion.tbl_departamento departamento\n" +
                "        ON investigador.id_departamento = departamento.coddpt\n" +
                "    LEFT OUTER JOIN configuracion.tbl_provincia provincia\n" +
                "        ON investigador.id_provincia = provincia.codprov\n" +
                "    LEFT OUTER JOIN configuracion.tbl_distrito distrito\n" +
                "        ON investigador.id_distrito = distrito.coddist\n" +
                "WHERE investigador.id_investigador = ?";

        CtiInvestigador objects = jdbcTemplate
                .queryForObject(sql, new Object[]{investigadorId}, new InvestigadorRowMapper());

        return objects;
    }

    private class InvestigadorRowMapper implements RowMapper<CtiInvestigador> {

        @Override
        public CtiInvestigador mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiInvestigador object = new CtiInvestigador();
            object.setCtiId(rs.getInt("investigadorId"));
            object.setNombres(rs.getString("nombres"));
            object.setApellidoPaterno(rs.getString("apellidoPaterno"));
            object.setApellidoMaterno(rs.getString("apellidoMaterno"));
            object.setSexo(rs.getInt("sexo"));
            object.setEmail(rs.getString("email"));
            object.setDireccionWeb(rs.getString("direccionWeb"));
            object.setDescPersonal(rs.getString("descPersonal"));
            object.setIdOrcid(rs.getString("idOrcid"));
            object.setIdPerfilScopus(rs.getString("idPerfilScopus"));
            object.setInstitucionLaboralId(rs.getInt("institucionLaboralId"));
            object.setDepartamentoId(rs.getInt("departamentoId"));
            object.setDepartamentoDescr(rs.getString("departamentoDescr"));
            object.setProvinciaId(rs.getInt("provinciaId"));
            object.setProvinciaDesc(rs.getString("provinciaDesc"));
            object.setDistritoId(rs.getInt("distritoId"));
            object.setDistritoDesc(rs.getString("distritoDesc"));
            object.setPaisNacimientoId(rs.getInt("paisNacimientoId"));
            object.setPaisNacimientoNombre(rs.getString("paisNacimientoNombre"));
            object.setPaisResidenciaId(rs.getInt("paisResidenciaId"));
            object.setPaisResidenciaNombre(rs.getString("paisResidenciaNombre"));

            return object;

        }
    }

    // ===================================
    // @ Conocimiento Idioma
    // ===================================

    @Override
    public List<CtiConocimientoIdioma> getConocimientoIdioma(Integer investigadorId) {
        String sql = "SELECT\n" +
                "       conocimiento_idioma.id_conocimiento_idioma as conocimientoIdiomaId,\n" +
                "       conocimiento_idioma.nivel_lectura as nivelLectura,\n" +
                "       conocimiento_idioma.nivel_escritura as nivelEscritura,\n" +
                "       conocimiento_idioma.nivel_conversacion as nivelConversacion,\n" +
                "       conocimiento_idioma.flag_lengua_materna as lenguaMaterna,\n" +
                "       idioma.id_idioma as idiomaId,\n" +
                "       idioma.num_idioma as idiomaNum,\n" +
                "       idioma.descripcion as idiomaDescripcion\n" +
                "FROM directorio_cti.tbl_investigador investigador\n" +
                "             LEFT OUTER JOIN directorio_cti.tbl_conocimiento_idioma conocimiento_idioma\n" +
                "                 ON investigador.id_investigador = conocimiento_idioma.id_investigador\n" +
                "             LEFT OUTER JOIN configuracion.tbl_idioma idioma\n" +
                "                 ON conocimiento_idioma.id_idioma = idioma.id_idioma\n" +
                "WHERE investigador.id_investigador = ?";

        List<CtiConocimientoIdioma> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new ConocimientoIdiomaRowMapper());

        return objects;
    }

    private class ConocimientoIdiomaRowMapper implements RowMapper<CtiConocimientoIdioma> {

        @Override
        public CtiConocimientoIdioma mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiConocimientoIdioma object = new CtiConocimientoIdioma();

            object.setCtiId(rs.getInt("conocimientoIdiomaId"));
            object.setNivelLectura(rs.getInt("nivelLectura"));
            object.setNivelEscritura(rs.getInt("nivelEscritura"));
            object.setNivelConversacion(rs.getInt("nivelConversacion"));
            object.setLenguaMaterna(rs.getInt("lenguaMaterna"));
            object.setIdiomaDescription(rs.getString("idiomaDescripcion"));
            object.setIdiomaId(rs.getInt("idiomaId"));
            object.setIdiomaNum(rs.getString("idiomaNum"));

            return object;

        }
    }


    // ===================================
    // @ Datos Confidentiales
    // ===================================

    @Override
    public List<CtiDatosConfidenciales> getDatosConfidenciales(Integer investigadorId) {
        String sql = "SELECT\n" +
                "       datos_confidenciales.id_datos_confidenciales as datosConfidencialesId, " +
                "       datos_confidenciales.telefono_fijo as telefonoFijo, " +
                "       datos_confidenciales.telefono_celular as telefonoCelular, " +
                "       datos_confidenciales.id_tipo_documento as tipoDocumentoId, " +
                "       datos_confidenciales.nro_documento as nroDocumento, " +
                "       datos_confidenciales.fecha_nacimiento as fechaNacimiento " +
                "FROM directorio_cti.tbl_investigador investigador " +
                "    LEFT OUTER JOIN directorio_cti.tbl_datos_confidenciales datos_confidenciales " +
                "        ON investigador.id_investigador = datos_confidenciales.id_investigador " +
                "WHERE investigador.id_investigador = ?";

        List<CtiDatosConfidenciales> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new DatosConfiencialesRowMapper());

        return objects;

    }

    private class DatosConfiencialesRowMapper implements RowMapper<CtiDatosConfidenciales> {

        @Override
        public CtiDatosConfidenciales mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiDatosConfidenciales object = new CtiDatosConfidenciales();

            object.setCtiId(rs.getInt("datosConfidencialesId"));
            object.setTelefonoFijo(rs.getString("telefonoFijo"));
            object.setTelefonoCelular(rs.getString("telefonoCelular"));
            object.setTipoDocumentoId(rs.getInt("tipoDocumentoId"));
            object.setNroDocumento(rs.getString("nroDocumento"));
            object.setFechaNacimiento(rs.getDate("fechaNacimiento"));

            return object;

        }
    }

    // ===================================
    // @ Formacion Academica
    // ===================================


    @Override
    public List<CtiFormacionAcademica> getFormacionAcademica(Integer investigadorId) {
        String sql = "SELECT\n" +
                "       formacion_academica.id_formacion_academica as formacionAcademicaId, " +
                "       formacion_academica.titulo as titulo, " +
                "       formacion_academica.fecha_inicio as fechaInicio, " +
                "       formacion_academica.fecha_fin as fechaFin, " +
                "       formacion_academica.nombre_centro_estudios as centroEstudiosNombre, " +
                "       formacion_academica.id_centro_estudios as centroEstudiosId, " +
                "       grado_academico.id_grado_academico as gradoAcademicoId, " +
                "       grado_academico.descripcion as gradoAcademicoDescripcion, " +
                "       pais.nompai as centroEstudioPaisNombre, " +
                "       pais.codpai as centroEstudioPaisId " +
                "FROM directorio_cti.tbl_investigador investigador " +
                "    LEFT OUTER JOIN directorio_cti.tbl_formacion_academica formacion_academica " +
                "        ON investigador.id_investigador = formacion_academica.id_investigador " +
                "    LEFT OUTER JOIN configuracion.tbl_grado_academico grado_academico " +
                "        ON formacion_academica.id_grado_academico = grado_academico.id_grado_academico " +
                "    LEFT OUTER JOIN instituciones_cti.tbl_institucion institucion " +
                "        ON formacion_academica.id_centro_estudios = institucion.id_institucion " +
                "    LEFT OUTER JOIN configuracion.tbl_pais pais " +
                "        ON  formacion_academica.id_pais::varchar = pais.codpai " +
                "WHERE investigador.id_investigador = ?";

        List<CtiFormacionAcademica> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new FormacionAcademicaRowMapper());

        return objects;
    }

    private class FormacionAcademicaRowMapper implements RowMapper<CtiFormacionAcademica> {

        @Override
        public CtiFormacionAcademica mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiFormacionAcademica object = new CtiFormacionAcademica();
            object.setCtiId(rs.getInt("formacionAcademicaId"));
            object.setTitulo(rs.getString("titulo"));
            object.setFechaInicio(rs.getDate("fechaInicio"));
            object.setFechaFin(rs.getDate("fechaFin"));
            object.setCentroEstudiosNombre(rs.getString("centroEstudiosNombre"));
            object.setCentroEstudiosId(rs.getInt("centroEstudiosId"));
            object.setCentroEstudiosPaisNombre(rs.getString("centroEstudioPaisNombre"));
            object.setCentroEstudiosPaisId(rs.getInt("centroEstudioPaisId"));
            object.setGradoAcademicoId(rs.getInt("gradoAcademicoId"));
            object.setGradoAcademicoDescripcion(rs.getString("gradoAcademicoDescripcion"));
            return object;

        }
    }

    // ===================================
    // @ Datos Laborales
    // ===================================

    @Override
    public List<CtiDatosLaborales> getDatosLaborales(Integer investigadorId) {
        String sql = "SELECT\n" +
                "       datos_laborales.id_datos_laborales as datosLaboralesId,\n" +
                "       datos_laborales.cargo as cargo,\n" +
                "       datos_laborales.fecha_inicio as fechaInicio,\n" +
                "       datos_laborales.fecha_fin as fechaFin,\n" +
                "       institucion.id_institucion as institucionId,\n" +
                "       institucion.razon_social as institucionRazonSocial\n" +
                "FROM directorio_cti.tbl_investigador investigador\n" +
                "    LEFT OUTER JOIN directorio_cti.tbl_datos_laborales datos_laborales\n" +
                "        ON investigador.id_investigador = datos_laborales.id_investigador\n" +
                "        -- related entities --\n" +
                "    LEFT OUTER JOIN instituciones_cti.tbl_institucion institucion\n" +
                "        ON datos_laborales.id_institucion_laboral = institucion.id_institucion\n" +
                "WHERE investigador.id_investigador = ?";

        List<CtiDatosLaborales> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new DatosLaboralesRowMapper());

        return objects;
    }

    private class DatosLaboralesRowMapper implements RowMapper<CtiDatosLaborales> {

        @Override
        public CtiDatosLaborales mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiDatosLaborales object = new CtiDatosLaborales();

            object.setCtiId(rs.getInt("datosLaboralesId"));
            object.setCargo(rs.getString("cargo"));
            object.setFechaInicio(rs.getDate("fechaInicio"));
            object.setFechaFin(rs.getDate("fechaFin"));
            object.setInstitucionId(rs.getInt("institucionId"));
            object.setInstitucionRazonSocial(rs.getString("institucionRazonSocial"));

            return object;
        }
    }

    // ===========================================
    // @ Investigador: Producciones Bibliograficas
    // ===========================================

    public List<CtiProduccionBibliografica> getAllProduccionesBibliograficas(Integer investigadorId) {
        String sql = "select distinct produccion.id_produccion_bibliografica as produccionBibliograficaId,\n" +
                "                produccion.titulo as titulo,\n" +
                "                produccion.palabras_clave as palabrasClave,\n" +
                "                produccion.fecha_produccion as fechaProduccion,\n" +
                "                produccion.autor as autor,\n" +
                "                produccion.coautor  as coautor,\n" +
                "                produccion.colaboradores as colaboradores,\n" +
                "                produccion.fecha_produccion as fechaProduccion,\n" +
                "                tipo_produccion.id_tipo_produccion_bibliografica as tipoProduccionBibliograficaId,\n" +
                "                tipo_produccion.descripcion as tipoProduccionBibliograficadescripcion\n" +
                "                from directorio_cti.tbl_produccion_bibliografica produccion\n" +
                "                left outer join directorio_cti.tbl_cab_autores autores"
                + " on autores.id_produccion_bibliografica = produccion.id_produccion_bibliografica\n" +
                "                left outer join directorio_cti.tbl_cab_coautores coautores"
                + " on coautores.id_produccion_bibliografica = produccion.id_produccion_bibliografica\n" +
                "                left outer join directorio_config.tbl_tipo_produccion_bibliografica tipo_produccion"
                + " on tipo_produccion.id_tipo_produccion_bibliografica"
                + " = produccion.id_tipo_produccion_bibliografica\n" +
                "                where autores.id_investigador = ? or coautores.id_investigador = ?";

        List<CtiProduccionBibliografica> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId, investigadorId}, new BaseProduccionBibliograficaRowMapper());

        return objects;
    }

    private class BaseProduccionBibliograficaRowMapper implements RowMapper<CtiProduccionBibliografica> {

        @Override
        public CtiProduccionBibliografica mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiProduccionBibliografica object = new CtiProduccionBibliografica();

            object.setCtiId(rs.getInt("produccionBibliograficaId"));
            object.setTitulo(rs.getString("titulo"));
            object.setAutor(rs.getString("autor"));
            object.setCoautor(rs.getString("coautor"));
            object.setColaboradores(rs.getString("colaboradores"));
            object.setFechaProduccion(rs.getDate("fechaProduccion"));
            object.setTipoProduccionBibliograficaId(rs.getInt("tipoProduccionBibliograficaId"));
            object.setTipoProduccionBibliograficadescripcion(rs.getString("tipoProduccionBibliograficadescripcion"));

            return object;
        }
    }

    // ===================================
    // @ Investigador: Proyectos
    // ===================================

    public List<CtiProyecto> getAllProyectos(Integer investigadorId) {
        String sql = "select proyecto.id_proyecto as proyectoId,\n" +
                "       proyecto.titulo as titulo,\n" +
                "       proyecto.descripcion as descripcion,\n" +
                "       proyecto.palabras_clave as palabrasClave,\n" +
                "       proyecto.fecha_inicio as fechaInicio,\n" +
                "       proyecto.fecha_fin as fechaFin,\n" +
                "       proyecto.responsable as responsable,\n" +
                "       proyecto.colaboradores as colaboradores,\n" +
                "       proyecto.id_institucion_laboral as institucionLaboralId,\n" +
                "       proyecto.id_institucion_colaboradora as institucionColaboradoraId,\n" +
                "       proyecto.id_institucion_subvencion as institucionSubvencionId,\n" +
                "       proyecto.id_institucion_subvencionadora as institucionSubvencionadoraId,\n" +
                "       proyecto.nombre_institucion_laboral as nombreInstitucionLaboral,\n" +
                "       proyecto.nombre_institucion_colabo as nombreInstitucionColaboradora,\n" +
                "       proyecto.nombre_institucion_subvencionadora as nombreInstitucionSubvencionadora\n" +
                "       from directorio_cti.tbl_proyecto proyecto\n" +
                "       where proyecto.id_investigador = ?\n" +
                "";

        List<CtiProyecto> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new BaseProyectoRowMapper());

        return objects;
    }

    private class BaseProyectoRowMapper implements RowMapper<CtiProyecto> {

        @Override
        public CtiProyecto mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiProyecto object = new CtiProyecto();
            object.setCtiId(rs.getInt("proyectoId"));
            object.setTitulo(rs.getString("titulo"));
            object.setDescripcion(rs.getString("descripcion"));
            object.setPalabrasClave(rs.getString("palabrasClave"));
            object.setFechaInicio(rs.getDate("fechaInicio"));
            object.setFechaFin(rs.getDate("fechaFin"));
            object.setResponsable(rs.getString("responsable"));
            object.setColaboradores(rs.getString("colaboradores"));
            object.setInstitucionLaboralId(rs.getInt("institucionLaboralId"));
            object.setInstitucionColaboradoraId(rs.getInt("institucionColaboradoraId"));
            object.setInstitucionSubvencionId(rs.getInt("institucionSubvencionId"));
            object.setInstitucionSubvencionadoraId(rs.getInt("institucionSubvencionadoraId"));

            object.setNombreInstitucionLaboral(rs.getString("nombreInstitucionLaboral"));
            object.setNombreInstitucionColaboradora(rs.getString("nombreInstitucionColaboradora"));
            object.setNombreInstitucionSubvencionadora(rs.getString("nombreInstitucionSubvencionadora"));

            return object;
        }
    }

    // ===================================
    // @ Investigador:  Propriedad intelectual
    // ===================================

    public List<CtiDerechosPi> getAllPropriedadIntelectual(Integer investigadorId) {
        String sql = "select\n" +
                "       derechos.id_derechos_pi as derechosPiId,\n" +
                "       derechos.id_tipo_pi as tipoPiId,\n" +
                "       derechos.titulo_pi as tituloPi,\n" +
                "       derechos.nombre_propietario as nombreProprietario,\n" +
                "       derechos.numero_registro_pi as numeroRegistroPi,\n" +
                "       derechos.fecha_aceptacion as fechaAceptacion\n" +
                "     from directorio_cti.tbl_derechos_pi derechos\n" +
                "     where derechos.id_investigador = ?";

        List<CtiDerechosPi> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new BaseDerechosPiRowMapper());

        return objects;
    }

    private class BaseDerechosPiRowMapper implements RowMapper<CtiDerechosPi> {

        @Override
        public CtiDerechosPi mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiDerechosPi object = new CtiDerechosPi();
            object.setCtiId(rs.getInt("derechosPiId"));
            object.setTipoPiId(rs.getInt("tipoPiId"));
            object.setTituloPi(rs.getString("tituloPi"));
            object.setNombreProprietario(rs.getString("nombreProprietario"));
            object.setNumeroRegistroPi(rs.getInt("numeroRegistroPi"));
            object.setFechaAceptacion(rs.getDate("fechaAceptacion"));
            return object;
        }
    }

    // ===================================
    // @ Investigador: Distinciones Premios
    // ===================================

    public List<CtiDistincionesPremios> getDistincionesPremios(Integer investigadorId) {
        String sql = "select\n" +
                "       premios.id_distinciones_premios as distincionesPremiosId,\n" +
                "       premios.id_institucion as institucionId,\n" +
                "       premios.id_pais as paisId,\n" +
                "       premios.distincion as distincion,\n" +
                "       premios.descripcion as descripcion,\n" +
                "       premios.fecha_inicio as fechaInicio,\n" +
                "       premios.fecha_fin as fechaFin,\n" +
                "       premios.nombre_institucion as nombreInstitucion\n" +
                "     from directorio_cti.tbl_distinciones_premios premios\n" +
                "     where premios.id_investigador = ?";

        List<CtiDistincionesPremios> objects = jdbcTemplate
                .query(sql, new Object[]{investigadorId}, new DistincionesPremiosRowMapper());

        return objects;
    }

    private class DistincionesPremiosRowMapper implements RowMapper<CtiDistincionesPremios> {

        @Override
        public CtiDistincionesPremios mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiDistincionesPremios object = new CtiDistincionesPremios();
            object.setCtiId(rs.getInt("distincionesPremiosId"));
            object.setInstitucionId(rs.getInt("institucionId"));
            object.setPaisId(rs.getInt("paisId"));
            object.setDistincion(rs.getString("distincion"));
            object.setDescripcion(rs.getString("descripcion"));
            object.setFechaInicio(rs.getDate("fechaInicio"));
            object.setFechaFin(rs.getDate("fechaFin"));
            object.setNombreInstitucion(rs.getString("nombreInstitucion"));

            return object;
        }
    }

    // ===================================
    // @ Fetch Produccion Bibliografica
    // ===================================

    public CtiProduccionBibliografica getProduccionBibliografica(Integer produccionBibliograficaId) {
        String sql = "select distinct produccion.id_produccion_bibliografica as produccionBibliograficaId,\n" +
                "    produccion.titulo as titulo,\n" +
                "    produccion.palabras_clave as palabrasClave,\n" +
                "    produccion.fecha_produccion as fechaProduccion,\n" +
                "    produccion.autor as autor,\n" +
                "    produccion.coautor  as coautor,\n" +
                "    produccion.colaboradores as colaboradores,\n" +
                "    produccion.fecha_produccion as fechaProduccion,\n" +
                "    tipo_produccion.id_tipo_produccion_bibliografica as tipoProduccionBibliograficaId,\n" +
                "    tipo_produccion.descripcion as tipoProduccionBibliograficadescripcion,\n" +

                "    investigador.id_investigador as investigadorId,\n" +
                "    investigador.apellido_paterno as investigadorApellidoPaterno,\n" +
                "    investigador.apellido_materno as investigadorApellidoMaterno,\n" +
                "    investigador.nombres  as investigadorNombres,\n" +
                "    investigador.email as investigadorEmail,\n" +

                "    det_autores.id_det_autores as detAutorId,\n" +
                "    det_autores.apellido_materno as detAutorApellidoMaterno,\n" +
                "    det_autores.apellido_paterno as detAutorApellidoPaterno,\n" +
                "    det_autores.nombres as detAutorNombres,\n" +
                "    det_autores.dni as detAutorDni,\n" +
                "    det_autores.email as detAutorEmail,\n" +

                "    det_autores.id_cab_autores,\n" +
                "    cab_autores.id_investigador\n" +

                "    from directorio_cti.tbl_produccion_bibliografica produccion\n" +
                "    left outer join directorio_config.tbl_tipo_produccion_bibliografica tipo_produccion\n"
                + " on tipo_produccion.id_tipo_produccion_bibliografica "
                + " = produccion.id_tipo_produccion_bibliografica\n" +
                "    left outer join directorio_cti.tbl_det_autores det_autores\n"
                + " on det_autores.id_produccion_bibliografica = produccion.id_produccion_bibliografica\n" +
                "    left outer join directorio_cti.tbl_cab_autores cab_autores\n"
                + " on cab_autores.id_cab_autores = det_autores.id_cab_autores\n" +
                "    left outer join directorio_cti.tbl_investigador investigador\n"
                + " on cab_autores.id_investigador = investigador.id_investigador\n" +
                "where produccion.id_produccion_bibliografica = ?"
                ;

        Optional<CtiProduccionBibliografica> object = jdbcTemplate.query(sql, new Object[]{produccionBibliograficaId},
                new ProduccionBibliograficaRowMapper()).stream().findFirst();

        if (object.isEmpty()) {
            return null;
        }
        List<CtiPerson> autors = jdbcTemplate.query(sql, new Object[]{produccionBibliograficaId}, new AutorRowMapper());

        object.get().setAutors(autors);

        return object.get();
    }

    private class ProduccionBibliograficaRowMapper implements RowMapper<CtiProduccionBibliografica> {

        @Override
        public CtiProduccionBibliografica mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null || rowNum > 0) {
                return null;
            }
            CtiProduccionBibliografica object = new CtiProduccionBibliografica();

            object.setCtiId(rs.getInt("produccionBibliograficaId"));
            object.setTitulo(rs.getString("titulo"));
            object.setAutor(rs.getString("autor"));
            object.setCoautor(rs.getString("coautor"));
            object.setColaboradores(rs.getString("colaboradores"));
            object.setFechaProduccion(rs.getDate("fechaProduccion"));
            object.setTipoProduccionBibliograficaId(rs.getInt("tipoProduccionBibliograficaId"));
            object.setTipoProduccionBibliograficadescripcion(rs.getString("tipoProduccionBibliograficadescripcion"));

            CtiInvestigador investigador = new CtiInvestigador();
            investigador.setCtiId(rs.getInt("investigadorId"));
            investigador.setNombres(rs.getString("investigadorNombres"));
            investigador.setApellidoPaterno(rs.getString("investigadorApellidoPaterno"));
            investigador.setApellidoMaterno(rs.getString("investigadorApellidoMaterno"));
            investigador.setEmail(rs.getString("investigadorEmail"));

            object.setInvestigador(investigador);

            return object;
        }
    }

    private class AutorRowMapper implements RowMapper<CtiPerson> {

        @Override
        public CtiPerson mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiPerson object = new CtiPerson();

            object.setCtiId(rs.getInt("detAutorId"));
            object.setApellidoMaterno(rs.getString("detAutorApellidoMaterno"));
            object.setApellidoPaterno(rs.getString("detAutorApellidoPaterno"));
            object.setNombres(rs.getString("detAutorNombres"));
            object.setDni(rs.getString("detAutorDni"));
            object.setEmail(rs.getString("detAutorEmail"));

            return object;
        }
    }

    // ===================================
    // @ Fetch Proyecto
    // ===================================

    public CtiProyecto getProyecto(Integer proyectoId) {
        String sql = "select distinct proyecto.id_proyecto as proyectoId,\n" +
                "                proyecto.titulo as titulo,\n" +
                "                proyecto.descripcion as descripcion,\n" +
                "                proyecto.palabras_clave as palabrasClave,\n" +
                "                proyecto.fecha_inicio as fechaInicio,\n" +
                "                proyecto.fecha_fin as fechaFin,\n" +
                "                proyecto.responsable as responsable,\n" +
                "                proyecto.colaboradores as colaboradores,\n" +
                "                proyecto.id_institucion_laboral as institucionLaboralId,\n" +
                "                proyecto.id_institucion_colaboradora as institucionColaboradoraId,\n" +
                "                proyecto.id_institucion_subvencion as institucionSubvencionId,\n" +
                "                proyecto.id_institucion_subvencionadora as institucionSubvencionadoraId,\n" +
                "                proyecto.nombre_institucion_laboral as nombreInstitucionLaboral,\n" +
                "                proyecto.nombre_institucion_colabo as nombreInstitucionColaboradora,\n" +
                "                proyecto.nombre_institucion_subvencionadora as nombreInstitucionSubvencionadora,\n" +

                "                det_col.id_det_colaboradores as detColId,\n" +
                "                det_col.apellido_materno as detColApellidoMaterno,\n" +
                "                det_col.apellido_paterno as detColApellidoPaterno,\n" +
                "                det_col.nombres as detColNombres,\n" +
                "                det_col.dni as detColDni,\n" +
                "                det_col.email as detColEmail,\n" +
                "                det_col.id_cab_colaboradores,\n" +
                "                cab_col.id_investigador,\n" +

                "                investigador.id_investigador as investigadorId,\n" +
                "                investigador.apellido_paterno as investigadorApellidoPaterno,\n" +
                "                investigador.apellido_materno as investigadorApellidoMaterno,\n" +
                "                investigador.nombres  as investigadorNombres,\n" +
                "                investigador.email as investigadorEmail\n" +

                "from directorio_cti.tbl_proyecto proyecto\n" +
                "         left outer join directorio_cti.tbl_det_colaboradores det_col "
                + " on det_col.id_proyecto = proyecto.id_proyecto\n" +
                "         left outer join directorio_cti.tbl_cab_colaboradores cab_col "
                + " on cab_col.id_cab_colaboradores = det_col.id_cab_colaboradores\n" +
                "         left outer join directorio_cti.tbl_investigador investigador "
                + " on cab_col.id_investigador = investigador.id_investigador\n" +
                "where proyecto.id_proyecto = ?"
                ;

        Optional<CtiProyecto> object = jdbcTemplate.query(sql, new Object[]{proyectoId},
                new ProyectoRowMapper()).stream().findFirst();

        if (object.isEmpty()) {
            return null;
        }
        List<CtiPerson> colaboradores = jdbcTemplate.query(sql, new Object[]{proyectoId}, new ColaboradoresRowMapper());

        object.get().setColaboradoresEntities(colaboradores);

        return object.get();
    }

    private class ProyectoRowMapper implements RowMapper<CtiProyecto> {

        @Override
        public CtiProyecto mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiProyecto object = new CtiProyecto();
            object.setCtiId(rs.getInt("proyectoId"));
            object.setTitulo(rs.getString("titulo"));
            object.setDescripcion(rs.getString("descripcion"));
            object.setPalabrasClave(rs.getString("palabrasClave"));
            object.setFechaInicio(rs.getDate("fechaInicio"));
            object.setFechaFin(rs.getDate("fechaFin"));
            object.setResponsable(rs.getString("responsable"));
            object.setColaboradores(rs.getString("colaboradores"));
            object.setInstitucionLaboralId(rs.getInt("institucionLaboralId"));
            object.setInstitucionColaboradoraId(rs.getInt("institucionColaboradoraId"));
            object.setInstitucionSubvencionId(rs.getInt("institucionSubvencionId"));
            object.setInstitucionSubvencionadoraId(rs.getInt("institucionSubvencionadoraId"));

            object.setNombreInstitucionLaboral(rs.getString("nombreInstitucionLaboral"));
            object.setNombreInstitucionColaboradora(rs.getString("nombreInstitucionColaboradora"));
            object.setNombreInstitucionSubvencionadora(rs.getString("nombreInstitucionSubvencionadora"));

            CtiInvestigador investigador = new CtiInvestigador();
            investigador.setCtiId(rs.getInt("investigadorId"));
            investigador.setNombres(rs.getString("investigadorNombres"));
            investigador.setApellidoPaterno(rs.getString("investigadorApellidoPaterno"));
            investigador.setApellidoMaterno(rs.getString("investigadorApellidoMaterno"));
            investigador.setEmail(rs.getString("investigadorEmail"));

            object.setInvestigador(investigador);

            return object;
        }
    }

    private class ColaboradoresRowMapper implements RowMapper<CtiPerson> {

        @Override
        public CtiPerson mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiPerson object = new CtiPerson();

            object.setCtiId(rs.getInt("detColId"));
            object.setApellidoMaterno(rs.getString("detColApellidoMaterno"));
            object.setApellidoPaterno(rs.getString("detColApellidoPaterno"));
            object.setNombres(rs.getString("detColNombres"));
            object.setDni(rs.getString("detColDni"));
            object.setEmail(rs.getString("detColEmail"));

            return object;
        }
    }

    // ===================================
    // @ Fetch Derecho Pi
    // ===================================

    @Override
    public CtiDerechosPi getDerechosPi(Integer ctiDerechoPi) {
        String sql = "select distinct\n" +
                "    derechos.id_derechos_pi as derechosPiId,\n" +
                "    derechos.id_tipo_pi as tipoPiId,\n" +
                "    derechos.titulo_pi as tituloPi,\n" +
                "    derechos.nombre_propietario as nombreProprietario,\n" +
                "    derechos.numero_registro_pi as numeroRegistroPi,\n" +
                "    derechos.fecha_aceptacion as fechaAceptacion,\n" +
                "    derechos.id_investigador,\n" +

                "    investigador.id_investigador as investigadorId,\n" +
                "    investigador.apellido_paterno as investigadorApellidoPaterno,\n" +
                "    investigador.apellido_materno as investigadorApellidoMaterno,\n" +
                "    investigador.nombres  as investigadorNombres,\n" +
                "    investigador.email as investigadorEmail\n" +

                "from directorio_cti.tbl_derechos_pi derechos\n" +
                "         left outer join directorio_cti.tbl_investigador investigador "
                + " on derechos.id_investigador = investigador.id_investigador\n" +
                "where derechos.id_derechos_pi = ?"
                ;

        Optional<CtiDerechosPi> object = jdbcTemplate.query(sql, new Object[]{ctiDerechoPi},
                new DerechosPiRowMapper()).stream().findFirst();

        if (object.isEmpty()) {
            return null;
        }

        return object.get();
    }

    private class DerechosPiRowMapper implements RowMapper<CtiDerechosPi> {

        @Override
        public CtiDerechosPi mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (rs == null) {
                return null;
            }
            CtiDerechosPi object = new CtiDerechosPi();
            object.setCtiId(rs.getInt("derechosPiId"));
            object.setTipoPiId(rs.getInt("tipoPiId"));
            object.setTituloPi(rs.getString("tituloPi"));
            object.setNombreProprietario(rs.getString("nombreProprietario"));
            object.setNumeroRegistroPi(rs.getInt("numeroRegistroPi"));
            object.setFechaAceptacion(rs.getDate("fechaAceptacion"));

            CtiInvestigador investigador = new CtiInvestigador();
            investigador.setCtiId(rs.getInt("investigadorId"));
            investigador.setNombres(rs.getString("investigadorNombres"));
            investigador.setApellidoPaterno(rs.getString("investigadorApellidoPaterno"));
            investigador.setApellidoMaterno(rs.getString("investigadorApellidoMaterno"));
            investigador.setEmail(rs.getString("investigadorEmail"));

            object.setInvestigador(investigador);

            return object;
        }
    }

}
