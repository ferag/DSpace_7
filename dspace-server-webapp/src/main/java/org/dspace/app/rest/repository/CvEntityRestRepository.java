/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.profile.CvEntity;
import org.dspace.app.profile.service.CvEntityService;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.authorization.impl.ItemCorrectionFeature;
import org.dspace.app.rest.authorization.impl.ProfileRelatedEntityChangeFeature;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CvEntityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible of exposing cv entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(CvEntityRest.CATEGORY + "." + CvEntityRest.NAME)
public class CvEntityRestRepository extends DSpaceRestRepository<CvEntityRest, UUID> {

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CvEntityService cvEntityService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    @Override
    @PreAuthorize("isAuthenticated()")
    public CvEntityRest findOne(Context context, UUID id) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Page<CvEntityRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected CvEntityRest createAndReturn(Context context) throws AuthorizeException, SQLException {

        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        UUID itemId = UUIDUtils.fromString(request.getParameter("item"));
        if (itemId == null) {
            throw new UnprocessableEntityException("The 'item' parameter with a valid UUID is required");
        }

        Item item = itemService.find(context, itemId);
        if (item == null) {
            throw new ResourceNotFoundException("No item found by the given item UUID");
        }

        if (isNotAuthorizedToCreate(context, item)) {
            throw new RESTAuthorizationException("The user is not allowed to create a CV entity from the given item");
        }

        CvEntity cvEntity = cvEntityService.createFromItem(context, item);
        return converterService.toRest(cvEntity, utils.obtainProjection());
    }

    private boolean isNotAuthorizedToCreate(Context context, Item item) throws SQLException {
        String authorizationFeatureName = ProfileRelatedEntityChangeFeature.NAME;
        AuthorizationFeature authorizationFeature = authorizationFeatureService.find(authorizationFeatureName);
        if (authorizationFeature == null) {
            throw new IllegalStateException("No AuthorizationFeature configured with name " + authorizationFeatureName);
        }
        return !authorizationFeature.isAuthorized(context, findItemRestById(context, item.getID().toString()));
    }

    @Override
    public Class<CvEntityRest> getDomainClass() {
        return CvEntityRest.class;
    }

    private BaseObjectRest<?> findItemRestById(Context context, String itemId) throws SQLException {
        String objectId = ItemCorrectionFeature.NAME + "_" + ItemRest.CATEGORY + "." + ItemRest.NAME + "_" + itemId;
        return authorizationRestUtil.getObject(context, objectId);
    }

}
