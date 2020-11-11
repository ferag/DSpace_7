/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.perucris.externalservices.renacyt.UpdateItemWithInformationFromRenacytService;
import org.dspace.perucris.externalservices.reniec.UpdateItemWithInformationFromReniecService;
import org.dspace.perucris.externalservices.sunat.UpdateItemWithInformationFromSunatService;
import org.dspace.perucris.externalservices.sunedu.UpdateItemWithInformationFromSuneduService;
import org.dspace.scripts.DSpaceRunnable;
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
        this.collectionUuid = UUIDUtils.fromString(commandLine.getOptionValue('i'));
        this.service = commandLine.getOptionValue('s');
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
        try {
            performUpdate(context, externalService, service);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    private void performUpdate(Context context, PeruExternalService externalService, String service) {
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems(context, service);
            handler.logInfo("Update start");
            int countFoundItems = 0;
            int countUpdatedItems = 0;
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                countFoundItems++;
                final boolean updated = externalService.updateItem(context, item);
                if (updated) {
                    countUpdatedItems++;
                }
                count++;
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

    private void setFilter(DiscoverQuery discoverQuery, String service) {
        if ("reniec".equals(service) || "sunedu".equals(service) || "renacyt".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:Person");
            discoverQuery.addFilterQueries("perucris.identifier.dni:*");
        }
        if ("sunat".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:OrgUnit");
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
