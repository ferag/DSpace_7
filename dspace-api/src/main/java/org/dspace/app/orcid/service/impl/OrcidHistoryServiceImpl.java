/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ibm.icu.text.DecimalFormat;
import org.apache.commons.collections.CollectionUtils;
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
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidHistoryDAO;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.util.OrcidWorkMetadata;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.SimpleMapConverter;
import org.dspace.util.UUIDUtils;
import org.orcid.jaxb.model.common.CitationType;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingType;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.ContributorAttributes;
import org.orcid.jaxb.model.v3.release.common.ContributorEmail;
import org.orcid.jaxb.model.v3.release.common.ContributorOrcid;
import org.orcid.jaxb.model.v3.release.common.CreditName;
import org.orcid.jaxb.model.v3.release.common.Day;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Month;
import org.orcid.jaxb.model.v3.release.common.OrcidIdBase;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
import org.orcid.jaxb.model.v3.release.common.PublicationDate;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.common.Year;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkContributors;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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

    @Autowired
    @Qualifier("mapConverterOrcidWorkType")
    private SimpleMapConverter mapConverterModifier;

    private HttpClient httpClient;

    @PostConstruct
    private void setup() {
        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
    }

    @Override
    public OrcidHistory find(Context context, int id) throws SQLException {
        return orcidHistoryDAO.findByID(context, OrcidHistory.class, id);
    }

    @Override
    public List<OrcidHistory> findByOwner(Context context, Item owner) throws SQLException {
        return orcidHistoryDAO.findByOwner(context, owner);
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
            throw new AuthorizeException("You must be an admin to delete a OrcidHistory");
        }
        orcidHistoryDAO.delete(context, orcidHistory);
    }

    @Override
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to update a OrcidHistory");
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
        String entityType = getMetadataValue(entity, "dspace.entity.type");
        if (entityType == null) {
            throw new IllegalArgumentException("The related entity item does not have a relationship type");
        }

        switch (entityType) {
            case "Person":
                return sendPersonToOrcid(context, orcidQueue, orcid, token);
            case "Publication":
                return sendPublicationToOrcid(context, orcidQueue, orcid, token, forceAddition);
            case "Project":
                return sendProjectToOrcid(context, orcidQueue, orcid, token, forceAddition);
            default:
                throw new IllegalArgumentException("The item to send to ORCID has an invalid type: " + entityType);

        }
    }

    private OrcidHistory sendPublicationToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
            boolean forceAddition) throws SQLException {
        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();
        OrcidWorkMetadata itemMetadata = new OrcidWorkMetadata(entity);
        Long putCode = null;
        Work work = new Work();
        addAuthors(context, work, itemMetadata);
        addPubblicationDate(work, itemMetadata);
        addExternalIdentifiers(work, itemMetadata);
        addType(work, itemMetadata);
        addCitation(work, itemMetadata);
        WorkTitle workTitle = new WorkTitle();
        workTitle.setTitle(new Title(itemMetadata.getTitle()));
        work.setWorkTitle(workTitle);
        if (!forceAddition) {
            putCode = findPutCode(context, entity, owner);
            work.setPutCode(putCode);
        } else {
            deleteOldRecords(context, entity, owner);
        }
        return sendObjectToOrcid(context, orcidQueue, orcid, token, putCode, work, WORK_ENDPOINT, Work.class);
    }

    private OrcidHistory sendProjectToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
            boolean forceAddition) throws SQLException {

        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();

        Funding funding = new Funding();
        funding.setType(FundingType.GRANT);

        Long putCode = null;

        if (!forceAddition) {
            putCode = findPutCode(context, entity, owner);
            funding.setPutCode(putCode);
        } else {
            deleteOldRecords(context, entity, owner);
        }

        String title = getMetadataValue(entity, "dc.title");
        FundingTitle fundTitle = new FundingTitle();
        fundTitle.setTitle(new Title(title));
        funding.setTitle(fundTitle);

        funding.setExternalIdentifiers(getExternalIds(entity));

        MetadataValue coordinator = getMetadata(entity, "crispj.coordinator");
        if (coordinator != null && coordinator.getAuthority() != null) {
            Item organization = itemService.findByIdOrLegacyId(context, coordinator.getAuthority());
            String name = getMetadataValue(organization, "dc.title");
            String city = getMetadataValue(organization, "organization.address.addressLocality");
            Iso3166Country country = Iso3166Country
                    .fromValue(getMetadataValue(organization, "organization.address.addressCountry"));
            Organization org = new Organization();
            org.setName(name);
            OrganizationAddress orgAddress = new OrganizationAddress();
            orgAddress.setCity(city);
            orgAddress.setCountry(country);
            org.setAddress(orgAddress);

            funding.setOrganization(org);
        }
        return sendObjectToOrcid(context, orcidQueue, orcid, token, putCode, funding, FUNDING_ENDPOINT, Funding.class);
    }

    private OrcidHistory sendPersonToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token)
            throws SQLException {
        // TODO send person info to orcid
        orcidQueueDAO.delete(context, orcidQueue);
        return null;
    }

    private <T> OrcidHistory sendObjectToOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
        Long putCode, T objToSend, String endpoint, Class<T> clazz) {

        Item entity = orcidQueue.getEntity();
        Item owner = orcidQueue.getOwner();
        String orcidUrl = configurationService.getProperty("orcid-api.api-url");
        String path = orcidUrl + "/" + orcid + endpoint;
        HttpEntityEnclosingRequestBase request = putCode != null ? new HttpPut(path + "/" + putCode)
                : new HttpPost(path);

        HttpResponse response = null;

        try {

            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            String objToSendAsString = marshall(jaxbContext, objToSend);

            request.addHeader("Content-Type", "application/vnd.orcid+xml");
            request.addHeader("Authorization", "Bearer " + token);
            request.setEntity(new StringEntity(objToSendAsString));

            response = httpClient.execute(request);

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
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Long findPutCode(Context context, Item entity, Item owner) throws SQLException {
        return orcidHistoryDAO.findByOwnerAndEntity(context, owner.getID(), entity.getID()).stream()
                              .filter(history -> StringUtils.isNotBlank(history.getPutCode()))
                              .map(history -> Long.valueOf(history.getPutCode())).findFirst().orElse(null);
    }

    private ExternalIDs getExternalIds(Item entity) {
        List<ExternalID> externalIds = new ArrayList<ExternalID>();
        String indentifierUri = getMetadataValue(entity, "dc.identifier.uri");

        ExternalID handle = new ExternalID();
        handle.setValue(indentifierUri);
        handle.setType("handle");
        handle.setRelationship(Relationship.SELF);
        externalIds.add(handle);

        String indentifierDoi = getMetadataValue(entity, "dc.identifier.doi");
        if (StringUtils.isNotBlank(indentifierDoi)) {
            ExternalID doi = new ExternalID();
            doi.setType("doi");
            doi.setValue(indentifierDoi);
            doi.setRelationship(Relationship.SELF);
            externalIds.add(doi);
        }
        ExternalIDs retExIds = new ExternalIDs();
        // this look a bit odd but it is the only way to add external ids in the orcid official jaxb serialization
        retExIds.getExternalIdentifier().addAll(externalIds);
        return retExIds;
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

    private void deleteOldRecords(Context context, Item entity, Item owner) throws SQLException {
        List<OrcidHistory> orcidHistories = orcidHistoryDAO.findByOwnerAndEntity(context, owner.getID(),entity.getID());
        for (OrcidHistory orcidHistory : orcidHistories) {
            orcidHistoryDAO.delete(context, orcidHistory);
        }
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private void addAuthors(Context context, Work work, OrcidWorkMetadata itemMetadata) throws SQLException {

        List<MetadataValue> authors = itemMetadata.getAuthors();
        if (CollectionUtils.isEmpty(authors)) {
            return;
        }

        WorkContributors workContributors = new WorkContributors();
        for (MetadataValue valContributor : authors) {
            Contributor contributor = new Contributor();
            String name = valContributor.getValue();
            UUID authorityUuid = UUIDUtils.fromString(valContributor.getAuthority());
            if (authorityUuid != null) {
                Item authorItem = itemService.find(context, authorityUuid);
                String email = itemService.getMetadataFirstValue(authorItem, "person", "email", null, null);
                String orcidId = itemService.getMetadataFirstValue(authorItem, "person", "identifier", "orcid", null);

                if (StringUtils.isNotBlank(email)) {
                    ContributorEmail contributorEmail = new ContributorEmail();
                    contributorEmail.setValue(email);
                    contributor.setContributorEmail(contributorEmail);
                }

                if (StringUtils.isNotBlank(orcidId)) {
                    String orcidDomain = configurationService.getProperty("orcid.domain-url");
                    if (StringUtils.isNotBlank(orcidId)) {
                        OrcidIdBase orcidBase = new OrcidIdBase();
                        orcidBase.setHost(orcidDomain);
                        orcidBase.setPath(orcidId);
                        orcidBase.setUri(orcidDomain + "/" + orcidId);
                        contributor.setContributorOrcid(new ContributorOrcid(orcidBase));
                    }
                }
            }

            contributor.setCreditName(new CreditName(name));
            ContributorAttributes attributes = new ContributorAttributes();
            attributes.setContributorRole(ContributorRole.AUTHOR);
            contributor.setContributorAttributes(attributes);
            workContributors.getContributor().add(contributor);
        }

        work.setWorkContributors(workContributors);
    }

    private void addPubblicationDate(Work work, OrcidWorkMetadata itemMetadata) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        FuzzyDate publicationDate = new FuzzyDate();

        if (StringUtils.isNotBlank(itemMetadata.getYear())
            || StringUtils.isNotBlank(itemMetadata.getMonth())
            || StringUtils.isNotBlank(itemMetadata.getDay())) {

            if (StringUtils.isNotBlank(itemMetadata.getYear())) {
                Year year = new Year();
                year.setValue(itemMetadata.getYear());
                publicationDate.setYear(year);
            }

            if (StringUtils.isNotBlank(itemMetadata.getMonth())) {
                Month month = new Month();
                month.setValue(decimalFormat.format(Long.parseLong(itemMetadata.getMonth())));
                publicationDate.setMonth(month);
            }

            if (StringUtils.isNotBlank(itemMetadata.getDay())) {
                Day day = new Day();
                day.setValue(decimalFormat.format(Long.parseLong(itemMetadata.getDay())));
                publicationDate.setDay(day);
            }
            work.setPublicationDate(new PublicationDate(publicationDate));
        }
    }

    private void addExternalIdentifiers(Work work, OrcidWorkMetadata itemMetadata) {
        if (itemMetadata.getExternalIdentifier() != null) {
            ExternalIDs workExternalIdentifiers = new ExternalIDs();
            for (String valIdentifier : itemMetadata.getExternalIdentifier()) {
                ExternalID workExternalIdentifier = new ExternalID();
                workExternalIdentifier.setType(itemMetadata.getExternalIdentifierType(valIdentifier));
                workExternalIdentifier.setValue(valIdentifier);
                workExternalIdentifier.setRelationship(Relationship.SELF);
                workExternalIdentifiers.getExternalIdentifier().add(workExternalIdentifier);
            }
            work.setWorkExternalIdentifiers(workExternalIdentifiers);
        }
    }

    private void addType(Work work, OrcidWorkMetadata itemMetadata) {
        if (mapConverterModifier == null) {
            work.setWorkType(WorkType.fromValue(itemMetadata.getWorkType()));
        } else {
            work.setWorkType(WorkType.fromValue(mapConverterModifier.getValue(itemMetadata.getWorkType())));
        }
    }

    private void addCitation(Work work, OrcidWorkMetadata itemMetadata) {
        String citationVal = itemMetadata.getCitation();
        if (StringUtils.isNotBlank(citationVal)) {
            Citation citation = new Citation();
            citation.setWorkCitationType(CitationType.BIBTEX);
            citation.setCitation(citationVal);
            work.setWorkCitation(citation);
        }
    }
}
