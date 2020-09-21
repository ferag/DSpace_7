/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.orcid.jaxb.model.common_v2.Iso3166Country.fromValue;

import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidHistoryDAO;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.orcid.jaxb.model.common_v2.ExternalId;
import org.orcid.jaxb.model.common_v2.ExternalIds;
import org.orcid.jaxb.model.common_v2.Iso3166Country;
import org.orcid.jaxb.model.common_v2.Organization;
import org.orcid.jaxb.model.common_v2.OrganizationAddress;
import org.orcid.jaxb.model.common_v2.RelationshipType;
import org.orcid.jaxb.model.record_v2.Funding;
import org.orcid.jaxb.model.record_v2.FundingTitle;
import org.orcid.jaxb.model.record_v2.FundingType;
import org.orcid.jaxb.model.record_v2.Work;
import org.orcid.jaxb.model.record_v2.WorkTitle;
import org.orcid.jaxb.model.record_v2.WorkType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidHistoryService}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class OrcidHistoryServiceImpl implements OrcidHistoryService {

    private static final String WORK_ENDPOINT = "/work";

    private static final String FUNDING_ENDPOINT = "/funding";

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired
    private OrcidHistoryDAO orcidHistoryDAO;

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Override
    public OrcidHistory find(Context context, int id) throws SQLException {
        return orcidHistoryDAO.findByID(context, OrcidHistory.class, id);
    }

    @Override
    public OrcidHistory create(Context context, Item owner, Item entity) throws SQLException {
        OrcidHistory orcidHistory = new OrcidHistory();
        orcidHistory.setEntity(entity);
        orcidHistory.setOwner(owner);
        return orcidHistoryDAO.create(context, orcidHistory);
    }

    @Override
    public void delete(Context context, OrcidHistory orcidHistory) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a OrcidHistory");
        }
        orcidHistoryDAO.delete(context, orcidHistory);
    }

    @Override
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a OrcidHistory");
        }
        if (orcidHistory != null) {
            orcidHistoryDAO.save(context, orcidHistory);
        }
    }

    @Override
    public OrcidHistory sendToOrcid(Context context, OrcidQueue orcidQueue, boolean forceAddition) throws SQLException {
        Item owner = orcidQueue.getOwner();
        String orcid = getMetadataValue(owner, "person.identifier.orcid");
        if (orcid == null) {
            throw new IllegalArgumentException("The related owner item does not have an orcid");
        }

        String token = getMetadataValue(owner, "cris.orcid.access-token");
        if (token == null) {
            throw new IllegalArgumentException("The related owner item does not have an access token");
        }

        Item entity = orcidQueue.getEntity();
        String entityType = getMetadataValue(entity, "relationship.type");
        if (entityType == null) {
            throw new IllegalArgumentException("The related entity item does not have a relationship type");
        }

        switch (entityType) {
            case "Person":
                //TODO
                sendPersonToOrcid(context, orcidQueue, orcid, token);
                break;
            case "Publication":
                return sendPublicationToOrcid(context, orcidQueue, orcid, token, forceAddition);
            case "Project":
                return sendProjectToOrcid(context, orcidQueue, orcid, token, forceAddition);
            default:
                break;

        }
        return null;
    }

    private OrcidHistory sendPublicationToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
            boolean forceAddition) throws SQLException {

        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();
        BigInteger putCode = null;
        Work work = new Work();
        String title = getMetadataValue(entity, "dc.title");
        work.setTitle(new WorkTitle(title, null, null));
        work.setType(WorkType.JOURNAL_ARTICLE);
        work.setExternalIds(getExternalIds(entity));
        if (!forceAddition) {
            putCode = findPutCode(context, entity, owner);
            work.setPutCode(putCode);
        }
        return sendObjectToOrcid(context, orcidQueue, orcid, token, putCode, work, WORK_ENDPOINT);
    }

    private OrcidHistory sendProjectToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
            boolean forceAddition) throws SQLException {

        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();

        Funding funding = new Funding();
        funding.setType(FundingType.GRANT);

        BigInteger putCode = null;

        if (!forceAddition) {
            putCode = findPutCode(context, entity, owner);
            funding.setPutCode(putCode);
        }

        String title = getMetadataValue(entity, "dc.title");
        funding.setTitle(new FundingTitle(title, null));

        funding.setExternalIds(getExternalIds(entity));

        MetadataValue coordinator = getMetadata(entity, "crispj.coordinator");
        if (coordinator != null && coordinator.getAuthority() != null) {
            Item organization = itemService.findByIdOrLegacyId(context, coordinator.getAuthority());
            String name = getMetadataValue(organization, "dc.title");
            String city = getMetadataValue(organization, "organization.address.addressLocality");
            Iso3166Country country = fromValue(getMetadataValue(organization, "organization.address.addressCountry"));
            funding.setOrganization(new Organization(name, new OrganizationAddress(city, null, country), null));
        }
        return sendObjectToOrcid(context, orcidQueue, orcid, token, putCode, funding, FUNDING_ENDPOINT);
    }

    private void sendPersonToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token)
            throws SQLException {
        // TODO send person info to orcid
        orcidQueueDAO.delete(context, orcidQueue);
    }

    private OrcidHistory sendObjectToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
            BigInteger putCode, Object objToSend, String endpoint) {

        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();
        String orcidUrl = configurationService.getProperty("orcid-api.api-url");
        HttpClient httpClient = HttpClientBuilder.create().build();
        String path = orcidUrl + "/" + orcid + endpoint;
        HttpEntityEnclosingRequestBase request = putCode != null ? new HttpPut(path + "/" + putCode)
                : new HttpPost(path);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Work.class);
            String objToSendAsString = marshall(jaxbContext, objToSend);

            request.addHeader("Content-Type", "application/vnd.orcid+xml");
            request.addHeader("Authorization", "Bearer " + token);
            request.setEntity(new StringEntity(objToSendAsString));

            HttpResponse response = httpClient.execute(request);

            OrcidHistory history = new OrcidHistory();
            history.setEntity(entity);
            history.setOwner(owner);
            history.setLastAttempt(new Date());
            history.setResponseMessage(IOUtils.toString(response.getEntity().getContent(), UTF_8.name()));
            history.setStatus(response.getStatusLine().getStatusCode());
            if (putCode != null) {
                history.setPutCode(putCode.toString());
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                history.setSuccessAttempt(new Date());
                String incomingPutCode = getPutCodeFromResponse(response);
                if (incomingPutCode != null) {
                    history.setPutCode(incomingPutCode);
                }
                orcidHistoryDAO.create(context, history);
                orcidQueueDAO.delete(context, orcidQueue);
            } else {
                orcidHistoryDAO.create(context, history);
                context.commit();
            }
            return history;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BigInteger findPutCode(Context context, Item entity, Item owner) throws SQLException {
        return orcidHistoryDAO.findByOwnerAndEntity(context, owner.getID(), entity.getID()).stream()
                              .filter(history -> StringUtils.isNotBlank(history.getPutCode()))
                              .map(history -> new BigInteger(history.getPutCode())).findFirst().orElse(null);
    }

    private ExternalIds getExternalIds(Item entity) {
        List<ExternalId> externalIds = new ArrayList<ExternalId>();
        String indentifierUri = getMetadataValue(entity, "dc.identifier.uri");

        ExternalId handle = new ExternalId();
        handle.setExternalIdValue(indentifierUri);
        handle.setExternalIdType("handle");
        handle.setExternalIdRelationship(RelationshipType.SELF);
        externalIds.add(handle);

        String indentifierDoi = getMetadataValue(entity, "dc.identifier.doi");
        if (StringUtils.isNotBlank(indentifierDoi)) {
            ExternalId doi = new ExternalId();
            doi.setExternalIdType("doi");
            doi.setExternalIdValue(indentifierDoi);
            doi.setExternalIdRelationship(RelationshipType.SELF);
            externalIds.add(doi);
        }
        return new ExternalIds(externalIds);
    }

    private String getPutCodeFromResponse(HttpResponse response) {
        Header[] headers = response.getHeaders("Location");
        if (headers.length == 0) {
            return null;
        }
        String value = headers[0].getValue();
        return value.substring(value.lastIndexOf("/") + 1);
    }

    private String marshall(JAXBContext jaxbContext, Object jaxbObject) throws JAXBException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter stringWriter = new StringWriter();
        marshaller.marshal(jaxbObject, stringWriter);
        final String xmlString = stringWriter.toString();
        return xmlString;
    }

    private String getMetadataValue(Item item, String metadataField) {
        return item.getMetadata().stream()
                   .filter(metadata -> metadata.getMetadataField().toString('.').equals(metadataField))
                   .map(metadata -> metadata.getValue()).findFirst().orElse(null);
    }

    private MetadataValue getMetadata(Item item, String metadataField) {
        return item.getMetadata().stream()
                   .filter(metadata -> metadata.getMetadataField().toString('.').equals(metadataField)).findFirst()
                   .orElse(null);
    }
}
