/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.utils;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryRelatedItemConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFunctionConfiguration;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.pgc.exception.DSpacePgcBadRequestException;
import org.dspace.pgc.exception.InvalidPgcSearchRequestException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @author Alba Aliu
 */

public class PgcDiscoveryQueryBuilder implements InitializingBean {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(PgcDiscoveryQueryBuilder.class);
    private  static  SearchService searchService =
        DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(null, SearchService.class);
    @Autowired
    private List<IndexFactory> indexableFactories;
    private int pageSizeLimit;
    @Override
    public void afterPropertiesSet() throws Exception {
        // set the default number of items to be shown
        pageSizeLimit = 10;
    }
    /**
     * Build a discovery query
     *
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param query                     the query string for this discovery query
     * @param dsoType                   only include search results with this type
     * @param page                      the pageable for this discovery query
     */
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query,
                                    String dsoType, Pageable page)
            throws DSpacePgcBadRequestException {

        List<String> dsoTypes = dsoType != null ? Collections.singletonList(dsoType) : Collections.emptyList();
        return buildQueryListDso(context, scope, discoveryConfiguration, query, dsoTypes, page);
    }
    /**
     * Build a discovery query
     *
     * @param context                   the DSpace context
     * @param scope                     the scope for this discovery query
     * @param discoveryConfiguration    the discovery configuration for this discovery query
     * @param query                     the query string for this discovery query
     * @param dsoTypes                  only include search results with one of these types
     * @param page                      the pageable for this discovery query
     */
    public DiscoverQuery buildQueryListDso(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<String> dsoTypes, Pageable page)
            throws DSpacePgcBadRequestException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, dsoTypes, scope);

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting
        configurePagination(page, queryArgs);
        configureSorting(page, queryArgs, discoveryConfiguration.getSearchSortConfiguration(), scope);

        addDiscoveryHitHighlightFields(discoveryConfiguration, queryArgs);

        queryArgs.setScopeObject(scope);
        return queryArgs;
    }
    private void addDiscoveryHitHighlightFields(DiscoveryConfiguration discoveryConfiguration,
                                                DiscoverQuery queryArgs) {
        if (discoveryConfiguration.getHitHighlightingConfiguration() != null) {
            List<DiscoveryHitHighlightFieldConfiguration> metadataFields = discoveryConfiguration
                    .getHitHighlightingConfiguration().getMetadataFields();
            for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : metadataFields) {
                queryArgs.addHitHighlightingField(
                        new DiscoverHitHighlightingField(fieldConfiguration.getField(), fieldConfiguration.getMaxSize(),
                                fieldConfiguration.getSnippets()));
            }
        }
    }
    private void configurePaginationForFacet(Pageable page, DiscoverQuery queryArgs) {
        if (page != null && queryArgs.getFacetFields().size() == 1) {
            queryArgs.getFacetFields().get(0).setOffset((int) page.getOffset());
        }
    }

    private void fillFacetIntoQueryArgs(Context context, IndexableObject scope, String prefix,
                                        DiscoverQuery queryArgs, DiscoverySearchFilterFacet facet, final int pageSize) {
        if (facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            try {
                FacetYearRange facetYearRange =
                        searchService.getFacetYearRange(context, scope, facet, queryArgs.getFilterQueries(), queryArgs);

                queryArgs.addYearRangeFacet(facet, facetYearRange);

            } catch (Exception e) {
                log.error(LogHelper.getHeader(context, "Error in Discovery while setting up date facet range",
                        "date facet: " + facet), e);
            }

        } else {

            //Add one to our facet limit to make sure that if we have more then the shown facets that we show our
            // "show more" url
            int facetLimit = pageSize + 1;
            //This should take care of the sorting for us
            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit,
                    facet.getSortOrderSidebar(), StringUtils.trimToNull(prefix),
                    facet.exposeMore(), facet.exposeMissing(), facet.exposeTotalElements(), facet.fillDateGaps(),
                    facet.inverseDirection()));
        }
    }
    private DiscoverQuery buildCommonDiscoverQuery(Context context, DiscoveryConfiguration discoveryConfiguration,
                                                   String query, List<String> dsoTypes,
                                                   IndexableObject scope)
            throws DSpacePgcBadRequestException {
        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration, scope);
        //Set search query
        if (StringUtils.isNotBlank(query)) {
            queryArgs.setQuery(query);
        }
        //Limit results to DSO types
        if (isNotEmpty(dsoTypes)) {
            dsoTypes.stream()
                    .map(this::getDsoType)
                    .forEach(queryArgs::addDSpaceObjectFilter);
        }

        return queryArgs;
    }
    private DiscoverQuery buildBaseQueryForConfiguration(
            DiscoveryConfiguration discoveryConfiguration, IndexableObject scope) {
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.setDiscoveryConfigurationName(discoveryConfiguration.getId());

        String[] queryArray = discoveryConfiguration.getDefaultFilterQueries()
                .toArray(
                        new String[discoveryConfiguration.getDefaultFilterQueries()
                                .size()]);

        if (discoveryConfiguration != null &&
                discoveryConfiguration instanceof DiscoveryRelatedItemConfiguration) {
            if (queryArray != null) {
                for ( int i = 0; i < queryArray.length; i++ ) {
                    queryArray[i] = MessageFormat.format(queryArray[i], scope.getID());
                }
            } else {
                log.warn("you are trying to set queries parameters on an empty queries list");
            }
        }

        queryArgs.addFilterQueries(queryArray);
        return queryArgs;
    }
    private void configureSorting(Pageable page, DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration,
                                  final IndexableObject scope) throws DSpacePgcBadRequestException {
        String sortBy = null;
        String sortOrder = null;

        //Read the Pageable object if there is one
        if (page != null) {
            Sort sort = page.getSort();
            if (sort != null && sort.iterator().hasNext()) {
                Sort.Order order = sort.iterator().next();
                sortBy = order.getProperty();
                sortOrder = order.getDirection().name();
            }
        }

        if (StringUtils.isNotBlank(sortBy) && !isConfigured(sortBy, searchSortConfiguration)) {
            throw new InvalidPgcSearchRequestException(
                    "The field: " + sortBy + "is not configured for the configuration!");
        }

        //Load defaults if we did not receive values
        if (sortBy == null) {
            sortBy = getDefaultSortField(searchSortConfiguration);
        }
        if (sortOrder == null) {
            sortOrder = getDefaultSortDirection(searchSortConfiguration, sortOrder);
        }

        //Update Discovery query
        DiscoverySortFieldConfiguration sortFieldConfiguration = searchSortConfiguration
                .getSortFieldConfiguration(sortBy);

        if (sortFieldConfiguration != null) {
            String sortField;

            if (DiscoverySortFunctionConfiguration.SORT_FUNCTION.equals(sortFieldConfiguration.getType())) {
                sortField = MessageFormat.format(
                        ((DiscoverySortFunctionConfiguration) sortFieldConfiguration).getFunction(scope.getID()),
                        scope.getID());
            } else {
                sortField = searchService
                        .toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
            }


            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
            } else {
                throw new DSpacePgcBadRequestException(sortOrder + " is not a valid sort order");
            }

        } else {
            throw new DSpacePgcBadRequestException(sortBy + " is not a valid sort field");
        }
    }
    private boolean isConfigured(String sortBy, DiscoverySortConfiguration searchSortConfiguration) {
        return Objects.nonNull(searchSortConfiguration.getSortFieldConfiguration(sortBy));
    }
    private String getDefaultSortDirection(DiscoverySortConfiguration searchSortConfiguration, String sortOrder) {
        if (Objects.nonNull(searchSortConfiguration.getSortFields()) &&
                !searchSortConfiguration.getSortFields().isEmpty()) {
            sortOrder = searchSortConfiguration.getSortFields().get(0).getDefaultSortOrder().name();
        }
        return sortOrder;
    }

    private String getDefaultSortField(DiscoverySortConfiguration searchSortConfiguration) {
        String sortBy;// Attempt to find the default one, if none found we use SCORE
        sortBy = "score";
        if (Objects.nonNull(searchSortConfiguration.getSortFields()) &&
                !searchSortConfiguration.getSortFields().isEmpty()) {
            DiscoverySortFieldConfiguration defaultSort = searchSortConfiguration.getSortFields().get(0);
            if (StringUtils.isBlank(defaultSort.getMetadataField())) {
                return sortBy;
            }
            sortBy = defaultSort.getMetadataField();
        }
        return sortBy;
    }

    private void configurePagination(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setMaxResults(Math.min(pageSizeLimit, page.getPageSize()));
            queryArgs.setStart(Math.toIntExact(page.getOffset()));
        } else {
            queryArgs.setMaxResults(pageSizeLimit);
            queryArgs.setStart(0);
        }
    }

    private String getDsoType(String dsoType) throws DSpacePgcBadRequestException {
        for (IndexFactory indexFactory : indexableFactories) {
            if (StringUtils.equalsIgnoreCase(indexFactory.getType(), dsoType)) {
                return indexFactory.getType();
            }
        }
        throw new DSpacePgcBadRequestException(dsoType + " is not a valid DSpace Object type");
    }

    private DiscoverQuery addFaceting(Context context, IndexableObject scope, DiscoverQuery queryArgs,
                                      DiscoveryConfiguration discoveryConfiguration) {

        List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        log.debug("facets for configuration " + discoveryConfiguration.getId() + ": " + (facets != null ? facets
                .size() : null));

        if (facets != null) {
            queryArgs.setFacetMinCount(1);

            /** enable faceting of search results */
            for (DiscoverySearchFilterFacet facet : facets) {
                fillFacetIntoQueryArgs(context, scope, null, queryArgs, facet, facet.getFacetLimit());
            }
        }

        return queryArgs;
    }
    // only for tests
    public void setPageSizeLimit(int pageSizeLimit) {
        this.pageSizeLimit = pageSizeLimit;
    }
}
