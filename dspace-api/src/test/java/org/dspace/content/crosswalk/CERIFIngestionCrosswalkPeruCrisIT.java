/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CERIFIngestionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CERIFIngestionCrosswalkPeruCrisIT extends AbstractIntegrationTestWithDatabase {

    private static final String OAI_PMH_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/cerif/perucris";

    private static final String METADATA_PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private Community community;

    private Collection collection;

    private CERIFIngestionCrosswalk crosswalk;

    private SAXBuilder builder = new SAXBuilder();

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    @Before
    public void setup() throws Exception {

        crosswalk = (CERIFIngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class, "cerif");
        assertThat("A CERIF ingestion crosswalk should be configured", crosswalk, notNullValue());
        crosswalk.setIdPrefix("repository-id::");
        crosswalk.setMetadataConfig("perucris-cerif");

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Publication").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(17));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.title", "La Implementacion de ORCID en la UASLP como parte de "
            + "sus servicios de ciencia abierta")));

        assertThat(values, hasItems(with("dc.date.issued", "2018-10-03")));
        assertThat(values, hasItems(with("dc.publisher", "Consejo Nacional de Ciencia, Tecnologia e "
            + "Innovacion Tecnologica - Concytec")));

        assertThat(values, hasItems(with("dc.subject", "Sistema de informacion cientifica")));
        assertThat(values, hasItems(with("dc.subject", "Informacion cientifica", 1)));
        assertThat(values, hasItems(with("dc.subject", "Terminologia cientifica", 2)));
        assertThat(values, hasItems(with("dc.subject", "Software de codigo abierto", 3)));
        assertThat(values, hasItems(with("dc.subject", "Tecnologia de la informacion", 4)));

        assertThat(values, hasItems(with("dc.contributor.author", "Vazquez Tapia, Rosalina")));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", METADATA_PLACEHOLDER)));
        assertThat(values, hasItems(with("dc.description.abstract", "Es el Repositorio Institucional de Acceso Abierto"
            + " de la UASLP, que almacena y organiza los acervos y publicaciones universitarias para su consulta, "
            + "visibilidad y preservacion digital, de manera libre y gratuita. Esta desarrollado bajo un modelo propio "
            + "basado en los principios de un CRIS (Current Research Information System). El objetivo de ORBIS es "
            + "brindar a los investigadores una plataforma institucional que le permita gestionar su curriculum "
            + "universitario y generar indicadores de produccion cientifica.")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInstitutionPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("InstitutionPublication").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(17));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.title", "La Implementacion de ORCID en la UASLP como parte de "
            + "sus servicios de ciencia abierta")));

        assertThat(values, hasItems(with("dc.date.issued", "2018-10-03")));
        assertThat(values, hasItems(with("dc.publisher", "Consejo Nacional de Ciencia, Tecnologia e "
            + "Innovacion Tecnologica - Concytec")));

        assertThat(values, hasItems(with("dc.subject", "Sistema de informacion cientifica")));
        assertThat(values, hasItems(with("dc.subject", "Informacion cientifica", 1)));
        assertThat(values, hasItems(with("dc.subject", "Terminologia cientifica", 2)));
        assertThat(values, hasItems(with("dc.subject", "Software de codigo abierto", 3)));
        assertThat(values, hasItems(with("dc.subject", "Tecnologia de la informacion", 4)));

        assertThat(values, hasItems(with("dc.contributor.author", "Vazquez Tapia, Rosalina")));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", METADATA_PLACEHOLDER)));
        assertThat(values, hasItems(with("dc.description.abstract", "Es el Repositorio Institucional de Acceso Abierto"
            + " de la UASLP, que almacena y organiza los acervos y publicaciones universitarias para su consulta, "
            + "visibilidad y preservacion digital, de manera libre y gratuita. Esta desarrollado bajo un modelo propio "
            + "basado en los principios de un CRIS (Current Research Information System). El objetivo de ORBIS es "
            + "brindar a los investigadores una plataforma institucional que le permita gestionar su curriculum "
            + "universitario y generar indicadores de produccion cientifica.")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPersonIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Person").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-person.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();

        assertThat(values, hasSize(15));
        assertThat(values, hasItems(with("dc.title", "Olivares Poggi, Cesar Augusto")));
        assertThat(values, hasItems(with("person.givenName", "Cesar Augusto")));
        assertThat(values, hasItems(with("person.familyName", "Olivares Poggi")));
        assertThat(values, hasItems(with("oairecerif.person.gender", "m")));
        assertThat(values, hasItems(with("person.identifier.orcid", "https://orcid.org/0000-0003-2678-3544")));
        assertThat(values, hasItems(with("person.identifier.scopus-author-id", "56709412100")));
        assertThat(values, hasItems(with("oairecerif.person.affiliation",
            "Consejo Nacional de Ciencia, Tecnologia e Innovacion Tecnologica - Concytec")));
        assertThat(values, hasItems(with("oairecerif.affiliation.startDate", METADATA_PLACEHOLDER)));
        assertThat(values, hasItems(with("oairecerif.affiliation.endDate", METADATA_PLACEHOLDER)));
        assertThat(values, hasItems(with("oairecerif.affiliation.role", METADATA_PLACEHOLDER)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProjectIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Project").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-project.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(9));

        assertThat(values, hasItems(with("dc.title", "Development of ecofriendly composite materials based "
            + "on geopolymer matrix and reinforced with waste fibers")));
        assertThat(values, hasItems(with("oairecerif.project.startDate", "2016-11-29")));
        assertThat(values, hasItems(with("oairecerif.project.endDate", "2017-01-04")));
        assertThat(values, hasItems(with("oairecerif.funder", "Fondo Nacional de Desarrollo Cientifico y Tecnologico - "
            + "Fondecyt", null, "will be generated::repository-id::OrgUnits/3fb93c1e-e6c7-45e5-bdf7-e2cebde9e49d", 0,
            500)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrgUnitIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("OrgUnit").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-orgUnit.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(13));
        assertThat(values, hasItems(with("dc.title", "Consejo Nacional de Ciencia, Tecnologia e "
            + "Innovacion Tecnologica - Concytec")));
        assertThat(values, hasItems(with("dc.type", "Unspecified")));
        assertThat(values, hasItems(with("organization.identifier.ruc", "20135727394")));
        assertThat(values, hasItems(with("organization.identifier.isni", "0000000121665691")));
        assertThat(values, hasItems(with("organization.identifier.scopusaffid", "60089517")));
        assertThat(values, hasItems(with("organization.identifier.crossrefid", "10.13039/501100010747")));
        assertThat(values, hasItems(with("organization.address.addressCountry", "PE")));
        assertThat(values, hasItems(with("perucris.ubigeo", "http://purl.org/pe-repo/inei/ubigeo/1501")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFundingIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Funding").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-perucris-funding.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(14));
        assertThat(values, hasItems(with("dc.title", "PROCYT")));
        assertThat(values, hasItems(with("oairecerif.acronym", "E001")));
        assertThat(values, hasItems(with("dc.type", "Unspecified")));
        assertThat(values, hasItems(with("oairecerif.funding.identifier", "E001")));
        assertThat(values, hasItems(with("oairecerif.funder", "Fondo Nacional de Desarrollo Cientifico y Tecnologico "
            + "- Fondecyt", null, "will be generated::repository-id::OrgUnits/3fb93c1e-e6c7-45e5-bdf7-e2cebde9e49d", 0,
            500)));
        assertThat(values, hasItems(with("oairecerif.oamandate", "true")));
        assertThat(values, hasItems(with("oairecerif.oamandate.url", "https://portal.concytec.gob.pe/images/stories/"
            + "images2013/portal/areas-institucion/dsic/ley-30035.pdf")));
        assertThat(values, hasItems(with("dc.subject", "Investigacion Cientifica", 0)));
        assertThat(values, hasItems(with("dc.subject", "FONDECYT", 1)));

    }

    private Document readDocument(String dir, String name) throws Exception {
        try (InputStream inputStream = new FileInputStream(new File(dir, name))) {
            return builder.build(inputStream);
        }
    }
}
