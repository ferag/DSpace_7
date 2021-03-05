package org.dspace.externalregistration.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.authenticate.model.OIDCProfileElementsResponse;
import org.dspace.authenticate.model.OIDCTokenResponse;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.externalregistration.provider.ExternalRegistrationProvider;
import org.dspace.externalregistration.service.ExternalRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;


public class ExternalRegistrationServiceImpl implements ExternalRegistrationService {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private List<ExternalRegistrationProvider> externalRegistrationProviders;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean canRegister(OIDCProfileElementsResponse userData) {
        return getProvider(userData) != null;
    }


    @Override
    public EPerson registerEPerson(
            Context context,
            HttpServletRequest request,
            OIDCTokenResponse tokens,
            OIDCProfileElementsResponse userData)
            throws SQLException {

        EPerson eperson = null;
        try {
            context.turnOffAuthorisationSystem();

            ExternalRegistrationProvider provider = getProvider(userData);
            if (provider == null) {
                throw new IllegalStateException("No provider found for the user data context.");
            }

            eperson = provider.createEPerson(context, userData);

            createInitialGrant(context, tokens, eperson);

            eperson.setCanLogIn(true);
            authenticationService.initEPerson(context, request, eperson);
            ePersonService.update(context, eperson);

            context.dispatchEvents();
            context.setCurrentUser(eperson);


        } catch (AuthorizeException | JsonProcessingException e) {
            throw new IllegalStateException();
        } finally {
            context.restoreAuthSystemState();
        }
        return eperson;


    }

    /**
     * Creates the initial grant required after person registration.
     * @param context
     * @param tokens
     * @param eperson
     * @throws JsonProcessingException
     * @throws SQLException
     */
    private void createInitialGrant(Context context,
            OIDCTokenResponse tokens, EPerson eperson) throws JsonProcessingException, SQLException {

        ClientModel clientModel = new ClientModel();
        clientModel.setClientName(tokens.getRelationshipClientName());
        clientModel.setClientId(tokens.getRelationshipClientId());
        clientModel.setId(UUID.randomUUID().toString());
        clientModel.setScopes(tokens.getScope().replace(" ", ","));
        clientModel.setIssuedAt(tokens.getRelationshipIssuedAt());
        clientModel.setExpireAt(tokens.getRelationshipExpireAt());
        ObjectMapper mapper = new ObjectMapper();
        String client = mapper.writeValueAsString(clientModel);
        ePersonService.addMetadata(context, eperson, "perucris", "oidc", "granted", null, client);
    }

    private ExternalRegistrationProvider getProvider(
            OIDCProfileElementsResponse userData) {
        for (ExternalRegistrationProvider provider: getExternalRegistrationProviders()) {
            if (provider.support(userData)) {
                return provider;
            }
        }
        return null;
    }

    public List<ExternalRegistrationProvider> getExternalRegistrationProviders() {
        return externalRegistrationProviders;
    }


    public void setExternalRegistrationProviders(List<ExternalRegistrationProvider> externalRegistrationProviders) {
        this.externalRegistrationProviders = externalRegistrationProviders;
    }

    static class ClientModel {

        private String id;
        private String clientName;
        private String scopes;
        private String clientId;
        private Long issuedAt;
        private Long expireAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Long getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(Long issuedAt) {
            this.issuedAt = issuedAt;
        }

        public Long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(Long expireAt) {
            this.expireAt = expireAt;
        }

    }

}
