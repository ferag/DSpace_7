/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.management.RuntimeErrorException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class CasAuthentication implements AuthenticationMethod {
	
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
	protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
	protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
	protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
	
	@Override
	public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
		return false;
	}
	
	@Override
	public boolean isImplicit() {
		return false;
	}
	
	@Override
	public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
		return false;
	}
	
	@Override
	public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {	}

	
	




	@Override
	public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
		String role = (String) request.getAttribute("role");
		Group group = groupService.find(context, UUID.fromString(role));
		if(group == null) {
			throw new RuntimeException("Role "+role+" not found");
		} else {
	        List<Group> result = new ArrayList<>();
	        result.add(group);
	        return result;
		}
	}

	@Override
	public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
			throws SQLException {

        if (request == null) {
            log.warn("Unable to authenticate using Orcid because the request object is null.");
            return BAD_ARGS;
        }
        OrcidJWTUserData userData = checkOrcidCode(request.getParameter("code"), request);
        if(userData==null || userData.getAccessToken()==null || userData.getOrcid() == null || !"/authenticate".equals(userData.getScope())) {
        	log.debug("Invalid user data from ORCID JWT: {}", userData != null ? userData.toString() : null);
            return AuthenticationMethod.NO_SUCH_USER;
        }
        
        //find EPerson through netID
        EPerson eperson;
		try {
			eperson = findEPerson(context, userData.getOrcid());
	        if(eperson==null) {
	            eperson = registerNewEPerson(context, request, userData);
	        }
	        if (eperson == null) {
	            return AuthenticationMethod.NO_SUCH_USER;
	        }
            context.setCurrentUser(eperson);
            AuthenticateServiceFactory.getInstance().getAuthenticationService().initEPerson(context, request, eperson);
            log.info(eperson.getNetid() + " has been authenticated via ORCID");
            return AuthenticationMethod.SUCCESS;
		} catch (AuthorizeException e) {
            log.error("Unable to successfully authenticate using ORCID for user because of an exception.", e);
            context.setCurrentUser(null);
            return AuthenticationMethod.NO_SUCH_USER;
		}

		
		
		return 0;
	}

	@Override
	public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
		//?response_type=code&scope=openid&client_id=client2&redirect_uri=http://localhost:8043/app2
        return configurationService.getProperty("authentication-cas.authorizeurl", "http://localhost:8081/oidc/authorize")
        	+ "?client_id=" + configurationService.getProperty("authentication-orcid.clientid")
        	+ "&response_type=code&scope=openid&redirect_uri="
        	+ configurationService.getProperty("authentication-cas.redirecturi");
	}

	@Override
	public String getName() {
		return "cas";
	}

}
