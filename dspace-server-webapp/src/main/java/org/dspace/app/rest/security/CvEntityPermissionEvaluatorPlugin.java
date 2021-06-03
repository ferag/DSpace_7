/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * class that evaluate READ permissions
 * over a CvPublication, CvProject and CvPatent items.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class CvEntityPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(CvEntityPermissionEvaluatorPlugin.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {
        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (restPermission == null || !DSpaceRestPermission.READ.equals(restPermission)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;
        try {
            if (targetId != null) {
                UUID dsoId = UUIDUtils.fromString(targetId.toString());
                DSpaceObjectService<DSpaceObject> dSpaceObjectService;
                try {
                    dSpaceObjectService =
                        contentServiceFactory.getDSpaceObjectService(Constants.getTypeID(targetType));
                } catch (UnsupportedOperationException e) {
                    // ok not a dspace object
                    return false;
                }

                ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

                if (dSpaceObjectService != null && dsoId != null) {
                    DSpaceObject dSpaceObject = dSpaceObjectService.find(context, dsoId);

                    //If the dso is null then we give permission so we can throw another status code instead
                    if (dSpaceObject == null) {
                        return true;
                    }

                    if (dSpaceObject instanceof Item) {
                        Item item = (Item)dSpaceObject;
                        if (isCvEntity(item)) {
                            List<MetadataValue> values = itemService.getMetadata(item, "cris", "owner", null, Item.ANY);
                            if (CollectionUtils.isEmpty(values)) {
                                return false;
                            }
                            UUID researcherUuid = UUID.fromString(values.get(0).getAuthority());
                            try {
                                ResearcherProfile profile = researcherProfileService.findById(context, researcherUuid);
                                if (profile.isVisible()) {
                                    return true;
                                }
                                if (Objects.nonNull(ePerson) && profile.getId().equals(ePerson.getID())) {
                                    return true;
                                }
                            } catch (SQLException | AuthorizeException e) {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        }
                    }
                    return false;
                }
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean isCvEntity(Item item) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        return StringUtils.equalsAny(entityType, "CvPublication", "CvPatent", "CvProject");
    }

}