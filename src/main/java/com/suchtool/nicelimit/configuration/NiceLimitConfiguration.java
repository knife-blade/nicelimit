package com.suchtool.nicelimit.configuration;


import com.suchtool.nicelimit.filter.NiceLimitFilterJakarta;
import com.suchtool.nicelimit.filter.NiceLimitFilterJavax;
import com.suchtool.nicelimit.handler.NiceLimitHandler;
import com.suchtool.nicelimit.listener.NiceLimitEnvironmentChangeEventListener;
import com.suchtool.nicelimit.property.NiceLimitFilterProperty;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import com.suchtool.nicelimit.runner.NiceLimitApplicationRunner;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

@Configuration(value = "com.suthtool.nicelimit.niceLimitConfiguration", proxyBeanMethods = false)
@ConditionalOnProperty(name = "suchtool.nicelimit.inject", havingValue = "true")
public class NiceLimitConfiguration {

    @Bean(name = "com.suchtool.nicelimit.niceLimitProperty")
    @ConfigurationProperties(prefix = "suchtool.nicelimit")
    public NiceLimitProperty niceLimitProperty() {
        return new NiceLimitProperty();
    }

    @Bean(name = "com.suchtool.nicelimit.niceLimitEnvironmentChangeEventListener")
    public NiceLimitEnvironmentChangeEventListener niceLimitEnvironmentChangeEventListener(
            NiceLimitHandler niceLimitHandler) {
        return new NiceLimitEnvironmentChangeEventListener(niceLimitHandler);
    }

    @Bean(name = "com.suchtool.nicelimit.niceLimitHandler")
    public NiceLimitHandler niceLimitHandler(NiceLimitProperty niceLimitProperty,
                                             RedissonClient redissonClient) {
        return new NiceLimitHandler(niceLimitProperty, redissonClient);
    }

    @Bean(name = "com.suchtool.nicelimit.niceLimitApplicationRunner")
    public NiceLimitApplicationRunner niceLimitApplicationRunner(
            NiceLimitHandler niceLimitHandler) {
        return new NiceLimitApplicationRunner(niceLimitHandler);
    }

    /**
     * SpringBoot2
     */
    @Configuration(value = "com.suchtool.nicelimit.niceLimitFilterJavaxConfiguration", proxyBeanMethods = false)
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(name = "suchtoolnicelimit.type", havingValue = "SERVLET")
    protected static class NiceLimitFilterJavaxConfiguration {
        @Bean(name = "com.suchtool.nicelimit.niceLimitFilterJavax")
        public NiceLimitFilterJavax niceLimitFilterJavax(NiceLimitHandler niceLimitHandler,
                                                         NiceLimitProperty niceLimitProperty) {
            NiceLimitFilterProperty filter = niceLimitProperty.getFilter();
            if (filter == null) {
                filter = new NiceLimitFilterProperty();
            }

            return new NiceLimitFilterJavax(niceLimitHandler, filter.getFilterOrder());
        }
    }

    /**
     * SpringBoot3
     */
    @Configuration(value = "com.suchtool.nicelimit.niceLimitFilterJakartaConfiguration", proxyBeanMethods = false)
    @ConditionalOnClass(jakarta.servlet.Filter.class)
    @ConditionalOnProperty(name = "suchtoolnicelimit.type", havingValue = "SERVLET")
    protected static class NiceLimitFilterJakartaConfiguration {
        @Bean(name = "com.suchtool.nicelimit.niceLimitFilterJakarta")
        public NiceLimitFilterJakarta niceLimitFilterJakarta(NiceLimitHandler niceLimitHandler,
                                                             NiceLimitProperty niceLimitProperty) {
            NiceLimitFilterProperty filter = niceLimitProperty.getFilter();
            if (filter == null) {
                filter = new NiceLimitFilterProperty();
            }

            return new NiceLimitFilterJakarta(niceLimitHandler, filter.getFilterOrder());
        }
    }
}
