/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.pgc.app;

import org.dspace.app.configuration.ThreeLeggedTokenFilter;
import org.dspace.app.configuration.TwoLeggedTokenFilter;
import org.dspace.pgc.service.api.PgcApiDataProviderService;
import org.dspace.pgc.service.api.contexts.PgcContextService;
import org.dspace.pgc.service.impl.PgcApiDataProviderServiceImpl;
import org.dspace.pgc.service.impl.contexts.DSpaceContextServicePgc;
import org.dspace.pgc.utils.PgcDiscoveryQueryBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alba Aliu
 *
 * Creates the necessary beans to be used by the module
 */

@Configuration
public class PgcBasicConfiguration {
    @Bean
    public PgcContextService pgcContextService() {
        return new DSpaceContextServicePgc();
    }
    @Bean
    public PgcApiDataProviderService pgcApiDataProvider() {
        return new PgcApiDataProviderServiceImpl();
    }
    @Bean
    public PgcDiscoveryQueryBuilder pgcDiscoveryQueryBuilder() {
        return new PgcDiscoveryQueryBuilder();
    }
    @Bean
    public FilterRegistrationBean<TwoLeggedTokenFilter> twoLeggedFilter() {
        FilterRegistrationBean<TwoLeggedTokenFilter> registrationBeanfilter
                = new FilterRegistrationBean<>();
        registrationBeanfilter.setFilter(new TwoLeggedTokenFilter());
        String[] list = {
            "/pgc-api/human-resources/*",
            "/pgc-api/people/*",
            "/pgc-api/global/*",
            "/pgc-api/research-outputs/*",
            "/pgc-api/publications/*",
            "/pgc-api/patents/*",
            "/pgc-api/institutions/*",
            "/pgc-api/orgunits/*",
            "/pgc-api/projects-fundings/*",
            "/pgc-api/projects/*",
            "/pgc-api/fundings/*",
            "/pgc-api/infrastructures/*",
            "/pgc-api/equipments/*",
        };
        registrationBeanfilter.addUrlPatterns(list);
        return registrationBeanfilter; }
    @Bean
    public FilterRegistrationBean<ThreeLeggedTokenFilter> threeLeggedFilter() {
        FilterRegistrationBean<ThreeLeggedTokenFilter> registrationBean
                = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ThreeLeggedTokenFilter());
        registrationBean.addUrlPatterns("/pgc-api/ctivitae/*");
        return registrationBean;
    }
}
