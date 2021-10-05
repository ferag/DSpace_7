/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.service.impl;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.util.Util;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.pgc.exception.InvalidScopeException;
import org.dspace.pgc.service.api.PgcApiDataProviderService;
import org.dspace.pgc.solr.DSpaceSolrCoreSearch;
import org.dspace.pgc.solr.DSpaceSolrCoreServer;
import org.dspace.pgc.solr.exceptions.DSpaceSolrCoreException;
import org.dspace.pgc.utils.PgcDiscoveryQueryBuilder;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


/**
 * @author Alba Aliu
 * Implementation class of PgcApiDataProviderService, used to generate xml files based on url request
 */

public class PgcApiDataProviderServiceImpl implements PgcApiDataProviderService {
    private static final Logger log = getLogger(PgcApiDataProviderServiceImpl.class);
    private org.dspace.services.ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private PgcDiscoveryQueryBuilder queryBuilder;
    @Autowired
    CollectionService collectionService;
    @Autowired
    CommunityService communityService;
    @Autowired
    ItemService itemService;

    /**
     * Constructs the content of file xml for document found on oai core
     *
     * @param scope    is the scope that will be translated into uuid
     * @param pageable will contain offset and pageSize
     * @param query    will query to filter on search core
     * @param context  contains database connection
     * @param request  used to access the request url
     */
    @Override
    public File scopeData(String scope, Pageable pageable, String query,
                          Context context, HttpServletRequest request) throws Exception {
        //first we get the scope public name from configuration
        String pgcPublic = configurationService.getProperty("public-scope");
        // find the scope object based on id configured on pgc-api for the scope in the url
        IndexableObject scopeObject = resolveScope(context, scope, false);
        // find the discovery query for the scope_object
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
                                                            .getDiscoveryConfiguration(scopeObject);
        DiscoverResult searchResult = null;
        DiscoverQuery discoverQuery = null;
        try {
            // create discovery query
            discoverQuery = queryBuilder.buildQuery(context, scopeObject,
                                                    discoveryConfiguration, query, null, pageable);
            String oaiCondition = String.format("{!join from=item.id to=search.resourceid fromIndex=%soai}*:*",
                                                configurationService.getProperty("solr.multicorePrefix", ""));
            discoverQuery.addFilterQueries(oaiCondition);
            // search on discovery index
            searchResult = searchService.search(context, scopeObject, discoverQuery);
            try {
                // search on oai core
                SolrDocumentList documents = searchOaiCore(searchResult.getIndexableObjects(),
                                                           DSpaceSolrCoreServer.getServer());
                // generate and return xml
                //for this type  of request the type of data will be pgc-public
                return generateXml(documents, query, pageable, request, pgcPublic,
                                   searchResult.getTotalSearchResults());
            } catch (Exception e) {
                log.error(e.getMessage());
                throw e;
            }
        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            throw new IllegalArgumentException("Error while searching with Discovery: " + e.getMessage());
        }
    }

    /**
     * Constructs the content of file xml for document found on oai core for a specific item with id
     *
     * @param scope    is the scope that will be translated into uuid
     * @param id       represents the id of item to be searched on oai core
     * @param context  contains database connection
     * @param pageable will contain offset and pageSize
     * @param request  used to access the request url
     */
    @Override
    public File scopeData(String scope, String id, Context context, Pageable pageable,
                          HttpServletRequest request) throws Exception {
        // in this case is directly searched on core
        try {
            //first we get both configurations names for the scopes
            String pgc_restricted = configurationService.getProperty("restricted-scope");
            String pgc_public = configurationService.getProperty("public-scope");
            // it can be pgc-public or pgc-restricted
            HttpSession session = request.getSession();
            Object tokenScope = session.getAttribute("pgc/auth.scope");
            Object ownerId = session.getAttribute("pgc/auth.sub");
            // as it accepts requests ctivitae/id it should control the scope of the token
            //if scope is restricted, before search on core search for the owner of Person with ID (in url)
            if (tokenScope != null) {
                if (tokenScope.equals(pgc_restricted)) {
                    // search on discovery index
                    DiscoverQuery discoverQuery = new DiscoverQuery();
                    if (ownerId != null) {
                        discoverQuery.setQuery("search.resourceid: " + id + " AND cris.owner_authority:" + ownerId);
                        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
                        if (discoverResult.getIndexableObjects().size() > 0) {
                            // in this case the node restricted part will be rendered
                            tokenScope = pgc_restricted;
                        } else {
                            // the sub doesn't match the owner id of the item
                            // so the public part will be rendered
                            tokenScope = pgc_public;
                        }
                    }
                } else {
                    // if the client app is authorized and has not the scope pgc-restricted,
                    // we set the scope pgc-public
                    tokenScope = pgc_public;
                }
            } else {
                // this means that is not ctivitae case
                tokenScope = pgc_public;
            }
            // search with id that comes from url
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery("search.resourceid: " + id);
            DiscoverResult discoverResult = searchService.search(context, discoverQuery);
            if (discoverResult.getIndexableObjects().isEmpty()) {
                return null;
            }
            SolrQuery params = new SolrQuery("item.id:" + id).addField("item.compile");
            try {
                SolrDocumentList documents = DSpaceSolrCoreSearch.query(DSpaceSolrCoreServer.getServer(), params);
                // generate and return xml
                return generateXml(documents, pageable, request, tokenScope.toString(), 1L,
                                   UUID.fromString(id));
            } catch (IOException | DSpaceSolrCoreException e) {
                log.error(e.getMessage());
            }
        } catch (SolrServerException e) {
            log.error("Error while searching in Discovery", e);
            throw new IllegalArgumentException("Error while searching in Discovery: " + e.getMessage());
        }
        throw new Exception();
    }


    /**
     * Constructs the content of file xml for document found on oai core
     *
     * @param id       represents the id of indexable object
     * @param scope    is the scope that will be translated into uuid
     * @param context  contains database connection
     * @param pageable will contain offset and pageSize
     * @param request  used to access the request url
     */
    @Override
    public File scopeDataInverseRelation(String id, String scope, Context context,
                                         Pageable pageable, HttpServletRequest request) throws Exception {
        //first we get the scope public name from configuration
        String pgc_public = configurationService.getProperty("public-scope");
        IndexableObject scopeObject = resolveScope(context, id, true);
        // name of relation will be read from configuration
        String inverseRelationName = configurationService.getPropertyValue("pgc-api." +
                                                                               scope + ".inverseRelation").toString();
        DiscoveryConfiguration discoveryConfiguration = searchConfigurationService
                                                            .getDiscoveryConfigurationByName(inverseRelationName);
        try {
            DiscoverQuery discoverQuery =
                queryBuilder.buildQuery(context, scopeObject, discoveryConfiguration, null, null, pageable);
            DiscoverResult searchResult = searchService.search(context, discoverQuery);
            try {
                SolrDocumentList documents = searchOaiCore(searchResult.getIndexableObjects(),
                                                           DSpaceSolrCoreServer.getServer());
                // for the moment we render only public part, it can be changed latter
                return generateXml(documents, pageable, request, pgc_public, searchResult.getTotalSearchResults());
                //convert this list in xml
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } catch (SearchServiceException e) {
            log.error("Error while searching with Discovery", e);
            throw new IllegalArgumentException("Error while searching with Discovery: " + e.getMessage());
        }
        throw new Exception();
    }


    /**
     * Constructs the IndexableObject for e specific scope and context, based on configuration of module
     *
     * @param scope   is the scope that will be translated into uuid
     * @param context contains database connection
     * @param related determines if the discoveryConfiguration will be generated based on inverse relation
     */
    private IndexableObject resolveScope(Context context, String scope, boolean related) {
        IndexableObject scopeObj = null;
        String id;
        if (!related) {
            id = configurationService.getPropertyValue("pgc-api." + scope + ".id").toString();
        } else {
            id = scope;
        }
        if (StringUtils.isNotBlank(scope)) {
            try {
                UUID uuid = UUID.fromString(id);
                scopeObj = new IndexableCommunity(communityService.find(context, uuid));
                if (scopeObj.getIndexedObject() == null) {
                    scopeObj = new IndexableCollection(collectionService.find(context, uuid));
                }
                if (scopeObj.getIndexedObject() == null) {
                    scopeObj = new IndexableItem(itemService.find(context, uuid));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("The given scope string " + StringUtils.trimToEmpty(scope) + " is not a UUID", ex);
            } catch (SQLException ex) {
                log.warn(
                        "Unable to retrieve DSpace Object with ID " +
                            StringUtils.trimToEmpty(scope) + " from the database",
                        ex);
            }
        }
        return scopeObj;
    }

    /**
     * Filters on the oai core and returns a list of solrDocuments
     *
     * @param indexableObjects the list of of items to be retrieved in oai core
     * @param solrClient solr client to be used
     */
    private SolrDocumentList searchOaiCore(final List<IndexableObject> indexableObjects,
                                           final SolrClient solrClient) throws Exception {
        final SolrDocumentList result = new SolrDocumentList();

        long numFound = 0;
        for (IndexableObject io : indexableObjects) {
            SolrQuery params = new SolrQuery("item.id:" + io.getID().toString()).addField("item.compile");
            try {
                SolrDocumentList documents = DSpaceSolrCoreSearch.query(solrClient, params);
                numFound += documents.size();
                result.addAll(documents);
            } catch (IOException | DSpaceSolrCoreException e) {
                log.error(e.getMessage());
                throw new Exception("Error while searching in OAI: " + e.getMessage());
            }
        }
        result.setNumFound(numFound);
        return result;
    }


    /**
     * Creates OaiXmlItemCerifOpenaireResponse object composed by Header and Cerif
     *  @param solrDocuments represents the list of solr documents returned by filtering on oai core
     */

    private Document composeCerifPayload(SolrDocumentList solrDocuments) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            Document newDoc = db.newDocument();
            Element root = newDoc.createElement("Search-API");
            appendCerifData(solrDocuments, db, newDoc, root);
            newDoc.appendChild(root);
            return newDoc;
        } catch (ParserConfigurationException | IOException | SAXException error) {
            log.error(error);
        }
        return null;
    }

    private void appendCerifData(final SolrDocumentList solrDocuments, final DocumentBuilder db, final Document newDoc,
                                 final Element root) throws SAXException, IOException {
        Element cerif = newDoc.createElement("Cerif");
        if (Optional.ofNullable(solrDocuments).map(sd -> sd.size() > 0).orElse(false)) {
            Iterator<SolrDocument> solrDocumentIterator = solrDocuments.iterator();
            while (solrDocumentIterator.hasNext()) {
                SolrDocument document = solrDocumentIterator.next();
                Document oldDoc = db.parse(new ByteArrayInputStream(document.get("item.compile").toString()
                                                                            .replace("<![CDATA[", "")
                                                                            .replace("]]>", "")
                                                                            .getBytes(StandardCharsets.UTF_8)));
                Node oldRoot = oldDoc.getDocumentElement();

                cerif.appendChild(newDoc.importNode(oldRoot, true));
            }
        }
        root.appendChild(cerif);
    }

    private File generateXml(SolrDocumentList documents, Pageable pageable,
                             HttpServletRequest request, String scopeTypeToken, long totalSearchResults)
        throws IOException, TransformerException {
        return generateXml(documents, "", pageable, request, scopeTypeToken,
                           totalSearchResults);
    }

    /**
     * Creates File content for xml
     * @param documents represent the list of oai documents to attach as string to the xml
     * @param pageable  will contain offset and pageSize
     * @param request   used to access the request url
     * @param totalResults total results found by search query
     * @param relatedEntity UUID of entity (CtiVitae) from where query has been originated
     */
    private File generateXml(SolrDocumentList documents, Pageable pageable,
                             HttpServletRequest request, String scopeTypeToken,
                             long totalResults,
                             UUID relatedEntity) throws IOException, TransformerException {

        return generateXml(documents, "", pageable, request, scopeTypeToken, relatedEntity, totalResults);
    }

    /**
     * Creates File content for xml
     * @param documents represent the list of oai documents to attach as string to the xml
     * @param query original query performed
     * @param pageable  will contain offset and pageSize
     * @param request   used to access the request url
     * @param totalResults total results found by search query
     */
    private File generateXml(SolrDocumentList documents,
                             String query, Pageable pageable,
                             HttpServletRequest request,
                             String scopeTypeToken,
                             Long totalResults) throws IOException, TransformerException, InvalidScopeException {

        return generateXml(documents, query, pageable, request, scopeTypeToken, null, totalResults);
    }

    /**
     * Creates File content for xml
     * @param documents represent the list of oai documents to attach as string to the xml
     * @param query original query performed
     * @param pageable  will contain offset and pageSize
     * @param request   used to access the request url
     * @param relatedEntity UUID of CVEntity query is referred to
     * @param totalResults total results found by search query
     */
    private File generateXml(SolrDocumentList documents,
                             String query, Pageable pageable,
                             HttpServletRequest request,
                             String scopeTypeToken,
                             UUID relatedEntity,
                             Long totalResults) throws IOException, TransformerException, InvalidScopeException {
        try {
            String nodeNameForScopeToken = configurationService.getPropertyValue(scopeTypeToken).toString();
            if (nodeNameForScopeToken != null) {
                // generate an unique id
                String uuid = UUID.randomUUID().toString();

                String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" +
                                    File.separator + "pgc-api" + File.separator + "template";


                StringBuilder url = new StringBuilder(request.getRequestURL().toString());
                if (StringUtils.isNotBlank(query) || Objects.isNull(relatedEntity)) {
                    url.append("?");
                    if (StringUtils.isNotBlank(query)) {
                        url
                            .append("query=")
                            .append(query)
                            .append("&");
                    }
                    url.append("page=")
                        .append(pageable.getPageNumber())
                        .append("&size=")
                        .append(pageable.getPageSize());
                    if (pageable.getSort().isSorted()) {
                        url.append("&sort=")
                            .append(sort(pageable));
                    }
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();

                Source xsltSource = new StreamSource(
                    new File(parent, configurationService.getProperty("pgc-api.xsl")));
                Transformer transformer = transformerFactory.newTransformer(xsltSource);
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
                transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

                transformer.setParameter("node", nodeNameForScopeToken);

                transformer.setParameter("source", configurationService.getProperty("pgc-api.path"));
                transformer.setParameter("apiVersion", Util.getSourceVersion());
                transformer.setParameter("page", Integer.toString(pageable.getPageNumber()));
                transformer.setParameter("size", Integer.toString(pageable.getPageSize()));
                transformer.setParameter("resultsInPage", Optional.ofNullable(documents)
                                        .map(d -> d.getNumFound()).map(l -> Long.toString(l)).orElse("0"));
                transformer.setParameter("totalResults", Long.toString(totalResults));
                transformer.setParameter("searchQuery", url.toString());


                transformer.setParameter("sourceDatabase", configurationService.getProperty("dspace.server.url"));
                transformer.setParameter("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));


                Document document = composeCerifPayload(documents);
                StringWriter stringWriter = domStringWriter(document, transformerFactory);
                Source streamSource = new StreamSource(
                    IOUtils.toInputStream(stringWriter.toString(), Charset.defaultCharset()));
                File tempFileXml = File.createTempFile(uuid, ".xml");

                transformer.transform(streamSource, new StreamResult(tempFileXml));

                // return file that holds the xml
                return tempFileXml;
            } else {
                log.error("No configuration found for scope " + scopeTypeToken);
                throw new InvalidScopeException("Token's scope is invalid");
            }
        } catch (IOException | TransformerException e) {
            throw e;
        }
    }

    private String sort(Pageable pageable) {
        return Arrays.stream(pageable.getSort().toString().split(":"))
            .map(String::trim)
            .map(String::toLowerCase)
            .collect(Collectors.joining(","));
    }

    private StringWriter domStringWriter(final Document document, final TransformerFactory transformerFactory)
        throws TransformerException {
        StringWriter stringWriter = new StringWriter();
        transformerFactory.newTransformer().transform(
            new DOMSource(document), new StreamResult(stringWriter));
        return stringWriter;
    }
}
