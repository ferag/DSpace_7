/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import static org.dspace.content.LicenseUtils.getLicenseText;
import static org.dspace.content.MetadataSchemaEnum.CRIS;
import static org.dspace.content.MetadataSchemaEnum.DC;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.profile.OrcidEntitySyncPreference;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.app.profile.OrcidSynchronizationMode;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Builder to construct Item objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class ItemBuilder extends AbstractDSpaceObjectBuilder<Item> {
    private static final Logger log = LogManager.getLogger(ItemBuilder.class);

    private boolean withdrawn = false;
    private boolean inArchive = false;
    private String handle = null;
    private WorkspaceItem workspaceItem;
    private Item item;
    private Group readerGroup = null;

    protected ItemBuilder(Context context) {
        super(context);
    }

    public static ItemBuilder createItem(final Context context, final Collection col) {
        ItemBuilder builder = new ItemBuilder(context);
        return builder.create(context, col);
    }

    private ItemBuilder create(final Context context, final Collection col) {
        this.context = context;

        try {
            workspaceItem = workspaceItemService.create(context, col, false);
            item = workspaceItem.getItem();
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public ItemBuilder withTitle(final String title) {
        return setMetadataSingleValue(item, MetadataSchemaEnum.DC.getName(), "title", null, title);
    }

    public ItemBuilder withAlternativeTitle(final String title) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "title", "alternative", title);
    }

    public ItemBuilder withTitleForLanguage(final String title, final String language) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "title", null, language, title);
    }

    public ItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(),
                "date", "issued", new DCDate(issueDate).toString());
    }

    public ItemBuilder withIdentifierOther(final String identifierOther) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "identifier", "other", identifierOther);
    }

    public ItemBuilder withAuthor(final String authorName) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "author", authorName);
    }

    public ItemBuilder withAuthor(final String authorName, final String authority, final int confidence) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "author",
                null, authorName, authority, confidence);
    }

    public ItemBuilder withAuthor(final String authorName, final String authority) {
        return addMetadataValue(item, DC.getName(), "contributor", "author", null, authorName, authority, 600);
    }

    public ItemBuilder withAuthorAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "author", "affiliation", affiliation);
    }

    public ItemBuilder withAuthorAffiliation(String affiliation, String authority) {
        return addMetadataValue(item, "oairecerif", "author", "affiliation", null,
            affiliation,  authority, 600);
    }

    public ItemBuilder withAuthorAffiliationPlaceholder() {
        return addMetadataValue(item, "oairecerif", "author", "affiliation",
                CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    //adds an affiliattion to the author
    public ItemBuilder withAffiliation(String affiliation, String authority) {
        return addMetadataValue(item, "person", "affiliation", "name", null, affiliation, authority, 600);
    }

    public ItemBuilder withEditor(final String editorName) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "contributor", "editor", editorName);
    }

    public ItemBuilder withEditorAffiliationPlaceholder() {
        return addMetadataValue(item, "oairecerif", "editor", "affiliation",
                CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    public ItemBuilder withEditor(final String editorName, final String authority) {
        return addMetadataValue(item, DC.getName(), "contributor", "editor", null, editorName, authority, 600);
    }

    public ItemBuilder withEditorAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "editor", "affiliation", affiliation);
    }

    public ItemBuilder withPersonIdentifierFirstName(final String personIdentifierFirstName) {
        return addMetadataValue(item, "person", "givenName", null, personIdentifierFirstName);
    }

    public ItemBuilder withPersonIdentifierLastName(final String personIdentifierLastName) {
        return addMetadataValue(item, "person", "familyName", null, personIdentifierLastName);
    }

    public ItemBuilder withSubject(final String subject) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "subject", null, subject);
    }

    public ItemBuilder withSubject(final String subject, final String authority, final int confidence) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "subject", null, null,
                subject, authority, confidence);
    }

    public ItemBuilder withEntityType(final String entityType) {
        return addMetadataValue(item, "dspace", "entity", "type", entityType);
    }

    public ItemBuilder withPublicationIssueNumber(final String issueNumber) {
        return addMetadataValue(item, "publicationissue", "issueNumber", null, issueNumber);
    }

    public ItemBuilder withPublicationVolumeNumber(final String volumeNumber) {
        return addMetadataValue(item, "publicationvolume", "volumeNumber", null, volumeNumber);
    }

    public ItemBuilder withProvenanceData(final String provenanceData) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "provenance", provenanceData);
    }

    public ItemBuilder enableIIIF() {
        return addMetadataValue(item, "dspace", "iiif", "enabled", "true");
    }

    public ItemBuilder disableIIIF() {
        return addMetadataValue(item, "dspace", "iiif", "enabled", "false");
    }

    public ItemBuilder enableIIIFSearch() {
        return addMetadataValue(item, "iiif", "search", "enabled", "true");
    }

    public ItemBuilder withIIIFViewingHint(String hint) {
        return addMetadataValue(item, "iiif", "viewing", "hint", hint);
    }

    public ItemBuilder withIIIFCanvasNaming(String naming) {
        return addMetadataValue(item, "iiif", "canvas", "naming", naming);
    }

    public ItemBuilder withIIIFCanvasWidth(int i) {
        return addMetadataValue(item, "iiif", "image", "width", String.valueOf(i));
    }

    public ItemBuilder withIIIFCanvasHeight(int i) {
        return addMetadataValue(item, "iiif", "image", "height", String.valueOf(i));
    }

    public ItemBuilder withMetadata(final String schema, final String element, final String qualifier,
                                    final String value) {
        return addMetadataValue(item, schema, element, qualifier, value);
    }
    public ItemBuilder withSecuredMetadata(final String schema, final String element, final String qualifier,
                                    final String value, Integer securityLevel) {
        return addMetadataValue(item, schema, element, qualifier, value);
    }
    public ItemBuilder withCrisOwner(String value, String authority) {
        return addMetadataValue(item, CRIS.getName(), "owner", null, null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withCrisOwner(EPerson ePerson) {
        return withCrisOwner(ePerson.getFullName(), ePerson.getID().toString());
    }

    public ItemBuilder withCrisPolicyEPerson(String value, String authority) {
        return addMetadataValue(item, CRIS.getName(), "policy", "eperson", null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withCrisPolicyGroup(String value, String authority) {
        return addMetadataValue(item, CRIS.getName(), "policy", "group", null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withUriIdentifier(String uri) {
        return addMetadataValue(item, "dc", "identifier", "uri", uri);
    }

    public ItemBuilder withCtiVitaeOwner(String value, String authority) {
        return addMetadataValue(item, "perucris", "ctivitae", "owner", null, value, authority, CF_ACCEPTED);
    }

    public ItemBuilder withCtiVitaeOwner(Item owner) {
        return withCtiVitaeOwner(owner.getName(), owner.getID().toString());
    }

    public ItemBuilder withDoiIdentifier(String doi) {
        return addMetadataValue(item, "dc", "identifier", "doi", doi);
    }

    public ItemBuilder withIsbnIdentifier(String isbn) {
        return addMetadataValue(item, "dc", "identifier", "isbn", isbn);
    }

    public ItemBuilder withIssnIdentifier(String issn) {
        return addMetadataValue(item, "dc", "identifier", "issn", issn);
    }

    public ItemBuilder withRelationIssn(String issn) {
        return addMetadataValue(item, "dc", "relation", "issn", issn);
    }

    public ItemBuilder withIsiIdentifier(String issn) {
        return addMetadataValue(item, "dc", "identifier", "isi", issn);
    }

    public ItemBuilder withScopusIdentifier(String scopus) {
        return addMetadataValue(item, "dc", "identifier", "scopus", scopus);
    }

    public ItemBuilder withOrcidIdentifier(String orcid) {
        return addMetadataValue(item, "person", "identifier", "orcid", orcid);
    }

    public ItemBuilder withLegacyId(String legacyId) {

        return addMetadataValue(item, "cris", "legacyId", null, legacyId);

    }

    public ItemBuilder withOrcidAccessToken(String accessToken) {
        return addMetadataValue(item, "cris", "orcid", "access-token", accessToken);
    }

    public ItemBuilder withOrcidWebhook(String webhook) {
        return addMetadataValue(item, "cris", "orcid", "webhook", webhook);
    }

    public ItemBuilder withOrcidAuthenticated(String authenticated) {
        return addMetadataValue(item, "cris", "orcid", "authenticated", authenticated);
    }

    public ItemBuilder withOrcidSynchronizationPublicationsPreference(OrcidEntitySyncPreference value) {
        return withOrcidSynchronizationPublicationsPreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationPublicationsPreference(String value) {
        return setMetadataSingleValue(item, "cris", "orcid", "sync-publications", value);
    }

    public ItemBuilder withOrcidSynchronizationFundingsPreference(OrcidEntitySyncPreference value) {
        return withOrcidSynchronizationFundingsPreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationFundingsPreference(String value) {
        return setMetadataSingleValue(item, "cris", "orcid", "sync-fundings", value);
    }

    public ItemBuilder withOrcidSynchronizationProfilePreference(OrcidProfileSyncPreference value) {
        return withOrcidSynchronizationProfilePreference(value.name());
    }

    public ItemBuilder withOrcidSynchronizationProfilePreference(String value) {
        return addMetadataValue(item, "cris", "orcid", "sync-profile", value);
    }

    public ItemBuilder withOrcidSynchronizationMode(OrcidSynchronizationMode mode) {
        return withOrcidSynchronizationMode(mode.name());
    }

    private ItemBuilder withOrcidSynchronizationMode(String mode) {
        return setMetadataSingleValue(item, "cris", "orcid", "sync-mode", mode);
    }

    public ItemBuilder withAuthorOrcid(String orcid) {
        return addMetadataValue(item, "perucris", "author", "orcid", orcid);
    }

    public ItemBuilder withEditorOrcid(String editorOrcid) {
        return addMetadataValue(item, "perucris", "editor", "orcid", editorOrcid);
    }

    public ItemBuilder withIsniIdentifier(String isni) {
        return addMetadataValue(item, "person", "identifier", "isni", isni);
    }

    public ItemBuilder withResearcherIdentifier(String rid) {
        return addMetadataValue(item, "person", "identifier", "rid", rid);
    }

    public ItemBuilder withScopusAuthorIdentifier(String id) {
        return addMetadataValue(item, "person", "identifier", "scopus-author-id", id);
    }

    public ItemBuilder withPatentNo(String patentNo) {
        return addMetadataValue(item, "dc", "identifier", "patentno", patentNo);
    }

    public ItemBuilder withCitationIdentifier(String citation) {
        return addMetadataValue(item, "dc", "identifier", "citation", citation);
    }

    public ItemBuilder withFullName(String fullname) {
        return setMetadataSingleValue(item, "crisrp", "name", null, fullname);
    }

    public ItemBuilder withVernacularName(String vernacularName) {
        return setMetadataSingleValue(item, "crisrp", "name", "translated", vernacularName);
    }

    public ItemBuilder withVariantName(String variant) {
        return addMetadataValue(item, "crisrp", "name", "variant", variant);
    }

    public ItemBuilder withGivenName(String givenName) {
        return setMetadataSingleValue(item, "person", "givenName", null, givenName);
    }

    public ItemBuilder withFamilyName(String familyName) {
        return setMetadataSingleValue(item, "person", "familyName", null, familyName);
    }

    public ItemBuilder withBirthDate(String birthDate) {
        return setMetadataSingleValue(item, "person", "birthDate", null, birthDate);
    }

    public ItemBuilder withGender(String gender) {
        return setMetadataSingleValue(item, "oairecerif", "person", "gender", gender);
    }

    public ItemBuilder withJobTitle(String jobTitle) {
        return setMetadataSingleValue(item, "person", "jobTitle", null, jobTitle);
    }

    public ItemBuilder withPersonMainAffiliation(String affiliation) {
        return addMetadataValue(item, "person", "affiliation", "name", affiliation);
    }

    public ItemBuilder withPersonMainAffiliation(final String affiliation, final String authority) {
        return addMetadataValue(item, "person", "affiliation", "name", null, affiliation, authority, 600);
    }

    public ItemBuilder withWorkingGroup(String workingGroup) {
        return addMetadataValue(item, "crisrp", "workgroup", null, workingGroup);
    }

    public ItemBuilder withPersonalSiteUrl(String url) {
        return addMetadataValue(item, "oairecerif", "identifier", "url", url);
    }

    public ItemBuilder withIdentifierUrl(String url) {
        return addMetadataValue(item, "dc", "identifier", "url", url);
    }

    public ItemBuilder withPersonalSiteTitle(String title) {
        return addMetadataValue(item, "crisrp", "site", "title", title);
    }

    public ItemBuilder withPersonEmail(String email) {
        return addMetadataValue(item, "person", "email", null, email);
    }

    public ItemBuilder withPersonMainAffiliationName(String name, String authority) {
        return addMetadataValue(item, "person", "affiliation", "name", null, name, authority, 600);
    }

    public ItemBuilder withPersonAffiliation(String affiliation) {
        return addMetadataValue(item, "oairecerif", "person", "affiliation", affiliation);
    }

    public ItemBuilder withPersonAffiliation(String affiliation, String authority) {
        return addMetadataValue(item, "oairecerif", "person", "affiliation", null, affiliation, authority, 600);
    }

    public ItemBuilder withPersonAffiliationStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "affiliation", "startDate", startDate);
    }

    public ItemBuilder withPersonAffiliationEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "affiliation", "endDate", endDate);
    }

    public ItemBuilder withPersonAffiliationRole(String role) {
        return addMetadataValue(item, "oairecerif", "affiliation", "role", role);
    }

    public ItemBuilder withDescription(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", null, description);
    }

    public ItemBuilder withDescriptionAbstract(String description) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "abstract", description);
    }

    public ItemBuilder withPersonEducation(String education) {
        return addMetadataValue(item, "crisrp", "education", null, education);
    }

    public ItemBuilder withPersonEducationStartDate(String startDate) {
        return addMetadataValue(item, "crisrp", "education", "start", startDate);
    }

    public ItemBuilder withPersonEducationEndDate(String endDate) {
        return addMetadataValue(item, "crisrp", "education", "end", endDate);
    }

    public ItemBuilder withPersonEducationRole(String role) {
        return addMetadataValue(item, "crisrp", "education", "role", role);
    }

    public ItemBuilder withPersonCountry(String country) {
        return addMetadataValue(item, "crisrp", "country", null, country);
    }

    public ItemBuilder withPersonQualification(String qualification) {
        return addMetadataValue(item, "crisrp", "qualification", null, qualification);
    }

    public ItemBuilder withPersonQualificationStartDate(String startDate) {
        return addMetadataValue(item, "crisrp", "qualification", "start", startDate);
    }

    public ItemBuilder withPersonQualificationEndDate(String endDate) {
        return addMetadataValue(item, "crisrp", "qualification", "end", endDate);
    }

    public ItemBuilder withPersonQualificationOrgUnit(String endDate) {
        return addMetadataValue(item, "crisrp", "qualification", "orgunit", endDate);
    }

    public ItemBuilder withPersonQualificationGroup(String group) {
        return addMetadataValue(item, "crisrp", "qualification", "group", group);
    }

    public ItemBuilder withPersonKnowsLanguages(String languages) {
        return addMetadataValue(item, "person", "knowsLanguage", null, languages);
    }

    public ItemBuilder withRelationProject(String project, String authority) {
        return addMetadataValue(item, DC.getName(), "relation", "project", null, project, authority, 600);
    }

    public ItemBuilder withRelationFunding(String funding, String authority) {
        return addMetadataValue(item, DC.getName(), "relation", "funding", null, funding, authority, 600);
    }

    public ItemBuilder withInternalId(String internalId) {
        return addMetadataValue(item, "oairecerif", "internalid", null, internalId);
    }

    public ItemBuilder withAcronym(String acronym) {
        return addMetadataValue(item, "oairecerif", "acronym", null, acronym);
    }

    public ItemBuilder withProjectStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "project", "startDate", startDate);
    }

    public ItemBuilder withProjectEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "project", "endDate", endDate);
    }

    public ItemBuilder withProjectStatus(String status) {
        return addMetadataValue(item, "oairecerif", "project", "status", status);
    }

    public ItemBuilder withProjectPartner(String partner) {
        return addMetadataValue(item, "crispj", "partnerou", null, partner);
    }

    public ItemBuilder withProjectOrganization(String organization) {
        return addMetadataValue(item, "crispj", "organization", null, organization);
    }

    public ItemBuilder withProjectInvestigator(String investigator) {
        return addMetadataValue(item, "crispj", "investigator", null, investigator);
    }

    public ItemBuilder withProjectInvestigator(String investigator, String authority) {
        return addMetadataValue(item, "crispj", "investigator", null, null, investigator, authority, 600);
    }

    public ItemBuilder withProjectCoinvestigators(String coinvestigators) {
        return addMetadataValue(item, "crispj", "coinvestigators", null, coinvestigators);
    }

    public ItemBuilder withProjectCoinvestigators(String coinvestigators, String authority) {
        return addMetadataValue(item, "crispj", "coinvestigators", null, null, coinvestigators, authority, 600);
    }

    public ItemBuilder withProjectCoordinator(String coordinator) {
        return addMetadataValue(item, "crispj", "coordinator", null, coordinator);
    }

    public ItemBuilder withProjectCoordinator(String coordinator, String authority) {
        return addMetadataValue(item, "crispj", "coordinator", null, null, coordinator, authority, 600);
    }

    public ItemBuilder withType(String type) {
        return addMetadataValue(item, "dc", "type", null, type);
    }

    public ItemBuilder withType(String type, String authority) {
        return addMetadataValue(item, "dc", "type", null, null, type, authority, 600);
    }

    public ItemBuilder withLanguage(String language) {
        return addMetadataValue(item, "dc", "language", "iso", language);
    }

    public ItemBuilder withFunder(String funder) {
        return addMetadataValue(item, "oairecerif", "funder", null, funder);
    }

    public ItemBuilder withFunder(String funder, String authority) {
        return addMetadataValue(item, "oairecerif", "funder", null, null, funder, authority, 600);
    }

    public ItemBuilder withFundingParent(String parent) {
        return addMetadataValue(item, "oairecerif", "fundingParent", null, parent);
    }

    public ItemBuilder withFundingParent(String parent, String authority) {
        return addMetadataValue(item, "oairecerif", "fundingParent", null, null, parent, authority, 600);
    }

    public ItemBuilder withPublisher(String publisher) {
        return addMetadataValue(item, "dc", "publisher", null, publisher);
    }

    public ItemBuilder withRelationPublication(String publication) {
        return addMetadataValue(item, "dc", "relation", "publication", publication);
    }

    public ItemBuilder withRelationDoi(String doi) {
        return addMetadataValue(item, "dc", "relation", "doi", doi);
    }

    public ItemBuilder withRelationIsbn(String isbn) {
        return addMetadataValue(item, "dc", "relation", "isbn", isbn);
    }

    public ItemBuilder withCoverageIsbn(String isbn) {
        return addMetadataValue(item, "dc", "coverage", "isbn", isbn);
    }

    public ItemBuilder withCoverageDoi(String doi) {
        return addMetadataValue(item, "dc", "coverage", "doi", doi);
    }

    public ItemBuilder withRelationProject(String project) {
        return addMetadataValue(item, "dc", "relation", "project", project);
    }

    public ItemBuilder withRelationGrantno(String grantno) {
        return addMetadataValue(item, "dc", "relation", "grantno", grantno);
    }

    public ItemBuilder withRelationFunding(String funding) {
        return addMetadataValue(item, "dc", "relation", "funding", funding);
    }

    public ItemBuilder withRelationConference(String conference) {
        return addMetadataValue(item, "dc", "relation", "conference", conference);
    }

    public ItemBuilder withRelationProduct(String dataset) {
        return addMetadataValue(item, "dc", "relation", "product", dataset);
    }

    public ItemBuilder withRelationEquipment(String equipment) {
        return addMetadataValue(item, "dc", "relation", "equipment", equipment);
    }

    public ItemBuilder withRelationEquipment(String equipment, String authority) {
        return addMetadataValue(item, "dc", "relation", "equipment", null, equipment, authority, 600);
    }

    public ItemBuilder withVolume(String volume) {
        return addMetadataValue(item, "oaire", "citation", "volume", volume);
    }

    public ItemBuilder withIssue(String issue) {
        return addMetadataValue(item, "oaire", "citation", "issue", issue);
    }

    public ItemBuilder withIsPartOf(String isPartOf) {
        return addMetadataValue(item, "dc", "relation", "ispartof", isPartOf);
    }

    public ItemBuilder withCitationStartPage(String startPage) {
        return addMetadataValue(item, "oaire", "citation", "startPage", startPage);
    }

    public ItemBuilder withCitationEndPage(String endPage) {
        return addMetadataValue(item, "oaire", "citation", "endPage", endPage);
    }

    public ItemBuilder withDNI(String dni) {
        return addMetadataValue(item, "perucris", "identifier", "dni", dni);
    }

    public ItemBuilder withPmidIdentifier(String pmid) {
        return addMetadataValue(item, "dc", "identifier", "pmid", pmid);
    }

    public ItemBuilder withAdvisorDni(String dni) {
        return addMetadataValue(item, "perucris", "advisor", "dni", dni);
    }

    public ItemBuilder withEditorDni(String dni) {
        return addMetadataValue(item, "perucris", "editor", "dni", dni);
    }

    public ItemBuilder withAdvisorOrcid(String orcid) {
        return addMetadataValue(item, "perucris", "advisor", "orcid", orcid);
    }

    public ItemBuilder withTituloProfesional(String tituloProfesional) {
        return addMetadataValue(item, "crisrp", "education", null, tituloProfesional);
    }

    public ItemBuilder withAbreviaturaTitulo(String abreviaturaTitulo) {
        return addMetadataValue(item, "crisrp", "education", "role", abreviaturaTitulo);
    }

    public ItemBuilder withUniversidad(String universidad) {
        return addMetadataValue(item, "perucris", "education", "grantor", universidad);
    }

    public ItemBuilder withOpenaireId(String openaireid) {
        return addMetadataValue(item, "crispj", "openaireid", null, openaireid);
    }

    public ItemBuilder withEmbargoEnd(String embargoEnd) {
        return addMetadataValue(item, "oairecerif", "access", "embargoEnd", embargoEnd);
    }

    public ItemBuilder withAccess(String access) {
        return addMetadataValue(item, "oairecerif", "access", null, access);
    }

    public ItemBuilder withPerucrisSubjectOCDE(String ocde) {
        return addMetadataValue(item, "perucris", "subject", "ocde", ocde);
    }

    public ItemBuilder withSubjectOCDE(String ocde) {
        return withPerucrisSubjectOCDE(ocde);
    }

    public ItemBuilder withTypeOCDE(String type) {
        return addMetadataValue(item, "perucris", "project", "typeOcde",
            type);
    }

    public ItemBuilder withSubjectLoc(String loc) {
        return addMetadataValue(item, "dc", "subject", "loc", loc);
    }

    public ItemBuilder withRightsUri(String uri) {
        return addMetadataValue(item, "dc", "rights", "uri", uri);
    }

    public ItemBuilder withVersion(String version) {
        return addMetadataValue(item, "oaire", "version", null, version);
    }

    public ItemBuilder withCoveragePublication(String coveragePublication) {
        return addMetadataValue(item, "dc", "coverage", "publication", coveragePublication);
    }

    public ItemBuilder withAdvisor(String advisor) {
        return addMetadataValue(item, "dc", "contributor", "advisor", advisor);
    }

    public ItemBuilder withRenatiDiscipline(String discipline) {
        return addMetadataValue(item, "renati", "discipline", null, discipline);
    }

    public ItemBuilder withRenatiType(String renatiType) {
        return addMetadataValue(item, "renati", "type", null, renatiType);
    }

    public ItemBuilder withRenatiLevel(String level) {
        return addMetadataValue(item, "renati", "level", null, level);
    }

    public ItemBuilder withRenatiJuror(String juror) {
        return addMetadataValue(item, "renati", "juror", null, juror);
    }

    public ItemBuilder makeUnDiscoverable() {
        item.setDiscoverable(false);
        return this;
    }

    public ItemBuilder withUrlIdentifier(String urlIdentifier) {
        return addMetadataValue(item, "oairecerif", "identifier", "url", urlIdentifier);
    }

    public ItemBuilder withOAMandate(String oamandate) {
        return addMetadataValue(item, "oairecerif", "oamandate", null, oamandate);
    }

    public ItemBuilder withOAMandateURL(String oamandateUrl) {
        return addMetadataValue(item, "oairecerif", "oamandate", "url", oamandateUrl);
    }

    public ItemBuilder withEquipmentOwnerOrgUnit(String ownerOrgUnit) {
        return addMetadataValue(item, "crisequipment", "ownerou", null, ownerOrgUnit);
    }

    public ItemBuilder withEquipmentOwnerPerson(String ownerPerson) {
        return addMetadataValue(item, "crisequipment", "ownerrp", null, ownerPerson);
    }

    public ItemBuilder withOrgUnitLegalName(String legalName) {
        return addMetadataValue(item, "organization", "legalName", null, legalName);
    }

    public ItemBuilder withOrgUnitLocality(String addressLocality) {
        return addMetadataValue(item, "organization", "address", "addressLocality", addressLocality);
    }

    public ItemBuilder withOrgUnitRinggoldIdentifier(String identifier) {
        return addMetadataValue(item, "organization", "identifier", "rin", identifier);
    }

    public ItemBuilder withOrgUnitCrossrefIdentifier(String crossrefid) {
        return addMetadataValue(item, "organization", "identifier", "crossrefid", crossrefid);
    }

    public ItemBuilder withParentOrganization(String parent) {
        return addMetadataValue(item, "organization", "parentOrganization", null, parent);
    }

    public ItemBuilder withParentOrganization(String parent, String authority) {
        return addMetadataValue(item, "organization", "parentOrganization", null, null, parent, authority, 600);
    }

    public ItemBuilder withOrgUnitIdentifier(String identifier) {
        return addMetadataValue(item, "organization", "identifier", null, identifier);
    }

    public ItemBuilder withFundingIdentifier(String identifier) {
        return addMetadataValue(item, "oairecerif", "funding", "identifier", identifier);
    }

    public ItemBuilder withFundingInvestigator(String investigator) {
        return addMetadataValue(item, "crisfund", "investigators", null, investigator);
    }

    public ItemBuilder withFundingInvestigator(String investigator, String authority) {
        return addMetadataValue(item, "crisfund", "investigators", null, null, investigator, authority, 600);
    }

    public ItemBuilder withFundingCoInvestigator(String investigator) {
        return addMetadataValue(item, "crisfund", "coinvestigators", null, investigator);
    }

    public ItemBuilder withFundingCoInvestigator(String investigator, String authority) {
        return addMetadataValue(item, "crisfund", "coinvestigators", null, null, investigator, authority, 600);
    }

    public ItemBuilder withAmount(String amount) {
        return addMetadataValue(item, "oairecerif", "amount", null, amount);
    }

    public ItemBuilder withAmountCurrency(String currency) {
        return addMetadataValue(item, "oairecerif", "amount", "currency", currency);
    }

    public ItemBuilder withExecutedAmount(String amount) {
        return addMetadataValue(item, "perucris", "executedAmount", null, amount);
    }

    public ItemBuilder withExecutedAmountCurrency(String currency) {
        return addMetadataValue(item, "perucris", "executedAmount", "currency", currency);
    }

    public ItemBuilder withFundingStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "funding", "startDate", startDate);
    }

    public ItemBuilder withFundingEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "funding", "endDate", endDate);
    }

    public ItemBuilder withFundingAwardUrl(String url) {
        return addMetadataValue(item, "crisfund", "award", "url", url);
    }

    public ItemBuilder withCrisSourceId(String sourceId) {
        return addMetadataValue(item, "cris", "sourceId", null, sourceId);
    }

    public ItemBuilder withRightsHolder(String rightsHolder) {
        return addMetadataValue(item, "dcterms", "rightsHolder", null, rightsHolder);
    }

    public ItemBuilder withEventPlace(String place) {
        return addMetadataValue(item, "oairecerif", "event", "place", place);
    }

    public ItemBuilder withEventCountry(String country) {
        return addMetadataValue(item, "oairecerif", "event", "country", country);
    }

    public ItemBuilder withEventStartDate(String startDate) {
        return addMetadataValue(item, "oairecerif", "event", "startDate", startDate);
    }

    public ItemBuilder withEventEndDate(String endDate) {
        return addMetadataValue(item, "oairecerif", "event", "endDate", endDate);
    }

    public ItemBuilder withEventOrgUnitOrganizer(String organizer) {
        return addMetadataValue(item, "crisevent", "organizerou", null, organizer);
    }

    public ItemBuilder withEventProjectOrganizer(String organizer) {
        return addMetadataValue(item, "crisevent", "organizerpj", null, organizer);
    }

    public ItemBuilder withEventOrgUnitSponsor(String sponsor) {
        return addMetadataValue(item, "crisevent", "sponsorou", null, sponsor);
    }

    public ItemBuilder withEventProjectSponsor(String sponsor) {
        return addMetadataValue(item, "crisevent", "sponsorpj", null, sponsor);
    }

    public ItemBuilder withEventOrgUnitPartner(String partner) {
        return addMetadataValue(item, "crisevent", "partnerou", null, partner);
    }

    public ItemBuilder withEventProjectPartner(String partner) {
        return addMetadataValue(item, "crisevent", "partnerpj", null, partner);
    }

    public ItemBuilder withOrganizationRuc(String ruc) {
        return addMetadataValue(item, "organization", "identifier", "ruc", ruc);
    }

    public ItemBuilder withOrgUnitCountry(String addressCountry) {
        return addMetadataValue(item, "organization", "address", "addressCountry", addressCountry);
    }

    public ItemBuilder withOrgUnitType(String orgunitType) {
        return addMetadataValue(item, "perucris", "type", "orgunit", orgunitType);
    }

    public ItemBuilder withUsageType(String usage) {
        return addMetadataValue(item, "perucris", "type", "usage", usage);
    }

    public ItemBuilder withResearchLine(String researchLine) {
        return addMetadataValue(item, "perucris", "researchLine", null, researchLine);
    }

    public ItemBuilder withManufacturingCountry(String manufacturingCountry) {
        return addMetadataValue(item, "perucris", "manufacturingCountry", null, manufacturingCountry);
    }

    public ItemBuilder withManufacturingDate(String manufacturing) {
        return addMetadataValue(item, "perucris", "date", "manufacturing", manufacturing);
    }

    public ItemBuilder withAcquisitionDate(String acquisitionDate) {
        return addMetadataValue(item, "perucris", "date", "acquisition", acquisitionDate);
    }

    public ItemBuilder withInternalNote(String internalNote) {
        return addMetadataValue(item, "perucris", "description", "internalNote", internalNote);
    }

    public ItemBuilder withPhone(String phone) {
        return addMetadataValue(item, "perucris", "phone", null, phone);
    }

    public ItemBuilder withMobilePhone(String mobilePhone) {
        return addMetadataValue(item, "perucris", "mobilePhone", null, mobilePhone);
    }

    public ItemBuilder withDinaIdentifier(String dina) {
        return addMetadataValue(item, "perucris", "identifier", "dina", dina);
    }

    public ItemBuilder withDniIdentifier(String dni) {
        return addMetadataValue(item, "perucris", "identifier", "dni", dni);
    }

    public ItemBuilder withAuthorDniIdentifier(String dni) {
        return addMetadataValue(item, "perucris", "author", "dni", dni);
    }

    public ItemBuilder withPassport(String passport) {
        return addMetadataValue(item, "perucris", "identifier", "passport", passport);
    }

    public ItemBuilder withStreetAddress(String street) {
        return addMetadataValue(item, "perucris", "address", "streetAddress", street);
    }

    public ItemBuilder withCountryAddress(String country) {
        return addMetadataValue(item, "perucris", "address", "addressCountry", country);
    }

    public ItemBuilder withPostalCode(String postalCode) {
        return addMetadataValue(item, "perucris", "address", "postalCode", postalCode);
    }

    public ItemBuilder withOrgUnitDirector(String director) {
        return addMetadataValue(item, "crisou", "director", null, director);
    }

    public ItemBuilder withOrgUnitFoundingDate(String foundingDate) {
        return addMetadataValue(item, "organization", "foundingDate", null, foundingDate);
    }

    public ItemBuilder withOrgUnitBoard(String boards) {
        return addMetadataValue(item, "crisou", "boards", null, boards);
    }

    public ItemBuilder withOrgUnitIsniIdentifier(String isni) {
        return addMetadataValue(item, "organization", "identifier", "isni", isni);
    }

    public ItemBuilder withOrgUnitRucIdentifier(String ruc) {
        return addMetadataValue(item, "organization", "identifier", "ruc", ruc);
    }

    public ItemBuilder withOrgUnitRinIdentifier(String rin) {
        return addMetadataValue(item, "organization", "identifier", "rin", rin);
    }

    public ItemBuilder withOrgUnitRorIdentifier(String ror) {
        return addMetadataValue(item, "organization", "identifier", "ror", ror);
    }

    public ItemBuilder withOrgUnitScopusAffiliationIdentifier(String scopusaffid) {
        return addMetadataValue(item, "organization", "identifier", "scopusaffid", scopusaffid);
    }

    public ItemBuilder withOrgUnitCrossRefFunderIdentifier(String crossrefid) {
        return addMetadataValue(item, "organization", "identifier", "crossrefid", crossrefid);
    }

    public ItemBuilder withOrgUnitAddressLocality(String addressLocality) {
        return addMetadataValue(item, "organization", "address", "addressLocality", addressLocality);
    }

    public ItemBuilder withOrgUnitAddressCountry(String addressCountry) {
        return addMetadataValue(item, "organization", "address", "addressCountry", addressCountry);
    }

    public ItemBuilder withUbigeo(String ubigeo) {
        return addMetadataValue(item, "perucris", "ubigeo", null, ubigeo);
    }

    public ItemBuilder withGeoLocationPlace(String geoLocationPlace) {
        return addMetadataValue(item, "datacite", "geoLocationPlace", null, geoLocationPlace);
    }

    public ItemBuilder withIndustrialClassification(String industrialClassification) {
        return addMetadataValue(item, "perucris", "type", "ciiu", industrialClassification);
    }

    public ItemBuilder withProjectTechniciansAndEquivalentStaff(String techniciansAndEquivalentStaff) {
        return addMetadataValue(item, "crispj", "techniciansAndEquivalentStaff", null, techniciansAndEquivalentStaff);
    }

    public ItemBuilder withProjectSupportingStaff(String supportingStaff) {
        return addMetadataValue(item, "crispj", "supportingStaff", null, supportingStaff);
    }

    public ItemBuilder withCvPublicationSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPublication", "syncEnabled", enabled + "");
    }

    public ItemBuilder withCvProjectSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvProject", "syncEnabled", enabled + "");
    }

    public ItemBuilder withCvPatentSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPatent", "syncEnabled", enabled + "");
    }

    public ItemBuilder withCvPersonBasicInfoSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPerson", "syncBasicInfo", enabled + "");
    }

    public ItemBuilder withCvPersonEducationSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPerson", "syncEducation", enabled + "");
    }

    public ItemBuilder withCvPersonAffiliationSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPerson", "syncAffiliation", enabled + "");
    }

    public ItemBuilder withCvPersonQualificationSyncEnabled(boolean enabled) {
        return setMetadataSingleValue(item, "perucris", "cvPerson", "syncQualification", enabled + "");
    }

    public ItemBuilder withDateAccepted(String dateAccepted) {
        return addMetadataValue(item, "dcterms", "dateAccepted", null, dateAccepted);
    }

    public ItemBuilder withDateSubmitted(String dateSubmitted) {
        return addMetadataValue(item, "dcterms", "dateSubmitted", null, dateSubmitted);
    }

    public ItemBuilder withHandle(String handle) {
        this.handle = handle;
        return this;
    }

    public ItemBuilder withNotificationTo(String notification, String authority) {
        return addMetadataValue(item, "perucris", "notification", "to", null, notification, authority, 600);
    }

    public ItemBuilder withRelationPatent(String patent) {
        return addMetadataValue(item, "dc", "relation", "patent", patent);
    }

    public ItemBuilder withDescriptionVersion(String version) {
        return addMetadataValue(item, MetadataSchemaEnum.DC.getName(), "description", "version", version);
    }

    public ItemBuilder withPatentCountry(String country) {
        return addMetadataValue(item, "oairecerif", "patent", "country", country);
    }

    public ItemBuilder withDegreeGrantor(String grantor) {
        return addMetadataValue(item, "thesis", "degree", "grantor", grantor);
    }

    public ItemBuilder withDegreeName(String dgreeName) {
        return addMetadataValue(item, "thesis", "degree", "name", dgreeName);
    }

    public ItemBuilder withDegreeDiscipline(String discipline) {
        return addMetadataValue(item, "thesis", "degree", "discipline", discipline);
    }

    public ItemBuilder withDDC(String dcc) {
        return addMetadataValue(item, "dc", "subject", "ddc", dcc);
    }

    public ItemBuilder withRelationIspartofseries(String ispartofseries) {
        return addMetadataValue(item, "dc", "relation", "ispartofseries", ispartofseries);
    }

    public ItemBuilder withDescriptionSponsorship(String description) {
        return addMetadataValue(item, "dc", "description", "sponsorship", description);
    }

    public ItemBuilder withSubjectMesh(String mesh) {
        return addMetadataValue(item, "dc", "subject", "mesh", mesh);
    }

    public ItemBuilder withRegistrationNumber(String registrationNumber) {
        return addMetadataValue(item, "perucris", "renacyt", "registrationNumber", registrationNumber);
    }

    public ItemBuilder withRegistration(String registration) {
        return addMetadataValue(item, "perucris", "renacyt", "registration", registration);
    }

    public ItemBuilder withDateOfQualification(String dateOfQualification) {
        return addMetadataValue(item, "perucris", "renacyt", "dateOfQualification",
                                       new DCDate(dateOfQualification).toString());
    }

    public ItemBuilder withStiActions(String actions) {
        return addMetadataValue(item, "perucris", "sti","actions", actions);
    }

    public ItemBuilder withRenacytOcde(String ocde) {
        return addMetadataValue(item, "perucris", "renacyt", "ocde", ocde);
    }

    public ItemBuilder withRenacytStrength(String strength) {
        return addMetadataValue(item, "perucris", "renacyt", "strength", strength);
    }

    public ItemBuilder withFormalUnit(String formalUnit) {
        return addMetadataValue(item, "perucris", "sti", "formalUnit", formalUnit);
    }

    public ItemBuilder withSectorInstitucional(String sectorInstitucional) {
        return addMetadataValue(item, "perucris", "type", "sectorInstitucional", sectorInstitucional);
    }

    public ItemBuilder withEducacionSuperior(String educacionSuperior) {
        return addMetadataValue(item, "perucris", "type", "educacionSuperior", educacionSuperior);
    }

    public ItemBuilder withTypeNaturaleza(String naturaleza) {
        return addMetadataValue(item, "perucris", "type", "naturaleza", naturaleza);
    }

    public ItemBuilder withValidityOfRegistration(String validityOfRegistration) {
        return addMetadataValue(item, "perucris", "renacyt", "validityOfRegistration", validityOfRegistration);
    }

    public ItemBuilder withRenacytClassification(String classification) {
        return addMetadataValue(item, "perucris", "renacyt", "classification", classification);
    }

    public ItemBuilder withContractorou(final String contractorou, final String authority) {
        return addMetadataValue(item, "crispj", "contractorou", null, null, contractorou, authority, 600);
    }

    public ItemBuilder withPartnerou(final String partnerou, final String authority) {
        return addMetadataValue(item, "crispj", "partnerou", null, null, partnerou, authority, 600);
    }

    public ItemBuilder withInKindContributorou(final String inKindContributorou, final String authority) {
        return addMetadataValue(item, "crispj", "inKindContributorou", null, null, inKindContributorou, authority, 600);
    }

    public ItemBuilder withOrganization(final String organization, final String authority) {
        return addMetadataValue(item, "crispj", "organization", null, null, organization, authority, 600);
    }

    public ItemBuilder withOwnerou(final String ownerou, final String authority) {
        return addMetadataValue(item, "crisequipment", "ownerou", null, null, ownerou, authority, 600);
    }

    /**
     * Withdrawn the item under build. Please note that an user need to be loggedin the context to avoid NPE during the
     * creation of the provenance metadata
     *
     * @return the ItemBuilder
     */
    public ItemBuilder withdrawn() {
        withdrawn = true;
        return this;
    }

    public ItemBuilder inArchive() {
        inArchive = true;
        return this;
    }

    public ItemBuilder withEmbargoPeriod(String embargoPeriod) {
        return setEmbargo(embargoPeriod, item);
    }

    public ItemBuilder withReaderGroup(Group group) {
        readerGroup = group;
        return this;
    }

    /**
     * Create an admin group for the collection with the specified members
     *
     * @param ePerson eperson to add to the admin group
     * @return this builder
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ItemBuilder withAdminUser(EPerson ePerson) throws SQLException, AuthorizeException {
        return setAdminPermission(item, ePerson, null);
    }

    public ItemBuilder grantLicense() {
        String license;
        try {
            EPerson submitter = workspaceItem.getSubmitter();
            submitter = context.reloadEntity(submitter);
            license = getLicenseText(context.getCurrentLocale(), workspaceItem.getCollection(), item, submitter);
            LicenseUtils.grantLicense(context, item, license, null);
        } catch (Exception e) {
            handleException(e);
        }
        return this;
    }

    public ItemBuilder withFulltext(String name, String source, InputStream is) {
        try {
            Bitstream b = itemService.createSingleBitstream(context, is, item);
            b.setName(context, name);
            b.setSource(context, source);
        } catch (Exception e) {
            handleException(e);
        }
        return this;
    }


    @Override
    public Item build() {
        try {
            installItemService.installItem(context, workspaceItem, this.handle);
            itemService.update(context, item);
            //Check if we need to make this item private. This has to be done after item install.
            if (readerGroup != null) {
                setOnlyReadPermission(workspaceItem.getItem(), readerGroup, null);
            }

            if (withdrawn) {
                itemService.withdraw(context, item);
            }
            if (inArchive) {
                item.setArchived(inArchive);
            }
            context.dispatchEvents();
            indexingService.commit();
            return item;
        } catch (Exception e) {
            return handleException(e);
        }
    }


    public Item buildWithLastModifiedDate(Date lastModifiedDate) {
        try {
            installItemService.installItem(context, workspaceItem, this.handle);
            itemService.updateLastModifiedDate(context, item, lastModifiedDate);
            //Check if we need to make this item private. This has to be done after item install.
            if (readerGroup != null) {
                setOnlyReadPermission(workspaceItem.getItem(), readerGroup, null);
            }

            if (withdrawn) {
                itemService.withdraw(context, item);
            }
            if (inArchive) {
                item.setArchived(inArchive);
            }
            context.dispatchEvents();
            indexingService.commit();
            return item;
        } catch (Exception e) {
            return handleException(e);
        }
    }
    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            item = c.reloadEntity(item);
            if (item != null) {
                delete(c, item);
                c.complete();
            }
        }
    }

    @Override
    protected DSpaceObjectService<Item> getService() {
        return itemService;
    }

    /**
     * Delete the Test Item referred to by the given UUID
     * @param uuid UUID of Test Item to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteItem(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Item item = itemService.find(c, uuid);
            if (item != null) {
                try {
                    itemService.delete(c, item);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }


}
