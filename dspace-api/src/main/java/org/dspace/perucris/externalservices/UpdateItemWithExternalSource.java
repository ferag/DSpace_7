/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.perucris.externalservices.renacyt.UpdateItemWithInformationFromRenacytService;
import org.dspace.perucris.externalservices.reniec.UpdateItemWithInformationFromReniecService;
import org.dspace.perucris.externalservices.sunat.UpdateItemWithInformationFromSunatService;
import org.dspace.perucris.externalservices.sunedu.UpdateItemWithInformationFromSuneduService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
import org.dspace.scripts.Process_;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update items with external service as RENIEC, SUNEDU
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemWithExternalSource
        extends DSpaceRunnable<UpdateItemWithExternalSourceScriptConfiguration<UpdateItemWithExternalSource>> {

    private static final Logger log = LogManager.getLogger(UpdateItemWithExternalSource.class);

    private UUID collectionUuid;

    private Context context;

    private String service;

    private Map<String, PeruExternalService> peruExternalService = new HashMap<String, PeruExternalService>();

    private ProcessService processService;

    private Boolean lastCompleted;

    private Integer limit;

    private UUID singleUuid;

    @Override
    public void setup() throws ParseException {
        peruExternalService.put("reniec", new DSpace().getServiceManager().getServiceByName(
                                              UpdateItemWithInformationFromReniecService.class.getName(),
                                              UpdateItemWithInformationFromReniecService.class));
        peruExternalService.put("sunedu", new DSpace().getServiceManager().getServiceByName(
                                              UpdateItemWithInformationFromSuneduService.class.getName(),
                                              UpdateItemWithInformationFromSuneduService.class));
        peruExternalService.put("renacyt", new DSpace().getServiceManager().getServiceByName(
                                               UpdateItemWithInformationFromRenacytService.class.getName(),
                                               UpdateItemWithInformationFromRenacytService.class));
        peruExternalService.put("sunat", new DSpace().getServiceManager().getServiceByName(
                                             UpdateItemWithInformationFromSunatService.class.getName(),
                                             UpdateItemWithInformationFromSunatService.class));
        processService = ScriptServiceFactory.getInstance().getProcessService();
        this.collectionUuid = UUIDUtils.fromString(commandLine.getOptionValue('i'));
        this.service = commandLine.getOptionValue('s');
        this.lastCompleted = Boolean.valueOf(commandLine.getOptionValue('b'));
        this.singleUuid = UUIDUtils.fromString(commandLine.getOptionValue('u'));
        String strLimit = commandLine.getOptionValue('l');
        this.limit = StringUtils.isNotBlank(strLimit) ?  Integer.valueOf(strLimit) : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateItemWithExternalSourceScriptConfiguration<UpdateItemWithExternalSource> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-from-supplier",
                   UpdateItemWithExternalSourceScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();

        if (service == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }
        PeruExternalService externalService = peruExternalService.get(this.service.toLowerCase());
        if (externalService == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }
        if (invalidSingleUuid()) {
            throw new IllegalArgumentException("The itemUuid specified is not valid");
        }

        if (Objects.nonNull(this.singleUuid)) {
            this.limit = 0;
            this.lastCompleted = false;
        }
        try {
            context.turnOffAuthorisationSystem();
            performUpdate(context, externalService, service);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private boolean invalidSingleUuid() {
        if (StringUtils.isBlank(commandLine.getOptionValue("u"))) {
            return false;
        }
        return Objects.isNull(this.singleUuid);
    }

    private void performUpdate(Context context, PeruExternalService externalService, String service) {
        int count = 0;
        boolean checkLimit = false;
        try {
            Iterator<Item> itemIterator = findItems(context, service);
            handler.logInfo("Update start");
            int countFoundItems = 0;
            int countUpdatedItems = 0;
            while (itemIterator.hasNext() && !checkLimit) {
                Item item = itemIterator.next();
                countFoundItems++;
                final Item itemToUpdate = context.reloadEntity(item);
                final boolean updated = externalService.updateItem(context, item);
                context.uncacheEntity(itemToUpdate);
                if (updated) {
                    countUpdatedItems++;
                }
                count++;
                if (this.limit == count) {
                    checkLimit = true;
                }
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            handler.logInfo("Found " + countFoundItems + " items");
            handler.logInfo("Updated " + countUpdatedItems + " items");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Iterator<Item> findItems(Context context, String service)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        setFilter(discoverQuery, service);
        if (this.collectionUuid != null) {
            discoverQuery.addFilterQueries("location.coll:" + this.collectionUuid.toString());
        }
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private String getDateOfLastCompletedProcess() throws SQLException {
        ProcessQueryParameterContainer processQueryParameterContainer = createProcessQueryParameterContainer(
                                       "update-from-supplier", null, ProcessStatus.COMPLETED);
        List<Process> processes = processService.search(context, processQueryParameterContainer, 100, 0);
        for (Process process : processes) {
            if (isLastUpdated(process)) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                return simpleDateFormat.format(process.getStartTime());
            }
        }
        return StringUtils.EMPTY;
    }

    private boolean isLastUpdated(Process process) {
        for (DSpaceCommandLineParameter command : processService.getParameters(process)) {
            if (StringUtils.equalsIgnoreCase(command.getValue(), this.service)) {
                return true;
            }
        }
        return false;
    }

    private ProcessQueryParameterContainer createProcessQueryParameterContainer(String scriptName, EPerson ePerson,
            ProcessStatus processStatus) {
        ProcessQueryParameterContainer processQueryParameterContainer = new ProcessQueryParameterContainer();
        if (StringUtils.isNotBlank(scriptName)) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.NAME, scriptName);
        }
        if (ePerson != null) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.E_PERSON, ePerson);
        }
        if (processStatus != null) {
            processQueryParameterContainer.addToQueryParameterMap(Process_.PROCESS_STATUS, processStatus);
        }
        return processQueryParameterContainer;
    }

    private void setFilter(DiscoverQuery discoverQuery, String service) throws SQLException {
        String date = null;
        if (lastCompleted.booleanValue()) {
            date = getDateOfLastCompletedProcess();
        }
        if (StringUtils.isNotBlank(date)) {
            discoverQuery.addFilterQueries("lastModified:[" + date + " TO *]");
        }
        if (Objects.nonNull(this.singleUuid)) {
            discoverQuery.setQuery(SearchUtils.RESOURCE_UNIQUE_ID + " : Item-" + singleUuid.toString());
        }
        if ("reniec".equals(service) || "sunedu".equals(service) || "renacyt".equals(service)) {
            discoverQuery.addFilterQueries("dspace.entity.type:Person");
            discoverQuery.addFilterQueries("perucris.identifier.dni:*");
        }
        if ("sunat".equals(service)) {
            discoverQuery.addFilterQueries("dspace.entity.type:OrgUnit");
            discoverQuery.addFilterQueries("organization.identifier.ruc:*");
        }
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    public UUID getCollectionUuid() {
        return collectionUuid;
    }

    public void setCollectionUuid(UUID collectionUuid) {
        this.collectionUuid = collectionUuid;
    }

    public Map<String, PeruExternalService> getPeruExternalService() {
        return peruExternalService;
    }

    public void setPeruExternalService(Map<String, PeruExternalService> peruExternalService) {
        this.peruExternalService = peruExternalService;
    }
}
