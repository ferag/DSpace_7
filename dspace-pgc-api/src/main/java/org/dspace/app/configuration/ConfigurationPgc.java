/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;

import static java.lang.Integer.MAX_VALUE;

import org.dspace.pgc.app.PgcBasicConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


@Configuration
@Import(PgcBasicConfiguration.class)
@ComponentScan("org.dspace.pgc.controller")
public class ConfigurationPgc extends WebMvcConfigurerAdapter {
    @Value("${pgc-api.path:pgc-api}")
    private String pgcPath;
    @Override

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + pgcPath + "/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(MAX_VALUE);
    }
}
