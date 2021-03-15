/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.content.authority.service.ItemAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.external.factory.ExternalServiceFactory;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.util.ItemAuthorityUtils;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;

/**
 * Sample authority to link a dspace item with another (i.e a publication with
 * the corresponding dataset or viceversa)
 *
 * @author Andrea Bollini
 * @author Giusdeppe Digilio
 * @version $Revision $
 */
public class ItemAuthority implements ChoiceAuthority, LinkableEntityAuthority {
    private static final Logger log = Logger.getLogger(ItemAuthority.class);
    final static String CHOICES_EXTERNALSOURCE_PREFIX = "choises.externalsource.";

    /** the name assigned to the specific instance by the PluginService, @see {@link NameAwarePlugin} **/
    private String authorityName;

    private DSpace dspace = new DSpace();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private SearchService searchService = dspace.getServiceManager().getServiceByName(
        "org.dspace.discovery.SearchService", SearchService.class);

    private ItemAuthorityServiceFactory itemAuthorityServiceFactory = dspace.getServiceManager().getServiceByName(
            "itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private List<CustomAuthorityFilter> customAuthorityFilters = dspace.getServiceManager()
        .getServicesByType(CustomAuthorityFilter.class);

    private RequestService requestService = dspace.getServiceManager().getServiceByName(RequestService.class.getName(),
        RequestService.class);

    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    private ExternalDataService externalDataService = ExternalServiceFactory.getInstance().getExternalDataService();

    // map of field key to presentation type
    protected Map<String, String> externalSource = new HashMap<String, String>();

    // punt!  this is a poor implementation..
    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    /**
     * Match a proposed value against existent DSpace item applying an optional
     * filter query to limit the scope only to specific item types
     */
    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        if (limit <= 0) {
            limit = 20;
        }

        SolrClient solr = searchService.getSolrSearchCore().getSolr();
        if (Objects.isNull(solr)) {
            log.error("unable to find solr instance");
            return new Choices(Choices.CF_UNSET);
        }

        String relationshipType = getLinkedEntityType();
        ItemAuthorityService itemAuthorityService = itemAuthorityServiceFactory.getInstance(relationshipType);
        String luceneQuery = itemAuthorityService.getSolrQuery(text);


        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(luceneQuery);
        solrQuery.setStart(start);
        solrQuery.setRows(limit);
        solrQuery.addFilterQuery("search.resourcetype:" + Item.class.getSimpleName());

        if (StringUtils.isNotBlank(relationshipType)) {
            solrQuery.addFilterQuery("relationship.type:" + relationshipType);
        }

        customAuthorityFilters.stream()
            .flatMap(caf -> caf.getFilterQueries(relationshipType).stream())
            .forEach(solrQuery::addFilterQuery);

        try {
            QueryResponse queryResponse = solr.query(solrQuery);
            List<Choice> choiceList = queryResponse.getResults()
                .stream()
                .map(doc ->  {
                    String title = ((ArrayList<String>) doc.getFieldValue("dc.title")).get(0);
                    Map<String, String> extras = ItemAuthorityUtils.buildExtra(getPluginInstanceName(), doc);
                    return new Choice((String) doc.getFieldValue("search.resourceid"),
                        title,
                        title, extras);
                }).collect(Collectors.toList());

            Choice[] results = new Choice[choiceList.size()];
            results = choiceList.toArray(results);
            long numFound = queryResponse.getResults().getNumFound();

            return new Choices(results, start, (int) numFound, Choices.CF_AMBIGUOUS,
                               numFound > (start + limit), 0);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new Choices(Choices.CF_UNSET);
        }
    }

    @Override
    public String getLabel(String key, String locale) {
        String title = key;
        if (key != null) {
            Context context = null;
            try {
                context = new Context();
                DSpaceObject dso = itemService.find(context, UUIDUtils.fromString(key));
                if (dso != null) {
                    title = dso.getName();
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                return key;
            }
        }
        return title;
    }

    @Override
    public String getLinkedEntityType() {
        return configurationService.getProperty("cris.ItemAuthority." + authorityName + ".relationshipType");
    }

    private boolean isInstitutionEntity() {
        Request currentRequest = requestService.getCurrentRequest();
        if (Objects.isNull(currentRequest)) {
            return false;
        }
        HttpServletRequest httpServletRequest = currentRequest.getHttpServletRequest();
        if (httpServletRequest == null) {
            return false;
        }
        String collection = httpServletRequest.getParameter("collection");
        if (StringUtils.isBlank(collection)) {
            return false;
        }
        Context context = Optional.ofNullable(currentRequest.getServletRequest())
            .map(rq -> (Context) rq.getAttribute("dspace.context")).orElseGet(Context::new);
        try {
            return collectionService
                .find(context, UUID.fromString(collection)).getRelationshipType().startsWith("Institution");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }

    /**
     * Return map of key to presentation
     *
     * @return
     */
    @Override
    public Map<String, String> getExternalSource() {
        // If empty, load from configuration
        if (externalSource.isEmpty()) {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_EXTERNALSOURCE_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();

                String metadata = key.substring(CHOICES_EXTERNALSOURCE_PREFIX.length());
                if (metadata == null) {
                    log.warn("Skipping invalid external source authority configuration property: " + key +
                        ": does not have schema.element.qualifier");
                    continue;
                }
                String sourceIdentifier = configurationService.getProperty(key);
                if (hasValidExternalSource(sourceIdentifier)) {
                    externalSource.put(metadata, sourceIdentifier);
                } else {
                    log.warn("Skipping invalid external source authority configuration property: " + sourceIdentifier +
                            " does not exist");
                    continue;
                }
            }
        }

        return externalSource;
    }

    private boolean hasValidExternalSource(String sourceIdentifier) {
        if (StringUtils.isNotBlank(sourceIdentifier)) {
            ExternalDataProvider externalsource = externalDataService.getExternalDataProvider(sourceIdentifier);
            return (externalsource != null);
        }
        return false;
    }
}
