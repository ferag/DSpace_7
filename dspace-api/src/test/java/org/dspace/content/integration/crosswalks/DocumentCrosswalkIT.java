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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link DocumentCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithJpegImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithPngImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withEntityType("Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withEntityType("Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.png"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithJpegImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
            assertThatRtfHasJpegImage(out);
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithPngImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withEntityType("Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withEntityType("Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.png"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
            assertThatRtfHasJpegImage(out);
        }

    }

    @Test
    public void testPdfCrosswalkPublicationDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-04-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
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
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withPerucrisSubjectOCDE("OCDE")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "publication-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, publication, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPublicationDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkProjectDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withOpenaireId("44-55-66")
            .withUrlIdentifier("www.project.test")
            .withUrlIdentifier("www.test.project")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
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
            .withPerucrisSubjectOCDE("First OCDE Subject")
            .withPerucrisSubjectOCDE("Second OCDE Subject")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test funding")
            .withType("Award")
            .withFunder("Another OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "project-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, project, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatProjectDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkEquipmentDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item equipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("T-EQ")
            .withTitle("Test Equipment")
            .withInternalId("ID-01")
            .withType("Type")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .withUsageType("Investigacion cientifica y desarrollo experimental")
            .withSubjectOCDE("First subject")
            .withSubjectOCDE("Second subject")
            .withResearchLine("ResearchLine")
            .withRelationFunding("Funding")
            .withRelationFunding("Another Funding")
            .withManufacturingCountry("IT")
            .withManufacturingDate("2020-01-01")
            .withAcquisitionDate("2021-01-01")
            .withAmount("4000")
            .withAmountCurrency("€")
            .withInternalNote("Note")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "equipment-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, equipment, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatEquipmentDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkOrgUnitDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .withUbigeo("UBIGEO")
            .withOrgUnitAddressCountry("Italy")
            .withOrgUnitAddressLocality("Via Roma")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "orgUnit-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, orgUnit, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatOrgUnitDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkFundingDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withExecutedAmount("15.000,00")
            .withExecutedAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFunder("OrgUnit Funder 2")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "funding-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, funding, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatFundingDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPatentDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Test patent")
            .withDateSubmitted("2020-01-01")
            .withIssueDate("2021-01-01")
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
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "patent-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, patent, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPatentDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithEmptyPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Test user")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> {
                assertThat(content, equalTo("Test user\n"));
            });
        }

    }

    private Item buildPersonItem() {
        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
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
            .withScopusAuthorIdentifier("111-222-333")
            .withScopusAuthorIdentifier("444-555-666")
            .withPersonAffiliation("University")
            .withPersonAffiliationStartDate("2020-01-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2015-01-01")
            .withPersonAffiliationEndDate("2020-01-01")
            .withPersonAffiliationRole("Developer")
            .withDescriptionAbstract(getBiography())
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
            .build();
        return item;
    }

    private String getBiography() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.Lorem ipsum dolor sit amet, consectetur "
            + "adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.";
    }

    private void assertThatRtfHasContent(ByteArrayOutputStream out, Consumer<String> assertConsumer)
        throws IOException, BadLocationException {
        RTFEditorKit rtfParser = new RTFEditorKit();
        Document document = rtfParser.createDefaultDocument();
        rtfParser.read(new ByteArrayInputStream(out.toByteArray()), document, 0);
        String content = document.getText(0, document.getLength());
        assertConsumer.accept(content);
    }

    private void assertThatRtfHasJpegImage(ByteArrayOutputStream out) {
        assertThat(out.toString(), containsString("\\jpegblip"));
    }

    private void assertThatPdfHasContent(ByteArrayOutputStream out, Consumer<String> assertConsumer)
        throws InvalidPasswordException, IOException {
        PDDocument document = PDDocument.load(out.toByteArray());
        String content = new PDFTextStripper().getText(document);
        assertConsumer.accept(content);
    }

    private void assertThatPersonDocumentHasContent(String content) {
        assertThat(content, containsString("John Smith"));
        assertThat(content, containsString("Researcher en University"));

        assertThat(content, containsString("Fecha de nacimiento: 1992-06-26"));
        assertThat(content, containsString("Sexo: M"));
        assertThat(content, containsString("País: England"));
        assertThat(content, containsString("Correo electrónico: test@test.com"));
        assertThat(content, containsString("ORCID: 0000-0002-9079-5932"));
        assertThat(content, containsString("ID Scopus del autor: 111-222-333, 444-555-666"));
        assertThat(content, containsString("Lorem ipsum dolor sit amet"));

        assertThat(content, containsString("Afiliaciones"));
        assertThat(content, containsString("Researcher en University desde 2020-01-02"));
        assertThat(content, containsString("Developer en Company desde 2015-01-01 hasta 2020-01-01"));

        assertThat(content, containsString("Educación"));
        assertThat(content, containsString("Student en School desde 2000-01-01 hasta 2005-01-01"));

        assertThat(content, containsString("Cualificaciones"));
        assertThat(content, containsString("First Qualification desde 2015-01-01 hasta 2016-01-01"));
        assertThat(content, containsString("Second Qualification desde 2016-01-02"));

        assertThat(content, containsString("Publicaciones"));
        assertThat(content, containsString("John Smith y Walter White (2020-01-01). First Publication"));
        assertThat(content, containsString("John Smith (2020-04-01). Second Publication"));

        assertThat(content, containsString("Otra información"));
        assertThat(content, containsString("Intereses: Science"));
        assertThat(content, containsString("Idiomas: English, Italian"));
        assertThat(content, containsString("Web personal: www.test.com ( Test ) , www.john-smith.com , "
            + "www.site.com ( Site )"));
    }

    private void assertThatPublicationDocumentHasContent(String content) {
        assertThat(content, containsString("Test Publication"));

        assertThat(content, containsString("Información básica de la publicación"));
        assertThat(content, containsString("Otros títulos: Alternative publication title"));
        assertThat(content, containsString("Fecha de publicación: 2020-01-01"));
        assertThat(content, containsString("DOI: doi:111.111/publication"));
        assertThat(content, containsString("ISBN: 978-3-16-148410-0"));
        assertThat(content, containsString("Número ISI: 111-222-333"));
        assertThat(content, containsString("Número SCP: 99999999"));
        assertThat(content, containsString("Autor(es): John Smith and Walter White ( Company )"));
        assertThat(content, containsString("Editor(es): Editor ( Editor Affiliation )"));
        assertThat(content, containsString("Editorial(es): Publication publisher"));
        assertThat(content, containsString("Palabras clave: test, export"));
        assertThat(content, containsString("Tipo: Controlled Vocabulary for Resource Type Genres::text::review"));
        assertThat(content, containsString("Materia(s) OCDE: OCDE"));
        assertThat(content, containsString("Idioma: Inglés"));

        assertThat(content, containsString("Detalles bibliográficos de la publicación"));
        assertThat(content, containsString("Published in publication - DOI: doi:10.3972/test"));
        assertThat(content, containsString("ISSN: 2049-3630"));
        assertThat(content, containsString("Volumen: V.01"));
        assertThat(content, containsString("Fascículo: Issue"));

        assertThat(content, containsString("Proyectos"));
        assertThat(content, containsString("Test Project ( TP ) - from 2020-01-01 to 2020-04-01"));

        assertThat(content, containsString("Financiadores"));
        assertThat(content, containsString("Another Test Funding ( ATF-01 ) - Funder: Another Test Funder"));
    }

    private void assertThatProjectDocumentHasContent(String content) {
        assertThat(content, containsString("Test Project"));
        assertThat(content, containsString("This is a project to test the export"));

        assertThat(content, containsString("Información básica"));
        assertThat(content, containsString("Acrónimo de proyecto: TP"));
        assertThat(content, containsString("OpenAIRE id(s): 11-22-33, 44-55-66"));
        assertThat(content, containsString("URL(s): www.project.test, www.test.project"));
        assertThat(content, containsString("Fecha de inicio: 2020-01-01"));
        assertThat(content, containsString("Fecha de fin: 2020-12-31"));
        assertThat(content, containsString("Estado: OPEN"));

        assertThat(content, containsString("Consorcio"));
        assertThat(content, containsString("Organizaciones asociadas: Partner OrgUnit, Another Partner OrgUnit"));
        assertThat(content, containsString("Organizacions participantes: First Member OrgUnit, "
            + "Second Member OrgUnit, Third Member OrgUnit"));

        assertThat(content, containsString("Equipo"));
        assertThat(content, containsString("Coordinador general: Coordinator OrgUnit"));
        assertThat(content, containsString("Investigador principal: Investigator"));
        assertThat(content, containsString("Co-investigador(es): First coinvestigator, Second coinvestigator"));

        assertThat(content, containsString("Otra información"));
        assertThat(content, containsString("Campo del conoscimiento OCDE: First OCDE Subject, Second OCDE Subject"));
        assertThat(content, containsString("Usa equipmiento(s): Test equipment"));
        assertThat(content, containsString("Palabra(s) clave: project, test"));
        assertThat(content, containsString("Mandato OA: true"));
        assertThat(content, containsString("URL de políticas OA: oamandate-url"));

    }

    private void assertThatOrgUnitDocumentHasContent(String content) {
        assertThat(content, containsString("Test OrgUnit"));

        assertThat(content, containsString("Información basica"));
        assertThat(content, containsString("Acrónimo: TOU"));
        assertThat(content, containsString("Tipo: https://w3id.org/cerif/vocab/OrganisationTypes"
            + "#StrategicResearchInsitute"));
        assertThat(content, containsString("Organización padre: Parent OrgUnit"));
        assertThat(content, containsString("Identificador(es): ID-01, ID-02"));
        assertThat(content, containsString("URL(s): www.orgUnit.com, www.orgUnit.it"));
        assertThat(content, containsString("Direccion Postal/País: Italy"));
        assertThat(content, containsString("Dirección Postal/Localidad: Via Roma"));
        assertThat(content, containsString("Ubigeo: UBIGEO"));
    }

    private void assertThatEquipmentDocumentHasContent(String content) {
        assertThat(content, containsString("Test Equipment"));
        assertThat(content, containsString("This is an equipment to test the export functionality"));

        assertThat(content, containsString("Información básica"));
        assertThat(content, containsString("Acrónimo: T-EQ"));
        assertThat(content, containsString("Tipo de equipamiento: Type"));
        assertThat(content, containsString("Código del equipamiento: ID-01"));
        assertThat(content, containsString("Organización propietaria: Test OrgUnit"));
        assertThat(content, containsString("Funding, Another Funding"));
        assertThat(content, containsString("Campo de conocimiento OCDE: First subject, Second subject"));
        assertThat(content, containsString("Uso del equipamiento: Investigacion cientifica y desarrollo experimental"));
        assertThat(content, containsString("Línea de investigación institucional: ResearchLine"));
        assertThat(content, containsString("País de fabricación o ensamblaje: Italia"));
        assertThat(content, containsString("Fecha de fabricación: 2020-01-01"));
        assertThat(content, containsString("Fecha de adquisición: 2021-01-01"));
        assertThat(content, containsString("Monto de adquisición: 4000"));
        assertThat(content, containsString("Moneda de adquisición: €"));

    }

    private void assertThatFundingDocumentHasContent(String content) {
        assertThat(content, containsString("Test Funding"));
        assertThat(content, containsString("Funding to test export"));

        assertThat(content, containsString("Información básica"));
        assertThat(content, containsString("Acrónimo: T-FU"));
        assertThat(content, containsString("Tipo: Gift"));
        assertThat(content, containsString("Código de financiamiento: ID-01"));
        assertThat(content, containsString("Monto programado: 30.000,00 (EUR)"));
        assertThat(content, containsString("Monto ejecutado: 15.000,00 (EUR)"));
        assertThat(content, containsString("Entidad subvencionadora: OrgUnit Funder, OrgUnit Funder 2"));
        assertThat(content, containsString("Duración: desde 2015-01-01 hasta 2020-01-01"));
    }

    private void assertThatPatentDocumentHasContent(String text) {
        assertThat(text, containsString("Test patent"));
        assertThat(text, containsString("This is a patent"));
        assertThat(text, containsString("Información básica de la patente"));
        assertThat(text, containsString("Número de la patente: 12345-666"));
        assertThat(text, containsString("Inventor(es): Walter White (4Science), Jesse Pinkman, John Smith (4Science)"));
        assertThat(text, containsString("Titular(es) de la patente (Organizaciones): Test Organization"));
        assertThat(text, containsString("Fecha de presentación de solicitud original: 2020-01-01"));
        assertThat(text, containsString("Fecha de concesión: 2021-01-01"));
        assertThat(text, containsString("Palabra(s) clave: patent, test"));
        assertThat(text, containsString("Predecesor(es): Another patent"));
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
