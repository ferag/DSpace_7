/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisMetricsRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage CrisMetrics Rest object
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(CrisMetricsRest.CATEGORY + "." + CrisMetricsRest.NAME)
public class CrisMetricsRestRepository extends DSpaceRestRepository<CrisMetricsRest, Integer>
                                       implements ReloadableEntityObjectRepository<CrisMetrics, Integer> {

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    @PreAuthorize("hasPermission(#id, 'METRIC', 'READ')")
    public CrisMetricsRest findOne(Context context, Integer id) {
        CrisMetrics crisMetrics = null;
        try {
            crisMetrics = crisMetricsService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (crisMetrics == null) {
            return null;
        }
        return converter.toRest(crisMetrics, utils.obtainProjection());
    }

    @Override
    public Page<CrisMetricsRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(CrisMetricsRest.NAME, "findAll");
    }

    @Override
    public CrisMetrics findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return crisMetricsService.find(context, id);
    }

    @Override
    public Class<CrisMetricsRest> getDomainClass() {
        return CrisMetricsRest.class;
    }

    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }
}