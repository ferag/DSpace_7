/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.LiveImportDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.external.service.impl.ExternalDataServiceImpl;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Implementation of {@link DSpaceRunnable}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CreateWorkspaceItemWithExternalSource extends DSpaceRunnable<
       CreateWorkspaceItemWithExternalSourceScriptConfiguration<CreateWorkspaceItemWithExternalSource>> {

    private static final Logger log = LogManager.getLogger(CreateWorkspaceItemWithExternalSource.class);

    private static final int LIMIT = 10;

    private String service;

    private Context context;

    private ItemServiceImpl itemService;

    private CollectionServiceImpl collectionService;

    private ExternalDataService externalDataService;

    private UUID communityUUID;

    private Collection collection;

    private ConfigurationService configurationService;

    private Map<String, LiveImportDataProvider> nameToProvider = new HashMap<String, LiveImportDataProvider>();

    private WorkflowService workflowService;

    @Override
    public void setup() throws ParseException {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemService = serviceManager
                                  .getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        collectionService = serviceManager
                            .getServiceByName(CollectionServiceImpl.class.getName(),CollectionServiceImpl.class);
        externalDataService = serviceManager
                             .getServiceByName(ExternalDataServiceImpl.class.getName(), ExternalDataServiceImpl.class);
        nameToProvider.put("scopus", serviceManager.getServiceByName("scopusLiveImportDataProvider",
                                         LiveImportDataProvider.class));
        nameToProvider.put("wos", serviceManager.getServiceByName("wosLiveImportDataProvider",
                                      LiveImportDataProvider.class));
        workflowService = WorkflowServiceFactory.getInstance()
            .getWorkflowService();
        this.service = commandLine.getOptionValue('s');
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();
        if (service == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }

        LiveImportDataProvider dataProvider = nameToProvider.get(service);
        if (dataProvider == null) {
            throw new IllegalArgumentException("The " + this.service + " provider does not exist");
        }

        this.communityUUID = UUID.fromString(configurationService.getProperty("directorios.community-id"));
        if (Objects.isNull(this.communityUUID)) {
            throw new RuntimeException("The UUID of directorios.community-id is NULL.");
        }

        UUID collectionUUID = getCollectionUUID();
        if (Objects.isNull(collectionUUID)) {
            throw new RuntimeException("The UUID of target Collection is null.");
        }

        this.collection = collectionService.find(context, collectionUUID);
        if (Objects.isNull(collectionUUID)) {
            throw new RuntimeException("Collection with uuid" + collectionUUID + "does not exist!");
        }

        try {
            context.turnOffAuthorisationSystem();
            performCreatingOfWorkspaceItems(context, dataProvider);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private UUID getCollectionUUID() {
        switch (this.service) {
            case "scopus":
                return UUID.fromString(configurationService.getProperty("scopus.importworkspaceitem.collection-id"));
            case "wos":
                return UUID.fromString(configurationService.getProperty("wos.importworkspaceitem.collection-id"));
            default:
        }
        return null;
    }

    private void assignCurrentUserInContext() throws SQLException {

        context.setCurrentUser(findEPerson());
    }

    private EPerson findEPerson() throws SQLException {
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        String email = commandLine.getOptionValue('e');
        if (StringUtils.isNotBlank(email)) {
            log.info("looking up eperson with email: {}", email);
            EPerson byEmail = ePersonService.findByEmail(context, email);
            if (Objects.nonNull(byEmail)) {
                return byEmail;
            }
        }
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            return ePersonService.find(context, uuid);
        }
        return null;
    }

    private void performCreatingOfWorkspaceItems(Context context,LiveImportDataProvider dataProvider) {

        int totalRecordWorked = 0;
        int countItemsProcessed = 0;
        try {
            Iterator<Item> itemIterator = findItems(context);
            handler.logInfo("Update start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                String id = buildID(item);
                if (StringUtils.isNotBlank(id)) {
                    int currentRecord = 0;
                    int recordsFound = dataProvider.getNumberOfResults(id);
                    log.info("{} records found", recordsFound);
                    int userPublicationsProcessed = 0;
                    while (recordsFound == -1 || userPublicationsProcessed < recordsFound) {
                        userPublicationsProcessed += fillWorkspaceItems(context, currentRecord, dataProvider, item, id);
                        currentRecord += LIMIT;
                    }
                    totalRecordWorked += userPublicationsProcessed;
                }
                countItemsProcessed++;
                if (countItemsProcessed == 20) {
                    context.commit();
                    countItemsProcessed = 0;
                }
            }
            context.commit();
            handler.logInfo("Processed " + totalRecordWorked + " records");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String buildID(Item item) {
        StringBuilder id = new StringBuilder();
        switch (this.service) {
            case "scopus":
                String scopusId = itemService.getMetadataFirstValue(
                                  item, "person", "identifier", "scopus-author-id", Item.ANY);
                if (StringUtils.isNotBlank(scopusId)) {
                    id.append("AU-ID(").append(scopusId).append(")");
                }
                break;
            case "wos":
                String orcidId = itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
                String rid = itemService.getMetadataFirstValue(item, "person", "identifier", "rid", Item.ANY);
                if (StringUtils.isNotBlank(orcidId) && StringUtils.isNotBlank(rid)) {
                    id.append("AI=(").append(orcidId).append(" OR ").append(rid).append(")");
                } else if (StringUtils.isNotBlank(orcidId)) {
                    id.append("AI=(").append(orcidId).append(")");
                } else if (StringUtils.isNotBlank(rid)) {
                    id.append("AI=(").append(rid).append(")");
                }
                break;
            default:
        }
        return id.toString();
    }

    private int fillWorkspaceItems(Context context, int record, LiveImportDataProvider dataProvider,
            Item item, String id) throws SQLException {
        int countDataObjects = 0;
        try {
            for (ExternalDataObject dataObject : dataProvider.searchExternalDataObjects(id, record, LIMIT)) {
                if (!exist(dataObject.getMetadata())) {
                    log.info("item with id {} not yet in DSPace", id);
                    WorkspaceItem wsItem = externalDataService.createWorkspaceItemFromExternalDataObject(context,
                                                               dataObject, this.collection);
                    for (List<MetadataValueDTO> metadataList : metadataValueToAdd(wsItem.getItem())) {
                        addMetadata(wsItem.getItem(), metadataList);
                    }
                    workflowService.start(context, wsItem);
                } else {
                    log.info("item with id {} already in DSPace", id);
                }
                countDataObjects++;
                if (countDataObjects % 20 == 0) {
                    context.commit();
                }
            }
        } catch (AuthorizeException | IOException | WorkflowException e) {
            log.error(e.getMessage(), e);
        }
        return countDataObjects;
    }

    private void addMetadata(Item item, List<MetadataValueDTO> metadataList) throws SQLException {
        for (MetadataValueDTO metadataValue : metadataList) {
            itemService.addMetadata(context, item, metadataValue.getSchema(), metadataValue.getElement(),
                metadataValue.getQualifier(), null, metadataValue.getValue());
        }
    }

    private boolean exist(List<MetadataValueDTO> metadatas) throws SQLException {
        if (metadatas.size() == 0) {
            return false;
        }
        MetadataValueDTO metadata = getMetadaToChech();
        for (MetadataValueDTO mv : metadatas) {
            String schema = mv.getSchema();
            String element = mv.getElement();
            String qualifier = mv.getQualifier();
            if (StringUtils.equals(schema, metadata.getSchema()) && StringUtils.equals(element, metadata.getElement())
                    && StringUtils.equals(qualifier, metadata.getQualifier())) {

                String value = (mv.getValue()).replaceAll(":", "");
                StringBuilder filter = new StringBuilder();
                filter.append(metadata.getSchema()).append(".").append(metadata.getElement());
                if (StringUtils.isNotBlank(metadata.getQualifier())) {
                    filter.append(".").append(metadata.getQualifier()).append(":").append(value);
                } else {
                    filter.append(":").append(value);
                }
                try {
                    Iterator<Item> itemIterator = findItemsByCollection(context, filter.toString(), IndexableItem.TYPE);
                    if (itemIterator.hasNext()) {
                        return true;
                    }
                    Iterator<Item> wItemIterator = findItemsByCollection(context, filter.toString(),
                            IndexableWorkflowItem.TYPE);
                    if (wItemIterator.hasNext()) {
                        return true;
                    }
                } catch (SearchServiceException e) {
                    log.error(e.getMessage(), e);
                }
                return false;
            }
        }
        return false;
    }

    private MetadataValueDTO getMetadaToChech() {
        MetadataValueDTO metadata = new MetadataValueDTO();
        switch (this.service) {
            case "scopus":
                metadata.setSchema("dc");
                metadata.setElement("identifier");
                metadata.setQualifier("scopus");
                break;
            case "wos":
                metadata.setSchema("dc");
                metadata.setElement("identifier");
                metadata.setQualifier("isi");
                break;
            default:
        }
        return metadata;
    }

    private Iterator<Item> findItemsByCollection(Context context, String filter, String indexableObjType)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(indexableObjType);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries(filter);
        discoverQuery.addFilterQueries("location.coll:" + this.collection.getID());
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private Iterator<Item> findItems(Context context)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        setFilter(discoverQuery, this.service);
        discoverQuery.addFilterQueries("location.comm:" + this.communityUUID);
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void setFilter(DiscoverQuery discoverQuery, String service) {
        if ("scopus".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:Person");
            discoverQuery.addFilterQueries("person.identifier.scopus-author-id:*");
        }
        if ("wos".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:Person");
            discoverQuery.addFilterQueries("person.identifier.orcid:* OR person.identifier.rid:*");
        }
    }

    private List<List<MetadataValueDTO>> metadataValueToAdd(Item item) {
        switch (this.service) {
            case "scopus":
                return Collections.singletonList(metadataList(item, "scopus-author-id"));
            case "wos":
                return Arrays.asList(
                    metadataList(item, "orcid"),
                    metadataList(item, "rid")
                );
            default:
                return Collections.emptyList();
        }
    }

    private List<MetadataValueDTO> metadataList(Item item, String identifier) {
        return itemService.getMetadata(item, "person", "identifier", identifier, Item.ANY)
            .stream().sorted(Comparator.comparingInt(MetadataValue::getPlace))
            .map(md -> new MetadataValueDTO("perucris", "author", identifier, null,
                md.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CreateWorkspaceItemWithExternalSourceScriptConfiguration<CreateWorkspaceItemWithExternalSource>
        getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("import-publications",
                                                CreateWorkspaceItemWithExternalSourceScriptConfiguration.class);
    }

    public Map<String, LiveImportDataProvider> getNameToProvider() {
        return nameToProvider;
    }

    public void setNameToProvider(Map<String, LiveImportDataProvider> nameToProvider) {
        this.nameToProvider = nameToProvider;
    }
}