/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.notification;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer that takes care of manage resourcepolicy for Notification items
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class NotificationConsumer implements Consumer {

    private ItemService itemService;

    private EPersonService ePersonService;

    private AuthorizeService authorizeService;

    private ResourcePolicyService resourcePolicyService;

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    /**
     * Initalise the consumer
     *
     * @throws Exception if error
     */
    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        DSpaceObject dso = event.getSubject(context);
        if ((dso instanceof Item)) {
            Item item = (Item) dso;
            if (item.isArchived() && isNotificationEntityItem(item) && !itemsAlreadyProcessed.contains(item)) {
                context.turnOffAuthorisationSystem();
                boolean isTheFirstRecipient = true;
                List<MetadataValue> list = itemService.getMetadataByMetadataString(item, "perucris.notification.to");
                if (!list.isEmpty()) {
                    for (MetadataValue metadataValue : list) {
                        UUID cvPersonitemUuid = UUID.fromString(metadataValue.getAuthority());
                        Item cvPersonItem = itemService.find(context, cvPersonitemUuid);
                        List<MetadataValue> crisOwner = itemService.getMetadata(cvPersonItem, "cris", "owner",
                                null, null);
                        if (!crisOwner.isEmpty()) {
                            UUID ePersonUuid = UUID.fromString(crisOwner.get(0).getAuthority());
                            EPerson cvOwner = this.ePersonService.find(context, ePersonUuid);
                            if (isTheFirstRecipient) {
                                resourcePolicyService.removeAllPolicies(context, item);
                                isTheFirstRecipient = false;
                            }
                            if (Objects.isNull(cvOwner)) {
                                log.warn("cvOwner not found for person {} with authority {}",
                                    crisOwner.get(0).getValue(),
                                    crisOwner.get(0).getAuthority());
                                continue;
                            }
                            itemService.addMetadata(context, item, "cris", "owner", null,
                                null, cvOwner.getFullName(), cvOwner.getID().toString(), Choices.CF_ACCEPTED);
                            authorizeService.addPolicy(context, item, Constants.READ, cvOwner);
                        }

                    }
                }
                itemsAlreadyProcessed.add(item);
                context.restoreAuthSystemState();
            }

        }
    }

    private boolean isNotificationEntityItem(Item item) {
        String entityType = this.itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        return StringUtils.equals(entityType, "Notification");
    }

    /**
     * Handle the end of the event
     *
     * @param context The relevant DSpace Context.
     * @throws Exception if error
     */
    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    /**
     * Finish the event
     *
     * @param context The relevant DSpace Context.
     */
    @Override
    public void finish(Context context) throws Exception {}

}