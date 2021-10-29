/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.harvest.service.HarvestSchedulingService;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the scheduling of harvesting tasks.
 * This class is responsible for all business logic calls for the harvesting tasks and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestSchedulingServiceImpl implements HarvestSchedulingService {

    /* The main harvesting thread */
    protected HarvestScheduler harvester;
    protected Thread mainHarvestThread;
    protected HarvestScheduler harvestScheduler;
    protected boolean isLocal;
    protected EPerson eperson;
    @Autowired(required = true)
    protected HarvestedCollectionService harvestedCollectionService;

    protected HarvestSchedulingServiceImpl() {

    }

    @Override
    public synchronized void startNewScheduler(boolean isLocal, EPerson eperson)
        throws SQLException, AuthorizeException {
        this.isLocal = isLocal;
        this.eperson = eperson;
        Context c = new Context();
        harvestedCollectionService.exists(c);
        c.complete();

        if (mainHarvestThread != null && harvester != null) {
            stopScheduler();
        }
        harvester = new HarvestScheduler(isLocal, eperson);
        HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_NONE);
        mainHarvestThread = new Thread(harvester);
        mainHarvestThread.start();
    }

    @Override
    public synchronized void stopScheduler() throws SQLException, AuthorizeException {
        synchronized (HarvestScheduler.lock) {
            HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_STOP);
            HarvestScheduler.lock.notify();
        }
        mainHarvestThread = null;
        harvester = null;
    }

    @Override
    public void pauseScheduler() throws SQLException, AuthorizeException {
        synchronized (HarvestScheduler.lock) {
            HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_PAUSE);
            HarvestScheduler.lock.notify();
        }
    }

    @Override
    public void resumeScheduler() throws SQLException, AuthorizeException {
        HarvestScheduler.setInterrupt(HarvestScheduler.HARVESTER_INTERRUPT_RESUME);
    }

    @Override
    public void resetScheduler() throws SQLException, AuthorizeException, IOException {
        Context context = new Context();
        List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
        for (HarvestedCollection hc : harvestedCollections) {
            hc.setHarvestStartTime(null);
            hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
            harvestedCollectionService.update(context, hc);
        }
    }
}
