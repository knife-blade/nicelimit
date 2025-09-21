package com.suchtool.nicelimit.configuration;


import com.suchtool.nicelimit.filter.NiceLimitFilter;
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
public class NiceLimitConfiguration  {

    @Bean(name = "com.suchtool.nicelimit.niceLimitProperty")
    @ConfigurationProperties(prefix = "suchtool.nicelimit")
    public NiceLimitProperty niceLimitProperty() {
        return new NiceLimitProperty();
    }

    @Bean(name = "com.suchtool.nicelimit.niceLimitFilterRegistration")
    public FilterRegistrationBean<?> filterRegistrationBean(NiceLimitProperty niceLimitProperty,
                                                            RedissonClient redissonClient) {
        FilterRegistrationBean<?> filterRegistrationBean = new FilterRegistrationBean<>(
                new NiceLimitFilter(niceLimitProperty, redissonClient));
        List<String> filterPatternList = niceLimitProperty.getFilterPattern();
        if (!CollectionUtils.isEmpty(filterPatternList)) {
            for (String pattern : filterPatternList) {
                filterRegistrationBean.addUrlPatterns(pattern);
            }
        }
        filterRegistrationBean.setName(niceLimitProperty.getFilterName());
        //过滤器执行顺序（决定doFilter顺序，不决定init和destroy顺序）
        filterRegistrationBean.setOrder(niceLimitProperty.getFilterOrder());
        return filterRegistrationBean;
    }
}
