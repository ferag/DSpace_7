/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.consumer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * This consumer is used to decorating Projects and Equipments entity types
 * with metadata derived from their linked OrgUnits items.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class DecoratingConsumer implements Consumer {

    /**
     * This list of metadata is used to retrieve OrgUnits from which getting the decorative metadata
     */
    private static final List<String> METADATA_2_CHECK = Arrays.asList("crispj.contractorou", "crispj.partnerou",
                                                        "crispj.inKindContributorou", "crispj.organization",
                                                        "oairecerif.funder", "crisequipment.ownerou");

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemService itemService;

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }
        int eventType = event.getEventType();
        if (eventType == Event.MODIFY || eventType == Event.INSTALL || eventType == Event.MODIFY_METADATA) {
            Item item = (Item) event.getSubject(context);
            if (itemsAlreadyProcessed.contains(item)) {
                return;
            }
            itemsAlreadyProcessed.add(item);
            String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
            if (StringUtils.equalsAny(entityType, "Project", "Equipment")) {
                for (String metadata : METADATA_2_CHECK) {
                    List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadata);
                    for (MetadataValue mv : metadataValues) {
                        removeExistingMetadata(context, item, mv);
                        String authority = mv.getAuthority();
                        String value = mv.getValue();
                        if (StringUtils.isNotBlank(authority) && StringUtils.isNotBlank(value)) {
                            Item orgUnit = itemService.find(context, UUID.fromString(authority));
                            updateItem(context, item, orgUnit, mv);
                        }
                    }
                }
            }
        }
    }

    private void removeExistingMetadata(Context context, Item item, MetadataValue mv) throws SQLException {
        removeMetadata(context, item, mv.getElement(), "titleAlternative");
        removeMetadata(context, item, mv.getElement(), "legalName");
        removeMetadata(context, item, mv.getElement(), "acronym");
    }

    private void removeMetadata(Context context, Item item, String element, String qualifier) throws SQLException {
        itemService.removeMetadataValues(context, item, "perucris", element, qualifier,
            null);
    }

    private void updateItem(Context context, Item originItem, Item orgUnit, MetadataValue originMetadataValue)
            throws SQLException {
        List<MetadataValue> title = itemService.getMetadata(orgUnit, "dc","title","alternative", Item.ANY);
        List<MetadataValue> legalName = itemService.getMetadata(orgUnit, "organization", "legalName",null, Item.ANY);
        List<MetadataValue> acronym = itemService.getMetadata(orgUnit, "oairecerif", "acronym", null, Item.ANY);
        addMetadata(context, originItem, originMetadataValue.getElement(), "titleAlternative", title);
        addMetadata(context, originItem, originMetadataValue.getElement(), "legalName", legalName);
        addMetadata(context, originItem, originMetadataValue.getElement(), "acronym", acronym);
    }

    private void addMetadata(Context context, Item item, String element, String qualifier,
                             List<MetadataValue> values) throws SQLException {

        if (CollectionUtils.isNotEmpty(values)) {
            for (MetadataValue value : values) {
                itemService.addMetadata(context, item, "perucris", element, qualifier, null, value.getValue());
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

}