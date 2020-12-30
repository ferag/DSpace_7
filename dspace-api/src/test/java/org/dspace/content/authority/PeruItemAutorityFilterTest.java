/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PeruItemAutorityFilter}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class PeruItemAutorityFilterTest {

    private PeruItemAutorityFilter peruItemAutorityFilter;
    private RequestService requestService = mock(RequestService.class);
    private CollectionService collectionService = mock(CollectionService.class);
    private HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private Context context = mock(Context.class);

    @Before
    public void setUp() throws Exception {
        peruItemAutorityFilter = new PeruItemAutorityFilter(requestService, collectionService);
        Request currentRequest = mock(Request.class);
        when(requestService.getCurrentRequest()).thenReturn(currentRequest);
        when(currentRequest.getHttpServletRequest()).thenReturn(httpServletRequest);

        ServletRequest servletRequest = mock(ServletRequest.class);
        when(currentRequest.getServletRequest()).thenReturn(servletRequest);
        when(currentRequest.getServletRequest().getAttribute("dspace.context")).thenReturn(context);
    }

    @Test
    public void noCollectionIdPassed() {
        when(httpServletRequest.getParameter("collection")).thenReturn(null);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();
        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void blankCollectionIdPassed() {
        when(httpServletRequest.getParameter("collection")).thenReturn("   ");

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();
        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void exceptionWhileFindingCollection() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());
        doThrow(new SQLException("SqlException"))
            .when(collectionService).find(context, collectionId);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void exceptionWhileFindingCollectionCommunities() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());

        Collection collection = mock(Collection.class);
        when(collectionService.find(context, collectionId))
            .thenReturn(collection);

        doThrow(new SQLException("SqlException"))
            .when(collection).getCommunities();

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void nullCommunities() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());

        Collection collection = mock(Collection.class);
        when(collection.getCommunities()).thenReturn(null);
        when(collectionService.find(context, collectionId))
            .thenReturn(collection);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void emptyCommunities() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());

        Collection collection = collectionWithCommunities(Collections.emptyList());
        when(collectionService.find(context, collectionId))
            .thenReturn(collection);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void manyCommunities() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());

        Collection collection = collectionWithCommunities(Arrays.asList(
            UUID.randomUUID(),
            UUID.randomUUID()));
        when(collectionService.find(context, collectionId))
            .thenReturn(collection);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void communityFound() throws SQLException {
        UUID collectionId = UUID.randomUUID();

        when(httpServletRequest.getParameter("collection"))
            .thenReturn(collectionId.toString());

        UUID communityId = UUID.randomUUID();
        Collection collection = collectionWithCommunities(
            Collections.singletonList(communityId));
        when(collectionService.find(context, collectionId))
            .thenReturn(collection);

        List<String> filterQueries = peruItemAutorityFilter.createFilterQueries();

        assertThat(filterQueries, is(Collections.singletonList("location.comm:" + communityId)));
    }

    private Collection collectionWithCommunities(List<UUID> communityIds) throws SQLException {

        Collection collection = mock(Collection.class);
        List<Community> communities = communityIds.stream()
            .map(id -> community(id))
            .collect(Collectors.toList());
        when(collection.getCommunities())
            .thenReturn(communities);

        return collection;
    }

    private Community community(UUID id) {
        Community community = mock(Community.class);
        when(community.getID()).thenReturn(id);
        return community;
    }
}