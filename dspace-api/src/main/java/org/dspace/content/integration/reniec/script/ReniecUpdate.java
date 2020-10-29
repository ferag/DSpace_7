/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.reniec.script;

import java.sql.SQLException;
import java.util.Iterator;
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
import org.dspace.reniec.UpdateItemWithInformationFromReniecService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DSpaceRunnable}
 *
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ReniecUpdate extends DSpaceRunnable<ReniecUpdateScriptConfiguration<ReniecUpdate>> {

    private static Logger log = LogManager.getLogger(ReniecUpdate.class);

    public static int countFoundItems = 0;

    private UUID collectionUuid;

    private Context context;

    @Autowired
    private  UpdateItemWithInformationFromReniecService updateItemWithInformationFromReniecService;

    @Override
    public void setup() throws ParseException {
        this.updateItemWithInformationFromReniecService = new DSpace().getServiceManager().getServiceByName(
                UpdateItemWithInformationFromReniecService.class.getName(),
                UpdateItemWithInformationFromReniecService.class);
        this.collectionUuid = UUIDUtils.fromString(commandLine.getOptionValue('i'));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ReniecUpdateScriptConfiguration<ReniecUpdate> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("reniec-update",
                   ReniecUpdateScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();
        try {
            performReniecUpdate(context);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    public void performReniecUpdate(Context context) {
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems(context);
            log.info("Reniec update start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                countFoundItems++;
                updateItemWithInformationFromReniecService.updateItem(context, item);
                count++;
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            log.info("Found " + countFoundItems + " items");
            log.info("Updated " + UpdateItemWithInformationFromReniecService.countItemUpdated + " items");
            log.info("Reniec update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Iterator<Item> findItems(Context context)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries("relationship.type:Person");
        discoverQuery.addFilterQueries("perucris.identifier.dni:*");
        if (this.collectionUuid != null) {
            discoverQuery.addFilterQueries("location.coll:" + this.collectionUuid.toString());
        }
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
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
}