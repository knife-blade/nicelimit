package com.suchtool.nicelimit.configuration;


import com.suchtool.nicelimit.filter.NiceLimitApplicationRunner;
import com.suchtool.nicelimit.filter.NiceLimitEnvironmentChangeEventListener;
import com.suchtool.nicelimit.filter.NiceLimitFilter;
import com.suchtool.nicelimit.filter.NiceLimitHandler;
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
    public FilterRegistrationBean<?> filterRegistrationBean(NiceLimitHandler niceLimitHandler,
                                                            NiceLimitProperty niceLimitProperty) {
        FilterRegistrationBean<?> filterRegistrationBean = new FilterRegistrationBean<>(
                new NiceLimitFilter(niceLimitHandler));
        List<String> filterPatternList = niceLimitProperty.getFilterPattern();
        if (!CollectionUtils.isEmpty(filterPatternList)) {
            for (String pattern : filterPatternList) {
                filterRegistrationBean.addUrlPatterns(pattern);
            }
        }
        filterRegistrationBean.setName(niceLimitProperty.getFilterName());
        if (niceLimitProperty.getFilterOrder() != null) {
            // 过滤器执行顺序（决定doFilter顺序，不决定init和destroy顺序）
            filterRegistrationBean.setOrder(niceLimitProperty.getFilterOrder());
        }

        return filterRegistrationBean;
    }
}
