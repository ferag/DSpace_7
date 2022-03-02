/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;


import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.model.CasProfileElementsResponse;
import org.dspace.authenticate.model.CasTokenResponse;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.externalregistration.factory.ExternalRegistrationServiceFactory;
import org.dspace.externalregistration.service.ExternalRegistrationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Custom OpenID Connect Authentication for DSpace to use perucas. For the
 * general oidc authentication see {@link OidcAuthentication}.
 * 
 * This implementation doesn't allow/needs to register user, which may be holder
 * by the openID authentication server.
 * 
 * @link https://openid.net/developers/specs/
 * 
 * @author pasquale.cavallo at 4science dot it
 */
public class CasAuthentication implements AuthenticationMethod {

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory
        .getInstance().getGroupService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory
        .getInstance().getMetadataFieldService();
    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory
        .getInstance().getMetadataSchemaService();
    protected ConfigurationService configurationService = DSpaceServicesFactory
        .getInstance().getConfigurationService();
    protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance()
            .getAuthenticationService();
    protected ExternalRegistrationService externalRegistrationService = ExternalRegistrationServiceFactory.getInstance()
            .getExternalRegistrationService();

    private static final Logger log = LoggerFactory.getLogger(CasAuthentication.class);

    /**
     * User are not allow to set/change password to them users
     * through this resource server.
     * Passwords are hold and manage by the authorization server, so this method
     * return false in any case
     *
     * @param context DSpace context
     * @param request HTTP request, in case anything in that is used to decide
     * @param email   e-mail address of user attempting to register
     * 
     */
    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    /**
     * Predicate, is this an implicit authentication method. An implicit method
     * gets credentials from the environment (such as an HTTP request or even
     * Java system properties) rather than the explicit username and password.
     * For example, a method that reads the X.509 certificates in an HTTPS
     * request is implicit.
     * For OpenID Connect authention, this method return always false.
     *
     * @return true if this method uses implicit authentication
     */
    @Override
    public boolean isImplicit() {
        return false;
    }

    /**
     * User are not allow to register through this resource server.
     * Since users are hold and manage by the authorization server,
     * this method return false in any case.
     * 
     * @param context  DSpace context
     * @param request  HTTP request, in case anything in that is used to decide
     * @param username e-mail address of user attempting to register
     * 
     */
    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    /**
     * In the OpenID connect implicit (authorization code) flow scenario, the resource server (US) need to resolve
     * convert the code into an access token. With the access token the resource server
     * will be able to access to the user data.
     * Groups will be available in the /introspect and /profile APIs, which needs the access token
     * to be called.
     * Then, the resource server MUST first resolve the code into and access token. This operation
     * will be done into the authenticate method, which populate the group into the authentication
     * object too.
     * 
     * @param context A valid DSpace context.
     * @param request The request that started this operation, or null if not
     *                applicable.
     * @return an empty array of Group
     */
    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        Boolean isUserAuthenticated = (Boolean) request.getAttribute("cas.isuserauthenticate");
        log.debug("CasAuthentication entered getSpecialGroups, isuserauthenticate" + isUserAuthenticated);
        Group choosedGroup = null;
        if (isUserAuthenticated != null && isUserAuthenticated == true) {
            choosedGroup = (Group)request.getAttribute("cas.epersonauthenticated.groups");
        }
        log.debug("CasAuthentication entered getSpecialGroups, choosedGroup" + choosedGroup);
        if (choosedGroup != null) {
            return Arrays.asList(choosedGroup);
        } else {
            return new ArrayList<Group>();
        }
    }

    /**
     * This method authenticate the user using OpenID code grant (implicit) flow.
     * This operation require two step.
     * First, we need to get an access token from the code received in the request,
     * then, we need to call the introspect method to get
     * the user-related data and authenticate him.
     * 
     * The code will be received in a query string parameter named `code`.
     * 
     * @param context  DSpace context, will be modified (ePerson set) upon success.
     * @param username Not user
     * @param password Not used
     * @param realm    Not used
     * @param request  The HTTP request that started this operation, or null if not
     *                 applicable.
     * @return One of: SUCCESS, NO_SUCH_USER, BAD_ARGS
     *  -> SUCCESS - authenticated OK.
     *  -> NO_SUCH_USER - user not found using this method. <br>
     *  -> BAD_ARGS - user/pw not appropriate for this method
     * @throws SQLException if database error

     */
    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {
        if (request == null) {
            log.warn("Unable to authenticate using OpenID Connect because the request object is null.");
            return BAD_ARGS;
        }

        Boolean isUserAuthenticated = (Boolean) request.getAttribute("cas.isuserauthenticate");
        if (isUserAuthenticated == null || isUserAuthenticated == false) {
            attachUserDataToRequest(request,context);
        }
        EPerson eperson  = (EPerson) request.getAttribute("cas.epersonauthenticated");
        if (eperson  != null) {
            context.setCurrentUser(eperson);
            AuthenticateServiceFactory.getInstance().getAuthenticationService()
                .initEPerson(context, request, eperson);
            log.info(eperson.getEmail() + " has been authenticated via OpenID");
            return AuthenticationMethod.SUCCESS;
        } else {
            return AuthenticationMethod.NO_SUCH_USER;
        }
    }

    private CasProfileElementsResponse getUserData(HttpServletRequest request, CasTokenResponse tokens) {
        CasProfileElementsResponse userData = null;
        if (tokens != null && tokens.getAccessToken() != null && !tokens.getAccessToken().isEmpty()) {
            userData = checkFieldAndExtractEperson(tokens);
        }
        return userData;
    }

    private EPerson getEPerson(Context context, HttpServletRequest request,
            CasTokenResponse tokens, CasProfileElementsResponse userData)
            throws SQLException {

        EPerson eperson = null;
        if (!tokens.isRelationshipVerified()) {
            // check whether an user with the same external ids already exists
            // it means that a registration with the same token has already occurred.
            if (StringUtils.isNotBlank(userData.getReniecDni())) {
                final MetadataField field = metadataFieldService.findByString(context, "perucris.eperson.dni", '.');
                eperson = ePersonService.findByEid(context, field, userData.getReniecDni());
            } else if (StringUtils.isNotBlank(userData.getOrcid())) {
                final MetadataField field = metadataFieldService.findByString(context, "perucris.eperson.orcid", '.');
                eperson = ePersonService.findByEid(context, field, userData.getOrcid());
            }

            if (eperson == null && StringUtils.isNotBlank(userData.getEmail())) {
                eperson = ePersonService.findByEmail(context, userData.getEmail());
            }

            if (eperson != null) {
                return eperson;
            }

            // attempt registration with the data available
            if (externalRegistrationService.canRegister(userData)) {
                return externalRegistrationService.registerEPerson(context, request, tokens, userData);
            } else {
                return null;
            }

        }

        String ePersonId = userData.getSub();
        if (ePersonId != null) {
            eperson = ePersonService.find(context, UUID.fromString(ePersonId));
            if (eperson == null) {
                log.warn("Cannot find eperson with epersonId: " + ePersonId);
            }
            return eperson;
        }

        return null;

    }

    private void attachUserDataToRequest(HttpServletRequest request, Context context) throws SQLException {

        CasTokenResponse tokens = getAuthToken(request);

        CasProfileElementsResponse userData = getUserData(request, tokens);

        EPerson eperson = getEPerson(context, request, tokens, userData);

        if (eperson != null) {
            request.setAttribute("cas.epersonauthenticated", eperson);
            request.setAttribute("cas.isuserauthenticate", true);
            String role = userData.getPgcRole();
            log.debug("Released role: " + role);
            if (role != null) {
                Group group = groupService.find(context, UUID.fromString(role));
                if (group == null) {
                    log.warn("Role not found: " + role);
                } else {
                    request.setAttribute("cas.epersonauthenticated.groups", group);
                }
            }
        }
    }

    private CasTokenResponse getAuthToken(HttpServletRequest request) {
        String clientId = configurationService.getProperty("authentication-cas.client-id");
        String clientSecret = configurationService.getProperty("authentication-cas.client-secret");
        String tokenEndpoint = configurationService.getProperty("authentication-cas.token-endpoint");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(tokenEndpoint);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.addHeader("Authorization","Basic " + Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes()));
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", (String) request.getParameter("code")));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", configurationService.getProperty("dspace.server.url") +
            "/api/authn/cas"));
        params.add(new BasicNameValuePair("client_id", clientId));
        try {
            HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            StringWriter writerEntity = new StringWriter();
            IOUtils.copy(entity.getContent(), writerEntity, "UTF-8");
            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
            String body = writer.toString();
            ObjectMapper om = new ObjectMapper();
            return om.readValue(body, CasTokenResponse.class);
        } catch (Exception e) {
            log.error("An error occurs retrieving CAS token: ", e);
            return null;
        }
    }

    /**
     * This method is responsible to call the userinfo endpoint of the CAS
     * authorization server in order to load the user data.
     * 
     * @param tokens The auth token
     * @return data get from the instrospect endpoint
     * 
     */
    private CasProfileElementsResponse checkFieldAndExtractEperson(CasTokenResponse tokens) {

        try {
            CasProfileElementsResponse data = new CasProfileElementsResponse();
            String userinfoEndpoint = configurationService.getProperty("authentication-cas.user-info-endpoint");
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(userinfoEndpoint + "?access_token=" + tokens.getAccessToken());
            HttpResponse response = client.execute(get);
            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
            String body = writer.toString();
            DocumentContext document = JsonPath.parse(body);
            String sub = document.read("$.sub", String.class);
            data.setSub(sub);
            try {
                String pgcRole = document.read("$.pgc-role", String.class);
                data.setPgcRole(pgcRole);
            } catch (Exception e) {
                log.warn("Cannot find role in userInfo response");
                return null;
            }
            try {
                String reniecDni = document.read("$.reniec_dni", String.class);
                data.setReniecDni(reniecDni);
            } catch (Exception e) {
                log.debug("Cannot find reniecDni in userInfo response");
            }
            try {
                String orcid = document.read("$.orcid", String.class);
                data.setOrcid(orcid);
            } catch (Exception e) {
                log.debug("Cannot find orcid in userInfo response");
            }
            try {
                String email = document.read("$.email", String.class);
                data.setEmail(email);
            } catch (Exception e) {
                log.debug("Cannot find email in userInfo response");
            }
            try {
                String familyName = document.read("$.family_name", String.class);
                data.setFamilyName(familyName);
            } catch (Exception e) {
                log.debug("Cannot find familyName in userInfo response");
            }
            try {
                String givenName = document.read("$.given_name", String.class);
                data.setGivenName(givenName);
            } catch (Exception e) {
                log.debug("Cannot find givenName in userInfo response");
            }
            try {
                String birthDate = document.read("$.birthdate", String.class);
                data.setBirthdate(birthDate);
            } catch (Exception e) {
                log.debug("Cannot find birthDate in userInfo response");
            }
            return data;
        } catch (IOException e) {
            log.error("Exception throwing trying to load data from userInfo, URL: "
                + configurationService.getProperty("authentication-cas.user-info-endpoint"));
            //null managed in caller
            return null;
        }
    }

    /**
     * Get login page to which to redirect. This URL points to the authorization
     * server authorize method, in order to get the authorization code.
     *
     * @param context  DSpace context, will be modified (ePerson set) upon success.
     * @param request  The HTTP request that started this operation, or null if not
     *                 applicable.
     * @param response The HTTP response from the servlet method.
     * @return fully-qualified URL
     */
    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        String authorizeUrl = configurationService.getProperty("authentication-cas.authorize-endpoint");
        String clientId = configurationService.getProperty("authentication-cas.client-id");
        String redirectUri = configurationService.getProperty("dspace.server.url") + "/api/authn/cas";
        if (StringUtils.isAnyBlank(authorizeUrl, clientId, redirectUri)) {
            log.error("Missing mandatory configuration properties for CasAuthentication");
            // empty return force the caller to skip this entry
            return "";
        }
        try {
            return authorizeUrl + "?client_id=" + clientId +
                    "&response_type=code&scope=openid+pgc-role+reniec-info+orcid&"
                + "redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    public String getName() {
        return "cas";
    }






}
