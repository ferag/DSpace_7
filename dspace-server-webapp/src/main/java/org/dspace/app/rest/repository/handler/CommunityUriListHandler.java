/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is responsible to transform uri-list to community object
 * 
 * @author mykhaylo
 *
 */
@Component
public class CommunityUriListHandler implements UriListHandler<Community> {
    @Autowired
    private CommunityService communityService;

    @Override
    public Community handle(Context context, HttpServletRequest request, List<String> uriList)
            throws SQLException, AuthorizeException {
        String requestUriListString = uriList.get(0);
        if (StringUtils.isBlank(requestUriListString)) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        String regex = "\\/api\\/" + CommunityRest.CATEGORY + "\\/" + English.plural(CommunityRest.NAME)
                + "\\/";
        String[] split = requestUriListString.split(regex, 2);
        if (split.length != 2) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        return communityService.find(context, UUID.fromString(split[1]));
    }

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (Community.class.isAssignableFrom(clazz) && uriList != null && uriList.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
            throws AuthorizeException {
        return true;
    }
}
