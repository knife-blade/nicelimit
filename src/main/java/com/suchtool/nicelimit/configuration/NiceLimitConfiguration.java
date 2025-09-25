package com.suchtool.nicelimit.configuration;


import com.suchtool.nicelimit.property.NiceLimitFilterProperty;
import com.suchtool.nicelimit.runner.NiceLimitApplicationRunner;
import com.suchtool.nicelimit.listener.NiceLimitEnvironmentChangeEventListener;
import com.suchtool.nicelimit.filter.NiceLimitFilter;
import com.suchtool.nicelimit.handler.NiceLimitHandler;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

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

    @Bean(name = "com.suchtool.nicelimit.niceLimitFilterRegistration")
    @ConditionalOnProperty(name = "suchtool.nicelimit.type", havingValue = "SERVLET")
    public FilterRegistrationBean<?> filterRegistrationBean(NiceLimitHandler niceLimitHandler,
                                                            NiceLimitProperty niceLimitProperty) {
        FilterRegistrationBean<?> filterRegistrationBean = new FilterRegistrationBean<>(
                new NiceLimitFilter(niceLimitHandler));
        NiceLimitFilterProperty filter = niceLimitProperty.getFilter();
        if (filter == null) {
            filter = new NiceLimitFilterProperty();
        }

        List<String> filterPatternList = filter.getFilterPattern();
        if (!CollectionUtils.isEmpty(filterPatternList)) {
            for (String pattern : filterPatternList) {
                filterRegistrationBean.addUrlPatterns(pattern);
            }
        }
        filterRegistrationBean.setName(filter.getFilterName());
        if (filter.getFilterOrder() != null) {
            // 过滤器执行顺序（决定doFilter顺序，不决定init和destroy顺序）
            filterRegistrationBean.setOrder(filter.getFilterOrder());
        }

        return filterRegistrationBean;
    }
}
