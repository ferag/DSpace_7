/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    @Autowired
    private BeanFactory context;

    private DefaultMethodSecurityExpressionHandler expressionHandler;

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        this.expressionHandler = new DefaultMethodSecurityExpressionHandler();
        this.expressionHandler.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        return expressionHandler;
    }

    @Override
    public void afterSingletonsInstantiated() {
        getSingleBeanOrNull(PermissionEvaluator.class).ifPresent(this.expressionHandler::setPermissionEvaluator);
    }

    private <T> Optional<T> getSingleBeanOrNull(Class<T> type) {
        try {
            return Optional.of(context.getBean(type));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }
}
