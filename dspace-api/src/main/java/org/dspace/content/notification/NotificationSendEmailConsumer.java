/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.notification;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Consumer that takes care of send Notification via email.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class NotificationSendEmailConsumer implements Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSendEmailConsumer.class);

    private Set<UUID> itemsAlreadyProcessed = new HashSet<>();

    private ItemService itemService;

    private EPersonService ePersonService;

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (itemsAlreadyProcessed == null) {
            itemsAlreadyProcessed = new HashSet<UUID>();
        }
        if (event.getEventType() != Event.INSTALL) {
            return;
        }
        DSpaceObject dso = event.getSubject(context);
        if ((dso instanceof Item)) {
            Item item = (Item) dso;
            if (item.isArchived() && isNotificationEntityItem(item) && !itemsAlreadyProcessed.contains(item.getID())) {
                context.turnOffAuthorisationSystem();
                List<MetadataValue> list = itemService.getMetadataByMetadataString(item, "perucris.notification.to");
                try {
                    if (!list.isEmpty()) {
                        for (MetadataValue mv : list) {
                            sendEmail(context, item, mv);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while sending notification : {}", e.getMessage(), e);
                }  finally {
                    itemsAlreadyProcessed.add(item.getID());
                    context.restoreAuthSystemState();
                }
            }

        }
    }

    private void sendEmail(Context context, Item item, MetadataValue metadataValue)
        throws SQLException, IOException, MessagingException {
        UUID cvPersonitemUuid = UUID.fromString(metadataValue.getAuthority());
        Item cvPersonItem = itemService.find(context, cvPersonitemUuid);
        List<MetadataValue> crisOwner = itemService.getMetadata(cvPersonItem, "cris", "owner", null, null);
        if (!crisOwner.isEmpty()) {
            UUID ePersonUuid = UUID.fromString(crisOwner.get(0).getAuthority());
            EPerson cvOwner = ePersonService.find(context, ePersonUuid);
            Locale supportedLocale = I18nUtil.getEPersonLocale(cvOwner);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "notification_email"));
            email.addArgument(itemService.getMetadataFirstValue(item, "perucris", "notification", "message",
                    Item.ANY));
            email.addRecipient(cvOwner.getEmail());
            email.addArgument(cvOwner);
            email.send();
        }
    }

    private boolean isNotificationEntityItem(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return StringUtils.equals(entityType, "Notification");
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

}