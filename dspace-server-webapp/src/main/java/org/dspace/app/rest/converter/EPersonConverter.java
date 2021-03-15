/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the EPerson in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class EPersonConverter extends DSpaceObjectConverter<EPerson, org.dspace.app.rest.model.EPersonRest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EPersonConverter.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private MetadataValueDTOConverter metadataValueConverter;

    @Override
    public EPersonRest convert(EPerson obj, Projection projection) {
        EPersonRest eperson = super.convert(obj, projection);
        eperson.setLastActive(obj.getLastActive());
        eperson.setNetid(obj.getNetid());
        eperson.setCanLogIn(obj.canLogIn());
        eperson.setRequireCertificate(obj.getRequireCertificate());
        eperson.setSelfRegistered(obj.getSelfRegistered());
        eperson.setEmail(obj.getEmail());

        addDefaultGroup(eperson, projection);
        return eperson;
    }

    @Override
    protected EPersonRest newInstance() {
        return new EPersonRest();
    }

    @Override
    public Class<EPerson> getModelClass() {
        return EPerson.class;
    }

    private void addDefaultGroup(EPersonRest eperson, Projection projection) {
        final String defaultGroup = configurationService.getProperty("eperson.group.default");
        if (StringUtils.isBlank(defaultGroup)) {
            return;
        }
        Optional<Context> context = Optional.ofNullable(requestService.getCurrentRequest())
            .map(cr -> ContextUtil.obtainContext(cr.getServletRequest()));
        if (context.isEmpty()) {
            return;
        }
        try {
            Group group = groupService.find(context.get(), UUIDUtils.fromString(defaultGroup));
            if (Objects.isNull(group)) {
                return;
            }
            MetadataValueDTO metadataValue = new MetadataValueDTO("perucris", "eperson", "role", null,
                group.getNameWithoutTypePrefix(), UUIDUtils.toString(group.getID()), Choices.CF_ACCEPTED);


            SortedMap<String, List<MetadataValueRest>> metadataMap = eperson.getMetadata().getMap();
            metadataMap.putIfAbsent("perucris.eperson.role", new ArrayList<>());

            MetadataValueRest metadataValueRest = metadataValueConverter.convert(metadataValue);

            List<MetadataValueRest> metadataList = metadataMap.get("perucris.eperson.role");
            metadataValueRest.setPlace(metadataList.size());
            metadataList.add(metadataValueRest);

        } catch (SQLException e) {
            LOGGER.warn("Error while finding default group: {}", e.getMessage());
        }
    }

}
