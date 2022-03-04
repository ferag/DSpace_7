/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration tests for {@link XlsCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private Community community;

    private Collection collection;

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private XlsCrosswalk xlsCrosswalk;

    private DCInputsReader dcInputsReader;

    @Before
    public void setup() throws SQLException, AuthorizeException, DCInputsReaderException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

        dcInputsReader = mock(DCInputsReader.class);

        when(dcInputsReader.hasFormWithName("traditionalpageone-oairecerif-identifier-url")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpageone-oairecerif-identifier-url"))
            .thenReturn(Arrays.asList("oairecerif.identifier.url", "crisrp.site.title"));

        when(dcInputsReader.hasFormWithName("traditionalpageone-oairecerif-person-affiliation")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpageone-oairecerif-person-affiliation"))
            .thenReturn(Arrays.asList("oairecerif.person.affiliation", "oairecerif.affiliation.startDate",
                "oairecerif.affiliation.endDate", "oairecerif.affiliation.role"));

        when(dcInputsReader.hasFormWithName("traditionalpageone-crisrp-education")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpageone-crisrp-education"))
            .thenReturn(Arrays.asList("crisrp.education", "crisrp.education.start",
                "crisrp.education.end", "crisrp.education.role"));

        when(dcInputsReader.hasFormWithName("traditionalpagetwo-crisrp-qualification")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpagetwo-crisrp-qualification"))
            .thenReturn(Arrays.asList("crisrp.qualification", "crisrp.qualification.start",
                "crisrp.qualification.end"));

        when(dcInputsReader.hasFormWithName("traditionalpageone-dc-contributor-author")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpageone-dc-contributor-author"))
            .thenReturn(Arrays.asList("dc.contributor.author", "oairecerif.author.affiliation"));

        when(dcInputsReader.hasFormWithName("traditionalpagetwo-dc-contributor-editor")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditionalpagetwo-dc-contributor-editor"))
            .thenReturn(Arrays.asList("dc.contributor.editor", "oairecerif.editor.affiliation"));

    }

    @After
    public void after() throws DCInputsReaderException {
        if (this.xlsCrosswalk != null) {
            this.xlsCrosswalk.setDCInputsReader(new DCInputsReader());
        }
    }

    @Test
    @Ignore
    public void testDisseminateManyPersons() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullPersonItem();

        Item secondItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Edward Red")
            .withGivenName("Edward")
            .withFamilyName("Red")
            .withBirthDate("1982-05-21")
            .withGender("M")
            .withPersonAffiliation("OrgUnit")
            .withPersonAffiliationStartDate("2015-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item thirdItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Adam White")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University of Rome")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("person-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Nombre preferido", "Nombre de pila",
                "Apellido", "Apellido paterno", "Apellido materno", "Apellido casada", "Fecha de nacimiento", "Sexo",
                "Puesto de trabajo", "Afiliación principal", "Web personal", "Email", "Teléfono",
                "Teléfono móvil", "Intereses", "ORCID", "Dina", "Dni", "Pasaporte", "Tarjeta de inmigración", "Renacyt",
                "ID Scopus del autor", "IDs Investigador", "Afiliaciones", "Biografía", "Educación", "País",
                "Cualificaciones", "Idiomas", "Ubigeo reniec", "Calle de residencia", "Código postal",
                "País de residencia"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("John Smith", "John", "Smith", "", "", "",
                "1992-06-26", "M", "Researcher", "University",
                "www.test.com/Test||www.john-smith.com||www.site.com/Site", "test@test.com", "0744112233", "3201122333",
                "Science", "0000-0002-9079-5932", "DINA-01", "DNI-01", "PASS-01", "", "", "111", "r1||r2",
                "Company/2018-01-01//Developer", "Biography: \n\"This is my biography\"",
                "School/2000-01-01/2005-01-01/Student", "England",
                "First Qualification/2015-01-01/2016-01-01||Second Qualification/2016-01-02", "English||Italian", "",
                "Via 1 maggio", "05100", "IT"));

        assertThat(getRowValues(sheet.getRow(2)),
            contains("Edward Red", "Edward", "Red", "", "", "", "1982-05-21", "M", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "OrgUnit/2015-01-01//Developer", "", "", "", "", "", "", "", "",
                ""));

        assertThat(getRowValues(sheet.getRow(3)),
            contains("Adam White", "Adam", "White", "", "", "", "1962-03-23", "M", "Researcher",
                "University of Rome", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "School/2000-01-01/2005-01-01/Student", "", "", "English||Italian", "", "", "", ""));

    }

    @Test
    @Ignore
    public void testDisseminateSinglePerson() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withVariantName("Heisenberg")
            .withVariantName("W.W.")
            .withGivenName("Walter")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Professor")
            .withPersonMainAffiliation("High School")
            .withPersonKnowsLanguages("English")
            .withPersonEducation("School")
            .withPersonEducationStartDate("1968-09-01")
            .withPersonEducationEndDate("1973-06-10")
            .withPersonEducationRole("Student")
            .withPersonEducation("University")
            .withPersonEducationStartDate("1980-09-01")
            .withPersonEducationEndDate("1985-06-10")
            .withPersonEducationRole("Student")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonQualification("Qualification")
            .withPersonQualificationStartDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("person-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Nombre preferido", "Nombre de pila",
                "Apellido", "Apellido paterno", "Apellido materno", "Apellido casada", "Fecha de nacimiento", "Sexo",
                "Puesto de trabajo", "Afiliación principal", "Web personal", "Email", "Teléfono",
                "Teléfono móvil", "Intereses", "ORCID", "Dina", "Dni", "Pasaporte", "Tarjeta de inmigración", "Renacyt",
                "ID Scopus del autor", "IDs Investigador", "Afiliaciones", "Biografía", "Educación", "País",
                "Cualificaciones", "Idiomas", "Ubigeo reniec", "Calle de residencia", "Código postal",
                "País de residencia"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("Walter White", "Walter", "White", "", "", "", "1962-03-23", "M",
                "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932", "", "", "", "", "", "", "",
                "", "", "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
                "Qualification", "English", "", "", "", ""));

    }

    @Test
    @Ignore
    public void testDisseminatePublications() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullPublicationItem();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDoiIdentifier("doi:222.222/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::learning object")
            .withIssueDate("2019-12-31")
            .withAuthor("Edward Smith")
            .withAuthorAffiliation("Company")
            .withAuthor("Walter White")
            .withVolume("V-02")
            .withCitationStartPage("1")
            .withCitationEndPage("20")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Another Publication")
            .withDoiIdentifier("doi:333.333/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::clinical trial")
            .withIssueDate("2010-02-01")
            .withAuthor("Jessie Pinkman")
            .withDescriptionAbstract("Description of publication")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("publication-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));


        assertThat(getRowValues(sheet.getRow(0)),
            contains("Título", "Subtítulo", "Tipo", "Idioma", "Fecha de publicación", "Parte de", "Revista o serie",
                "ISBN (del contenedor)", "ISSN (del contenedor)", "DOI (del contenedor)", "Editorial", "DOI", "ISBN",
                "ISSN", "PubMed ID", "Número ISI", "Número SCP", "URL(s) alternativas", "Volumen", "Edición",
                "Página de inicio", "Página de fin", "Autores", "Editores", "Resumen", "Evento", "Producto", "Versión",
                "Notas internas", "Licencia", "Condición de acceso", "Fecha de Fin del Embargo", "Materia(s) DDC",
                "Materia(s) LOC", "Red de palabra(s) clave", "Materia(s) OCDE", "Revisión de",
                "ISBN (del elemento revisado)", "DOI (del elemento revisado)", "Sponsors", "Asesores", "Tipo Renati",
                "Nombre de la carrera", "Nivel", "Disciplina de la carrera", "Disciplina(s)", "Jurado(s)", "Donante",
                "Cita", "Descripción", "Sponsors"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("Test Publication", "Alternative publication title", "http://purl.org/coar/resource_type/c_efa0",
                "en", "2020-01-01", "Published in publication", "", "", "", "doi:10.3972/test", "Publication publisher",
                "doi:111.111/publication", "978-3-16-148410-0", "2049-3630", "", "111-222-333", "99999999", "", "V.01",
                "Issue", "", "", "John Smith||Walter White/Company", "Editor/Editor Affiliation", "",
                "The best Conference", "DataSet", "V01", "", "", "", "", "", "", "", "", "Coverage publication", "", "",
                "", "First advisor||Second advisor", "", "", "", "", "discipline", "", "", "", "", ""));

        assertThat(getRowValues(sheet.getRow(2)),
            contains("Second Publication", "", "http://purl.org/coar/resource_type/c_e059", "", "2019-12-31", "", "",
                "", "", "", "", "doi:222.222/publication", "", "", "", "", "", "", "V-02", "", "1", "20",
                "Edward Smith/Company||Walter White", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", ""));

        assertThat(getRowValues(sheet.getRow(3)),
            contains("Another Publication", "", "http://purl.org/coar/resource_type/c_cb28", "", "2010-02-01", "", "",
                "", "", "", "", "doi:333.333/publication", "", "", "", "", "", "", "", "", "", "", "Jessie Pinkman", "",
                "Description of publication", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", ""));

    }

    @Test
    @Ignore
    public void testDisseminateProjects() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullProjectItem();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("STP")
            .withTitle("Second Test Project")
            .withOpenaireId("55-66-77")
            .withOpenaireId("11-33-22")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2010-01-01")
            .withProjectEndDate("2012-12-31")
            .withProjectStatus("Status")
            .withProjectCoordinator("Second Coordinator OrgUnit")
            .withProjectInvestigator("Second investigator")
            .withProjectCoinvestigators("Coinvestigator")
            .withRelationEquipment("Another test equipment")
            .withOAMandateURL("oamandate")
            .withResearchLine("Research line")
            .withGeoLocationPlace("location1")
            .withGeoLocationPlace("location2")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TTP")
            .withTitle("Third Test Project")
            .withOpenaireId("88-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Third Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withSubject("project")
            .withSubject("test")
            .withOAMandate("false")
            .withTypeOCDE("oecd_project_type::Innovación tecnológica::Innovación de producto")
            .withOAMandateURL("www.oamandate.com")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("project-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Tipo OCDE", "Otro tipo", "Título", "Acrónimo", "OpenAIRE id(s)", "URL(s)", "Fecha de inicio",
                "Fecha de fin", "Estado", "Coordinador(es)", "Organizacion(es) socias",
                "Organizacion(es) participantes", "Coordinador del proyecto", "Co-Investigador(es)",
                "Usa equipmiento(s)", "Palabra(s) clave", "Descripción", "Mandato OA", "URL Políticas OA",
                "Linea de investigación", "Geo localizaciones", "Clasificación industrial", "Técnicos",
                "Staff de apoyo"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("", "", "Test Project", "TP", "11-22-33", "www.project.test", "2020-01-01", "2020-12-31", "OPEN",
                "Coordinator OrgUnit", "Partner OrgUnit||Another Partner OrgUnit",
                "First Member OrgUnit||Second Member OrgUnit||Third Member OrgUnit", "Investigator",
                "First coinvestigator||Second coinvestigator", "Test equipment", "project||test",
                "This is a project to test the export", "true", "oamandate-url", "", "", "", "", ""));

        assertThat(getRowValues(sheet.getRow(2)),
            contains("", "", "Second Test Project", "STP", "55-66-77||11-33-22", "www.project.test", "2010-01-01",
                "2012-12-31", "Status", "Second Coordinator OrgUnit", "", "", "Second investigator", "Coinvestigator",
                "Another test equipment", "", "", "", "oamandate", "Research line", "location1||location2", "", "",
                ""));

        assertThat(getRowValues(sheet.getRow(3)),
            contains("https://purl.org/pe-repo/ocde/tipoProyecto#innovacionDeProducto", "", "Third Test Project", "TTP", "88-22-33", "www.project.test", "2020-01-01", "2020-12-31",
                "OPEN", "Third Coordinator OrgUnit", "Partner OrgUnit", "Member OrgUnit", "Investigator",
                "First coinvestigator||Second coinvestigator", "", "project||test", "", "false", "www.oamandate.com",
                "", "", "", "", ""));
    }

    @Test
    @Ignore
    public void testDisseminateOrgUnits() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .withOrgUnitDirector("OU Director")
            .withOrgUnitFoundingDate("1990-05-12")
            .withOrgUnitBoard("board1")
            .withOrgUnitBoard("board2")
            .withOrgUnitRucIdentifier("RUC-01")
            .withOrgUnitRinIdentifier("RIN-01")
            .withOrgUnitRorIdentifier("ROR-01")
            .withOrgUnitScopusAffiliationIdentifier("SCOPUS-01")
            .withOrgUnitCrossRefFunderIdentifier("CRF-01")
            .withOrgUnitAddressLocality("via del canale")
            .withOrgUnitAddressCountry("Italy")
            .withSubjectOCDE("oecd::Ciencias sociales::Geografía social, Geografía económica::Estudios urbanos")
            .withUbigeo("010201")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .withOrgUnitRucIdentifier("RUC-02")
            .withOrgUnitRinIdentifier("RIN-02")
            .withOrgUnitRorIdentifier("ROR-02")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TTOU")
            .withTitle("Third Test OrgUnit")
            .withType("Private non-profit")
            .withOrgUnitIdentifier("ID-03")
            .withUrlIdentifier("www.orgUnit.test")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("orgUnit-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Nombre", "Nombre legal", "Acrónimo", "Tipo",
                "Institución padre", "ID(s) general", "URL(s)", "RUC ID(s)", "International Standard Name ID(s)",
                "Research Organization Registry ID(s)", "Ringgold ID(s)", "ID Scopus de la afiliación",
                "ID CrossRef del Fundandor", "Materia(s)", "Palabra(s) clave", "País de residencia",
                "Localidad de residencia", "Descripción", "UbiGeo"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("Test OrgUnit", "Test OrgUnit LegalName", "TOU",
                "Instituto de Investigación Estratégica",
                "Parent OrgUnit", "ID-01||ID-02", "www.orgUnit.com||www.orgUnit.it", "RUC-01", "",
                "ROR-01", "RIN-01", "SCOPUS-01", "CRF-01", "Estudios urbanos", "",
                "Italy", "via del canale", "", "010201"));

        assertThat(getRowValues(sheet.getRow(2)),
            contains("Another Test OrgUnit", "", "ATOU",
                "Privado sin ánimo de lucro", "Parent OrgUnit",
                "ID-03", "", "RUC-02", "", "ROR-02", "RIN-02", "", "", "", "", "", "", "", ""));

        assertThat(getRowValues(sheet.getRow(3)),
            contains("Third Test OrgUnit", "", "TTOU",
                "Privado sin ánimo de lucro", "", "ID-03",
                "www.orgUnit.test", "", "", "", "", "", "", "", "", "", "", "", ""));
    }

    @Test
    @Ignore
    public void testDisseminateEquipments() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("FT-EQ")
            .withType("Type")
            .withTitle("First Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .withUsageType("Investigacion cientifica y desarrollo experimental")
            .withSubjectOCDE("oecd::Ciencias sociales::Geografía social, " +
                "Geografía económica::Planificación del transporte y aspectos sociales del transporte")
            .withSubjectOCDE("oecd::Ciencias sociales::Geografía social, Geografía económica::Estudios urbanos")
            .withResearchLine("ResearchLine")
            .withRelationFunding("Funding")
            .withManufacturingCountry("IT")
            .withManufacturingDate("2020-01-01")
            .withAcquisitionDate("2021-01-01")
            .withAmount("4000")
            .withAmountCurrency("€")
            .withInternalNote("Note")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("ST-EQ")
            .withTitle("Second Test Equipment")
            .withInternalId("ID-02")
            .withDescription("This is another equipment to test the export functionality")
            .withEquipmentOwnerPerson("John Smith")
            .withSubjectOCDE("oecd::Ciencias sociales::Geografía social, Geografía económica::Estudios urbanos")
            .withRelationFunding("First funding")
            .withRelationFunding("Second funding")
            .withAcquisitionDate("2021-02-01")
            .withAmount("5000")
            .withAmountCurrency("€")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("TT-EQ")
            .withTitle("Third Test Equipment")
            .withInternalId("ID-03")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("equipment-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Nombre", "Acrónimo", "Tipo", "Identificador de la institución", "Descripción",
                "Organización propietaria", "Persona propietaria", "Uso", "Materia(s)", "Linea de investigación",
                "Financiador(es)", "País de manufactura", "Fecha de manufactura", "Fecha de adquisición", "Cantidad",
                "Moneda", "Nota interna"));

        assertThat(getRowValues(sheet.getRow(1)), contains("First Test Equipment", "FT-EQ", "Type", "ID-01",
            "This is an equipment to test the export functionality", "Test OrgUnit", "Walter White",
            "Investigacion cientifica y desarrollo experimental",
            "Planificación del transporte y aspectos sociales del transporte||Estudios urbanos",
            "ResearchLine", "Funding", "IT", "2020-01-01", "2021-01-01", "4000", "€", "Note"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Second Test Equipment", "ST-EQ", "", "ID-02",
            "This is another equipment to test the export functionality", "", "John Smith", "",
            "Estudios urbanos", "",
            "First funding||Second funding", "", "", "2021-02-01", "5000", "€", ""));

        assertThat(getRowValues(sheet.getRow(3)), contains("Third Test Equipment", "TT-EQ", "", "ID-03", "", "", "", "",
            "", "", "", "", "", "", "", "", ""));
    }

    @Test
    @Ignore
    public void testDisseminateFundings() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .withFundingParent("Parent Funding")
            .withExecutedAmount("15.000,00")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("AT-FU")
            .withTitle("Another Test Funding")
            .withType("Grant")
            .withInternalId("ID-02")
            .withFundingIdentifier("0002")
            .withAmount("10.000,00")
            .withFunder("Test Funder")
            .withFundingStartDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .withExecutedAmount("25.000,00")
            .withExecutedAmountCurrency("€")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("TT-FU")
            .withTitle("Third Test Funding")
            .withType("Grant")
            .withInternalId("ID-03")
            .withFundingIdentifier("0003")
            .withAmount("20.000,00")
            .withAmountCurrency("EUR")
            .withFundingEndDate("2010-01-01")
            .withOAMandate("false")
            .withOAMandateURL("www.mandate.com")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("funding-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)),
            contains("Nombre", "Acrónimo", "Tipo", "Código de financiamiento", "Cantidad", "Moneda",
                "Cantidad ejecutada", "Moneda de la cantidad ejecutada", "Descripción", "Palabra(s) clave",
                "Financiador", "Parte de", "Fecha de inicio", "Fecha de fin", "Mandato OA", "URL de políticas OA"));

        assertThat(getRowValues(sheet.getRow(1)),
            contains("Test Funding", "T-FU", "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Gift",
                "ID-01", "30.000,00", "EUR", "15.000,00", "", "Funding to test export", "", "OrgUnit Funder",
                "Parent Funding", "2015-01-01", "2020-01-01", "true", "www.mandate.url"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Another Test Funding", "AT-FU",
            "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Grant", "ID-02", "10.000,00",
            "", "25.000,00", "€", "", "", "Test Funder", "", "2020-01-01", "", "true", "www.mandate.url"));

        assertThat(getRowValues(sheet.getRow(3)),
            contains("Third Test Funding", "TT-FU",
                "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Grant", "ID-03",
                "20.000,00", "EUR", "", "", "", "", "", "", "", "2010-01-01", "false", "www.mandate.com"));
    }

    @Test
    @Ignore
    public void testDisseminatePatents() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("First patent")
            .withPatentCountry("Italy")
            .withDateSubmitted("2020-01-01")
            .withIssueDate("2021-01-01")
            .withLanguage("en")
            .withType("patent")
            .withPublisher("First publisher")
            .withPublisher("Second publisher")
            .withPatentNo("12345-666")
            .withAuthor("Walter White", "b6ff8101-05ec-49c5-bd12-cba7894012b7")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("John Smith", "will be referenced::ORCID::0000-0000-0012-3456")
            .withAuthorAffiliation("4Science")
            .withRightsHolder("Test Organization")
            .withDescriptionAbstract("This is a patent")
            .withRelationPatent("Another patent")
            .withSubject("patent")
            .withSubject("test")
            .withRelationFunding("Test funding")
            .withRelationProject("First project")
            .withRelationProject("Second project")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Second patent")
            .withType("patent")
            .withPatentNo("12345-777")
            .withAuthor("Bruce Wayne")
            .withRelationPatent("Another patent")
            .withSubject("second")
            .withRelationFunding("Funding")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Third patent")
            .withPatentCountry("England")
            .withDateSubmitted("2019-01-01")
            .withLanguage("ita")
            .withPublisher("Publisher")
            .withPatentNo("12345-888")
            .withRightsHolder("Organization")
            .withDescriptionAbstract("Patent description")
            .withRelationPatent("Another patent")
            .withRelationFunding("First funding")
            .withRelationFunding("Second funding")
            .build();

        context.restoreAuthSystemState();

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("patent-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)), contains("Title", "Approval date", "Registration date",
            "Patent number", "Country", "Type", "Language", "Inventor(s)", "Holder(s)", "Keyword(s)",
            "Funding(s)", "Project(s)", "Predecessor(s)", "Abstract"));

        assertThat(getRowValues(sheet.getRow(1)), contains("First patent", "2020-01-01", "2021-01-01", "12345-666",
            "Italy", "patent", "en", "Walter White/4Science||Jesse Pinkman||John Smith/4Science", "Test Organization",
            "patent||test", "Test funding", "First project||Second project",
            "Another patent", "This is a patent"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Second patent", "", "", "12345-777", "", "patent", "",
            "Bruce Wayne", "", "second", "Funding", "", "Another patent", ""));

        assertThat(getRowValues(sheet.getRow(3)), contains("Third patent", "2019-01-01", "", "12345-888", "England",
            "", "ita", "", "Organization", "", "First funding||Second funding", "", "Another patent",
            "Patent description"));
    }

    @Test
    @Ignore
    public void testDisseminateWithNotPublicMetadataFields() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
            .withEmail("owner@email.com")
            .withNameInMetadata("Walter", "White")
            .build();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withCrisOwner(owner)
            .withVariantName("Heisenberg")
            .withVariantName("W.W.")
            .withGivenName("Walter")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Professor")
            .withPersonMainAffiliation("High School")
            .withPersonKnowsLanguages("English")
            .withPersonEducation("School")
            .withPersonEducationStartDate("1968-09-01")
            .withPersonEducationEndDate("1973-06-10")
            .withPersonEducationRole("Student")
            .withPersonEducation("University")
            .withPersonEducationStartDate("1980-09-01")
            .withPersonEducationEndDate("1985-06-10")
            .withPersonEducationRole("Student")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonQualification("Qualification")
            .withPersonQualificationStartDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("person-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, out);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "Walter", "White", "", "", "",
            "1962-03-23", "M", "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "", "", "", "", "",
            "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English", "", "", "", ""));

        context.turnOffAuthorisationSystem();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        CrisLayoutBoxBuilder.createBuilder(context, personType, false, false)
            .addField(createCrisLayoutField("oairecerif.person.gender"))
            .addField(createCrisLayoutField("person.birthDate"))
            .withSecurity(LayoutSecurity.OWNER_ONLY)
            .build();
        context.restoreAuthSystemState();

        out = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, out);

        workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "Walter", "White", "", "", "",
            "1962-03-23", "M", "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "", "", "", "", "",
            "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English", "", "", "", ""));

    }

    @Test
    @Ignore
    public void testCvPersonDisseminateWithNotPublicMetadataFields() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
            .withEmail("owner@email.com")
            .withNameInMetadata("Walter", "White")
            .build();

        Item item = createItem(context, collection)
            .withEntityType("CvPerson")
            .withTitle("Walter White")
            .withCrisOwner(owner)
            .withVariantName("Heisenberg")
            .withVariantName("W.W.")
            .withGivenName("Walter")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Professor")
            .withPersonMainAffiliation("High School")
            .withPersonKnowsLanguages("English")
            .withPersonEducation("School")
            .withPersonEducationStartDate("1968-09-01")
            .withPersonEducationEndDate("1973-06-10")
            .withPersonEducationRole("Student")
            .withPersonEducation("University")
            .withPersonEducationStartDate("1980-09-01")
            .withPersonEducationEndDate("1985-06-10")
            .withPersonEducationRole("Student")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonQualification("Qualification")
            .withPersonQualificationStartDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("ctivitae-profile-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, out);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "Walter", "White", "", "", "",
            "1962-03-23", "M", "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "", "", "", "", "",
            "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English", "", "", "", ""));

        context.turnOffAuthorisationSystem();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();

        CrisLayoutBoxBuilder.createBuilder(context, personType, false, false)
            .addField(createCrisLayoutField("oairecerif.person.gender"))
            .addField(createCrisLayoutField("person.birthDate"))
            .withSecurity(LayoutSecurity.OWNER_ONLY)
            .build();
        context.restoreAuthSystemState();

        out = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, out);

        workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "Walter", "White", "", "", "",
            "", "", "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "", "", "", "", "",
            "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English", "", "", "", ""));

        context.setCurrentUser(owner);

        out = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, out);

        workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "Walter", "White", "", "", "",
            "1962-03-23", "M", "Professor", "High School", "", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "", "", "", "", "",
            "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English", "", "", "", ""));

    }

    private Item createFullPersonItem() {
        return createItem(context, collection)
            .withTitle("John Smith")
            .withEntityType("Person")
            .withFullName("John Smith")
            .withVernacularName("JOHN SMITH")
            .withVariantName("J.S.")
            .withVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("111")
            .withResearcherIdentifier("r1")
            .withResearcherIdentifier("r2")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withDescriptionAbstract("Biography: \n\"This is my biography\"")
            .withPersonCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .withPersonQualification("Second Qualification")
            .withPersonQualificationStartDate("2016-01-02")
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPhone("0744112233")
            .withMobilePhone("3201122333")
            .withDinaIdentifier("DINA-01")
            .withDniIdentifier("DNI-01")
            .withPassport("PASS-01")
            .withStreetAddress("Via 1 maggio")
            .withCountryAddress("IT")
            .withPostalCode("05100")
            .withUbigeo("12345")
            .build();
    }

    private Item createFullPublicationItem() {
        return ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationConference("The best Conference")
            .withRightsUri("rights-uri")
            .withCoveragePublication("Coverage publication")
            .withVersion("V01")
            .withAdvisor("First advisor")
            .withAdvisor("Second advisor")
            .withRenatiDiscipline("discipline")
            .withRelationProduct("DataSet")
            .build();
    }

    private Item createFullProjectItem() {
        return ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectPartner("Another Partner OrgUnit")
            .withProjectOrganization("First Member OrgUnit")
            .withProjectOrganization("Second Member OrgUnit")
            .withProjectOrganization("Third Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();
    }

    private List<String> getRowValues(Row row) {
        return StreamSupport.stream(row.spliterator(), false)
            .map(cell -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue())
            .collect(Collectors.toList());
    }

    private CrisLayoutField createCrisLayoutField(String metadataField) throws SQLException, AuthorizeException {
        return CrisLayoutFieldBuilder.createMetadataField(context, metadataField, 0, 0).build();
    }
}
