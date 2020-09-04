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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.model.OIDCIntrospectResponse;
import org.dspace.authenticate.model.OIDCTokenResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    private static final Logger log = LoggerFactory.getLogger(CasAuthentication.class);

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
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        String role = (String) request.getParameter("role");
        if (role != null) {
            Group group = groupService.find(context, UUID.fromString(role));
            if (group == null) {
                throw new RuntimeException("Role " + role + " not found");
            } else {
                List<Group> result = new ArrayList<>();
                result.add(group);
                return result;
            }
        }
        return new ArrayList<Group>();
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {
        if (request == null) {
            log.warn("Unable to authenticate using CAS OpenID Connect because the request object is null.");
            return BAD_ARGS;
        }
        String clientId = configurationService.getProperty("authentication-cas.clientid");
        String clientSecret = configurationService.getProperty("authentication-cas.clientsecret");
        String tokenEndpoint = configurationService.getProperty("authentication-cas.tokenendpoint");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(tokenEndpoint);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.addHeader("Authorization","Basic " + Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes()));
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", (String) request.getParameter("code")));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri",
            configurationService.getProperty("authentication-cas.redirecturi")));
        try {
            HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            StringWriter writerEntity = new StringWriter();
            IOUtils.copy(entity.getContent(), writerEntity, "UTF-8");
            System.out.println("Request -> " + writerEntity.toString());
            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
            String body = writer.toString();
            ObjectMapper om = new ObjectMapper();
            OIDCTokenResponse tokens = om.readValue(body, OIDCTokenResponse.class);
            if (tokens.getIdToken() != null && !tokens.getIdToken().isEmpty()) {
                String ePersonId = checkFieldAndExtractEperson(tokens);
                if (ePersonId != null) {
                    EPerson eperson = ePersonService.find(context, UUID.fromString(ePersonId));
                    if (eperson == null) {
                        return AuthenticationMethod.NO_SUCH_USER;
                    } else {
                        context.setCurrentUser(eperson);
                        AuthenticateServiceFactory.getInstance().getAuthenticationService()
                            .initEPerson(context, request, eperson);
                        log.info(ePersonId + " has been authenticated via CAS");
                        return AuthenticationMethod.SUCCESS;
                    }
                }
            }
        } catch (IOException e) {
            context.setCurrentUser(null);
        }
        return AuthenticationMethod.NO_SUCH_USER;
    }

    private String checkFieldAndExtractEperson(OIDCTokenResponse tokens) {
        try {
            String clientId = configurationService.getProperty("authentication-cas.clientid");
            String clientSecret = configurationService.getProperty("authentication-cas.clientsecret");
            String tokenEndpoint = configurationService.getProperty("authentication-cas.introspectendpoint");
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(tokenEndpoint);
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");
            post.addHeader("Authorization","Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes()));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("token", tokens.getAccessToken()));
            HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            StringWriter writerEntity = new StringWriter();
            IOUtils.copy(entity.getContent(), writerEntity, "UTF-8");
            System.out.println("Request -> " + writerEntity.toString());
            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");
            String body = writer.toString();
            ObjectMapper om = new ObjectMapper();
            OIDCIntrospectResponse introspect = om.readValue(body, OIDCIntrospectResponse.class);
            return introspect.getSubject();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        return configurationService.getProperty("authentication-cas.authorizeurl", "http://localhost:8081/oidc/authorize")
            + "?client_id=" + configurationService.getProperty("authentication-cas.clientid")
            + "&response_type=code&scope=openid&redirect_uri="
            + configurationService.getProperty("authentication-cas.redirecturi");
    }

    @Override
    public String getName() {
        return "cas";
    }

}
