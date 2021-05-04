/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.LiveImportDataProvider;
import org.dspace.perucris.externalservices.CreateWorkspaceItemWithExternalSource;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
*
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
*/
public class CreateWorkspaceItemFromExternalServiceIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @SuppressWarnings("unused")
    private Item itemPersonA;
    @SuppressWarnings("unused")
    private Item itemPublication;
    @SuppressWarnings("unused")
    private Item itemPublication2;

    private XmlWorkflowItem witem;

    private XmlWorkflowItem witem2;


    private Collection col1;
    private Collection col2Scopus;
    private Collection col2WOS;
    private CreateWorkspaceItemWithExternalSource createWorkspaceItemService;
    private Map<String, LiveImportDataProvider> nameToProvider;
    private LiveImportDataProvider mockScopusProvider;
    private LiveImportDataProvider mockWosProvider;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        EntityType institutionPublicationType = createEntityType("InstitutionPublication");

        EntityType publicationType = createEntityType("Publication");

        createHasShadowCopyRelationship(institutionPublicationType, publicationType);
        createIsCorrectionOfRelationship(institutionPublicationType);
        createIsCorrectionOfRelationship(publicationType);

        EPerson submitter = createEPerson("submitter@example.com");
        EPerson firstDirectorioUser = createEPerson("firstDirectorioUser@example.com");
        EPerson secondDirectorioUser = createEPerson("secondDirectorioUser@example.com");
        EPerson institutionUser = createEPerson("user@example.com");

        Community directorioCommunity = CommunityBuilder.createCommunity(context)
            .withName("Directorio Community")
            .build();

        Community directorioSubCommunity = CommunityBuilder.createSubCommunity(context, directorioCommunity)
            .withName("Directorio de Produccion Cientifica")
            .build();

        Group directorioReviewGroup = GroupBuilder.createGroup(context)
            .withName("review group")
            .addMember(firstDirectorioUser)
            .addMember(secondDirectorioUser)
            .build();

        Group directorioEditorGroup = GroupBuilder.createGroup(context)
            .withName("editor group")
            .addMember(firstDirectorioUser)
            .addMember(secondDirectorioUser)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Group reviewGroup = GroupBuilder.createGroup(context)
            .withName("Reviewer group")
            .addMember(institutionUser)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        this.col1 = CollectionBuilder.createCollection(context, directorioCommunity)
                                     .withEntityType("Person")
                                     .withName("Collection 1").build();

        this.col2Scopus = createCollection(context, parentCommunity)
            .withName("Collection for new WorkspaceItems imported from Scopus")
            .withEntityType("InstitutionPublication")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        this.col2WOS = createCollection(context, parentCommunity)
            .withName("Collection for new WorkspaceItems imported from WOS")
            .withEntityType("InstitutionPublication")
            .withSubmissionDefinition("traditional")
            .withSubmitterGroup(submitter)
            .withRoleGroup("reviewer", reviewGroup)
            .build();

        configurationService.setProperty("directorios.community-id", directorioCommunity.getID());
        configurationService.setProperty("scopus.importworkspaceitem.collection-id", this.col2Scopus.getID());
        configurationService.setProperty("wos.importworkspaceitem.collection-id", this.col2WOS.getID());
        createWorkspaceItemService = new CreateWorkspaceItemWithExternalSource();
        nameToProvider = new HashMap<String, LiveImportDataProvider>();
        mockScopusProvider = Mockito.mock(LiveImportDataProvider.class);
        mockWosProvider = Mockito.mock(LiveImportDataProvider.class);
    }

    @After
    public void destroy() throws Exception {

        context.turnOffAuthorisationSystem();
        collectionRoleService.deleteByCollection(context, col2Scopus);
        collectionRoleService.deleteByCollection(context, col2WOS);
        workflowItemService.deleteByCollection(context, col2Scopus);
        workflowItemService.deleteByCollection(context, col2WOS);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();

        super.destroy();
    }

    @Test
    public void creatingWorkspaceItemImportedFromScopusTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withScopusAuthorIdentifier("55484808800")
                                      .build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null,
                                                  "Improvement of editorial quality of journals");
        MetadataValueDTO scopusAuthorId = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                               "55484808800");
        MetadataValueDTO doi = new MetadataValueDTO("dc", "identifier","doi", null, "10.4403/jlis.it-12052");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85019960269");
        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(doi);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(scopusAuthorId);
        metadataFirstRecord.add(scopus);
        metadataFirstRecord.add(type);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO doi2R = new MetadataValueDTO("dc", "identifier", "doi", null, "10.1016/j.procs.38");
        MetadataValueDTO scopusAuthorId2R = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                                 "55484808800");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus2R = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85020703703");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(doi2R);
        metadataSecondRecord.add(scopusAuthorId2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(scopus2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        when(mockScopusProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockScopusProvider.searchExternalDataObjects(ArgumentMatchers.any(),
                               ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "scopus", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("scopus", mockScopusProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections.traditionalpageone.['dc.title'][0].value",
                                  is(title.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.scopus'][0].value", is(scopus.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.doi'][0].value", is(doi.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0]._embedded"
                                   + ".item.metadata['perucris.author.scopus-author-id'][0].value",
                                     is(scopusAuthorId.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections.traditionalpageone['dc.title'][0].value",
                                     is(title2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.identifier.scopus'][0].value", is(scopus2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.identifier.doi'][0].value", is(doi2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1]._embedded"
                                   + ".item.metadata['perucris.author.scopus-author-id'][0].value",
                                     is(scopusAuthorId2R.getValue())))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void createOnlyOneWorkspaceItemImportedFromScopusTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withScopusAuthorIdentifier("55484808800")
                                      .build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null,
                                                  "Improvement of editorial quality of journals");
        MetadataValueDTO scopusAuthorId = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                               "55484808800");
        MetadataValueDTO doi = new MetadataValueDTO("dc", "identifier","doi", null, "10.4403/jlis.it-12052");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85019960269");
        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(doi);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(scopusAuthorId);
        metadataFirstRecord.add(scopus);
        metadataFirstRecord.add(type);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO doi2R = new MetadataValueDTO("dc", "identifier", "doi", null, "10.1016/j.procs.38");
        MetadataValueDTO scopusAuthorId2R = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                                 "55484808800");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus2R = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85020703703");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(doi2R);
        metadataSecondRecord.add(scopusAuthorId2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(scopus2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        when(mockScopusProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockScopusProvider.searchExternalDataObjects(ArgumentMatchers.any(),
                               ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        this.itemPublication = ItemBuilder.createItem(context, this.col2Scopus)
                                          .withScopusIdentifier(scopus.getValue())
                                          .withIssueDate(date.getValue())
                                          .withTitle(title.getValue()).build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "scopus", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("scopus", mockScopusProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections.traditionalpageone['dc.title'][0].value",
                                     is(title2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.scopus'][0].value", is(scopus2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.doi'][0].value", is(doi2R.getValue())))
                 .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void scopusAuthorIdentifierNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withScopusAuthorIdentifier("000").build();

        when(mockScopusProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(0);

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "scopus", "-e", admin.getEmail()};

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("scopus", mockScopusProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void creatingWorkspaceItemImportedFromWOSTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withOrcidIdentifier("0000-0002-9029-1854")
                                      .withResearcherIdentifier("123456789")
                                      .build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null, "Putting Historical Data in Context");
        MetadataValueDTO identifier = new MetadataValueDTO("dc", "identifier", "other", null, "WOS:000439929300064");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Book in series");
        MetadataValueDTO rid = new MetadataValueDTO("person", "identifier", "rid", null, "123456789");
        MetadataValueDTO orcid = new MetadataValueDTO("person", "identifier", "orcid", null, "0000-0002-9029-1854");

        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(type);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(identifier);
        metadataFirstRecord.add(rid);
        metadataFirstRecord.add(orcid);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO identifier2R = new MetadataValueDTO("dc", "identifier", "other", null, "WOS:000348252500018");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO description2R = new MetadataValueDTO("dc", "description", "abstract", null,
                                                              "In 2013, Directory of Open Access Journals (DOAJ)");
        MetadataValueDTO rid2R = new MetadataValueDTO("person", "identifier", "rid", null, "123456789");
        MetadataValueDTO orcid2R = new MetadataValueDTO("person", "identifier", "orcid", null, "0000-0002-9029-1854");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(identifier2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(description2R);
        metadataSecondRecord.add(rid2R);
        metadataSecondRecord.add(orcid2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        when(mockWosProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockWosProvider.searchExternalDataObjects(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                                       ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "wos", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("wos", mockWosProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections.traditionalpageone.['dc.title'][0].value",
                                  is(title.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.other'][0].value", is(identifier.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.date.issued'][0].value", is(date.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.type'][0].value", is(type.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0]._embedded"
                                   + ".item.metadata['perucris.author.orcid'][0].value",
                                  is(orcid.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0]._embedded"
                                   + ".item.metadata['perucris.author.rid'][0].value",
                                  is(rid.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections.traditionalpageone['dc.title'][0].value",
                                  is(title2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.identifier.other'][0].value",is(identifier2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.date.issued'][0].value", is(date2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.type'][0].value", is(type2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1]._embedded"
                                   + ".item.metadata['perucris.author.orcid'][0].value",
                                     is(orcid2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1]._embedded"
                                   + ".item.metadata['perucris.author.rid'][0].value",
                                     is(rid2R.getValue())))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void allItemsAlreadyExistImportFromWOSTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withOrcidIdentifier("0000-0002-9029-1854")
                                      .build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null, "Putting Historical Data in Context");
        MetadataValueDTO identifier = new MetadataValueDTO("dc", "identifier", "other", null, "WOS:000439929300064");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Book in series");

        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(type);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(identifier);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO identifier2R = new MetadataValueDTO("dc", "identifier", "other", null, "WOS:000348252500018");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO description2R = new MetadataValueDTO("dc", "description", "abstract", null,
                                                              "In 2013, Directory of Open Access Journals (DOAJ)");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(identifier2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(description2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        this.itemPublication = ItemBuilder.createItem(context, this.col2WOS)
                                          .withIdentifierOther(identifier.getValue())
                                          .withIssueDate(date.getValue())
                                          .withTitle(title.getValue()).build();

        this.itemPublication2 = ItemBuilder.createItem(context, this.col2WOS)
                                           .withIdentifierOther(identifier2R.getValue())
                                           .withIssueDate(date2R.getValue())
                                           .withTitle(title2R.getValue()).build();

        when(mockWosProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockWosProvider.searchExternalDataObjects(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                                       ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "wos", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("wos", mockWosProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void oneWorkflowItemAlreadyExistImportFromWOSTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withOrcidIdentifier("0000-0002-9029-1854")
                                      .build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null, "Putting Historical Data in Context");
        MetadataValueDTO identifier = new MetadataValueDTO("dc", "identifier", "isi", null, "WOS:000439929300064");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Book in series");

        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(type);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(identifier);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO identifier2R = new MetadataValueDTO("dc", "identifier", "isi", null, "WOS:000348252500018");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017");
        MetadataValueDTO description2R = new MetadataValueDTO("dc", "description", "abstract", null,
                                                              "In 2013, Directory of Open Access Journals (DOAJ)");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(identifier2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(description2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        this.witem = WorkflowItemBuilder.createWorkflowItem(context, this.col2WOS)
                .withTitle(title.getValue())
                .withIssueDate(date.getValue())
                .withIdentifierIsi(identifier.getValue()).build();

        when(mockWosProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockWosProvider.searchExternalDataObjects(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                                       ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "wos", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("wos", mockWosProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections.traditionalpageone.['dc.title'][0].value",
                                  is(title.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.identifier.isi'][0].value", is(identifier.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[0].sections"
                                   + ".traditionalpageone['dc.date.issued'][0].value", is(date.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections.traditionalpageone['dc.title'][0].value",
                                  is(title2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.identifier.isi'][0].value",is(identifier2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.date.issued'][0].value", is(date2R.getValue())))
                 .andExpect(jsonPath("$._embedded.workflowitems[1].sections"
                                   + ".traditionalpageone['dc.type'][0].value", is(type2R.getValue())))
                 .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void allWorkflowItemAlreadyExistImportFromScopusTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        this.itemPersonA = ItemBuilder.createItem(context, this.col1)
                                      .withPersonIdentifierFirstName("EDWIN")
                                      .withPersonIdentifierLastName("SAUCEDO")
                                      .withScopusAuthorIdentifier("55484808800").build();

        //define first record
        MetadataValueDTO title = new MetadataValueDTO("dc","title", null,null,
                                                  "Improvement of editorial quality of journals");
        MetadataValueDTO scopusAuthorId = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                               "55484808800");
        MetadataValueDTO doi = new MetadataValueDTO("dc", "identifier","doi", null, "10.4403/jlis.it-12052");
        MetadataValueDTO type = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85019960269");
        List<MetadataValueDTO> metadataFirstRecord = new ArrayList<MetadataValueDTO>();
        metadataFirstRecord.add(doi);
        metadataFirstRecord.add(title);
        metadataFirstRecord.add(date);
        metadataFirstRecord.add(scopusAuthorId);
        metadataFirstRecord.add(scopus);
        metadataFirstRecord.add(type);

        ExternalDataObject firstRecord = new ExternalDataObject();
        firstRecord.setMetadata(metadataFirstRecord);

        //define second record
        MetadataValueDTO title2R = new MetadataValueDTO("dc", "title", null, null, "Regional Portal FVG");
        MetadataValueDTO doi2R = new MetadataValueDTO("dc", "identifier", "doi", null, "10.1016/j.procs.38");
        MetadataValueDTO scopusAuthorId2R = new MetadataValueDTO("person", "identifier", "scopus-author-id", null,
                                                                 "55484808800");
        MetadataValueDTO type2R = new MetadataValueDTO("dc", "type", null, null, "Journal");
        MetadataValueDTO date2R = new MetadataValueDTO("dc", "date", "issued", null, "2017-01-01");
        MetadataValueDTO scopus2R = new MetadataValueDTO("dc", "identifier", "scopus", null, "2-s2.0-85020703703");

        List<MetadataValueDTO> metadataSecondRecord = new ArrayList<MetadataValueDTO>();
        metadataSecondRecord.add(title2R);
        metadataSecondRecord.add(doi2R);
        metadataSecondRecord.add(scopusAuthorId2R);
        metadataSecondRecord.add(type2R);
        metadataSecondRecord.add(date2R);
        metadataSecondRecord.add(scopus2R);

        ExternalDataObject secondRecord = new ExternalDataObject();
        secondRecord.setMetadata(metadataSecondRecord);

        List<ExternalDataObject> externalObjects = new ArrayList<ExternalDataObject>();
        externalObjects.add(firstRecord);
        externalObjects.add(secondRecord);

        when(mockScopusProvider.getNumberOfResults(ArgumentMatchers.any())).thenReturn(2);
        when(mockScopusProvider.searchExternalDataObjects(ArgumentMatchers.any(),ArgumentMatchers.anyInt(),
                                                          ArgumentMatchers.anyInt())).thenReturn(externalObjects);

        this.witem = WorkflowItemBuilder.createWorkflowItem(context, this.col2Scopus)
                                        .withTitle(title.getValue())
                                        .withIssueDate(date.getValue())
                                        .withScopusIdentifier(scopus.getValue()).build();

        this.witem2 = WorkflowItemBuilder.createWorkflowItem(context, this.col2Scopus)
                                         .withTitle(title2R.getValue())
                                         .withIssueDate(date2R.getValue())
                                         .withScopusIdentifier(scopus2R.getValue()).build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"import-publications", "-s", "scopus", "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        nameToProvider.put("scopus", mockScopusProvider);
        createWorkspaceItemService.initialize(args, handler, admin);
        createWorkspaceItemService.setNameToProvider(nameToProvider);
        createWorkspaceItemService.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.workflowitems", Matchers.containsInAnyOrder(
              WorkflowItemMatcher.matchItemWithTitleAndDateIssued(this.witem, title.getValue(), date.getValue()),
              WorkflowItemMatcher.matchItemWithTitleAndDateIssued(this.witem2, title2R.getValue(),date2R.getValue())
              )))
          .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType)
            .build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType institutionType, EntityType directorioType) {
        return createRelationshipTypeBuilder(context, institutionType, directorioType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsCorrectionOfRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType,
            entityType, "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1).build();
    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withPassword(password)
            .build();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }
}