/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.profile.service.AfterImportAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterImportAction} that updates
 * Solr documents of Projects, Patents and Publications
 * related to the Person with new created item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateRelatedEntitiesAction implements AfterImportAction {

    private static final Logger log = LogManager.getLogger(UpdateRelatedEntitiesAction.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    private List<String> discoveryKeys;

    @Override
    public void applyTo(Context context, Item item, ExternalDataObject externalDataObject)
            throws SQLException, AuthorizeException {
        Map<String, DiscoveryConfiguration> name2discoveryConfiguration = searchConfigurationService.getMap();
        for (String discoveryName : discoveryKeys) {
            DiscoveryConfiguration discoveryConfiguration = name2discoveryConfiguration.get(discoveryName);
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
            discoverQuery.setDiscoveryConfigurationName(discoveryConfiguration.getId());
            for (String defaultFilterQuery : discoveryConfiguration.getDefaultFilterQueries()) {
                discoverQuery.addFilterQueries(MessageFormat.format(defaultFilterQuery, externalDataObject.getId()));
            }
            updateSolrDocuments(new DiscoverResultIterator<Item, UUID>(context, discoverQuery), item, context);
        }
    }

    private void updateSolrDocuments(Iterator<Item> items, Item cvItem, Context context) {
        while (items.hasNext()) {
            Item item = items.next();
            UpdateRequest req = new UpdateRequest();
            SolrClient solrClient = searchService.getSolrSearchCore().getSolr();
            StringBuilder uniqueID = new StringBuilder("Item-");
            uniqueID.append(item.getID());
            try {
                SolrInputDocument solrInDoc = new SolrInputDocument();
                solrInDoc.addField(SearchUtils.RESOURCE_UNIQUE_ID, uniqueID.toString());
                Map<String, Object> title = Collections.singletonMap("add", context.getCurrentUser().getFullName());
                Map<String, Object> id = Collections.singletonMap("add", cvItem.getID().toString());
                solrInDoc.addField("perucris.ctivitae.owner", title);
                solrInDoc.addField("perucris.ctivitae.owner_authority", id);
                req.add(solrInDoc);
                solrClient.request(req);
                solrClient.commit();
            } catch (SolrServerException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public List<String> getDiscoveryKeys() {
        return discoveryKeys;
    }

    public void setDiscoveryKeys(List<String> discoveryKeys) {
        this.discoveryKeys = discoveryKeys;
    }

}