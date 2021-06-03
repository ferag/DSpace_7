/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Consumer that takes care of set ubigeo region.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UbigeoConsumer implements Consumer {

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemService itemService;
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() == Constants.ITEM) {
            Item item = (Item) event.getSubject(context);

            if (item == null || itemsAlreadyProcessed.contains(item)) {
                return;
            }

            String ubigeo = getAuthorityValue(item, "perucris", "ubigeo", null);
            String ubigeoSunat = getAuthorityValue(item, "perucris", "ubigeo", "ubigeoSunat");
            String domicilioReniec = getAuthorityValue(item, "perucris", "domicilio", "ubigeoReniec");
            String nacimientoReniec = getAuthorityValue(item, "perucris", "nacimiento", "ubigeoReniec");

            updateRegion(context, item, ubigeo, "perucris", "ubigeoRegion", null);
            updateRegion(context, item, ubigeoSunat, "perucris", "ubigeo", "ubigeoSunatRegion");
            updateRegion(context, item, domicilioReniec, "perucris", "domicilio", "ubigeoReniecRegion");
            updateRegion(context, item, nacimientoReniec, "perucris", "nacimiento", "ubigeoReniecRegion");

            itemsAlreadyProcessed.add(item);
        }
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}

    private String getAuthorityValue(Item item, String schema, String element, String qualifier) {
        List<MetadataValue> values = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0).getAuthority();
    }

    private void updateRegion(Context context, Item item, String value, String schema, String element,
            String qualifier) {
        if (StringUtils.isBlank(value) || (value.length() < 2)) {
            return;
        }
        String vocabularyId = value.substring(0, 2);
        ChoiceAuthority source = choiceAuthorityService.getChoiceAuthorityByAuthorityName("peru_ubigeo");
        Choice choice = source.getChoice(vocabularyId, context.getCurrentLocale().toString());
        if (StringUtils.isNotBlank(choice.label)) {
            try {
                if (StringUtils.isNoneBlank(itemService.getMetadataFirstValue(item, schema, element, qualifier,null))) {
                    itemService.replaceMetadata(context, item, schema, element, qualifier,
                                                null, choice.label, vocabularyId, Choices.CF_ACCEPTED, 0);
                } else {
                    itemService.addMetadata(context, item, schema, element, qualifier, null, choice.label, vocabularyId,
                            Choices.CF_ACCEPTED);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}