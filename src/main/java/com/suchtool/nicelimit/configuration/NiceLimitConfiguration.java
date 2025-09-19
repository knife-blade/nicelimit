package com.suchtool.nicelimit.configuration;


import com.suchtool.nicelimit.annotation.EnableNiceLimit;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

@Configuration(value = "com.suthtool.nicelimit.niceLimitConfiguration", proxyBeanMethods = false)
public class NiceLimitConfiguration implements ImportAware {
    @Nullable
    protected AnnotationAttributes enableNiceLimit;

    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableNiceLimit = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableNiceLimit.class.getName(), false));
    }

    @Bean(name = "com.suchtool.nicelimit.niceLimitProperty")
    @ConfigurationProperties(prefix = "suchtool.nicelimit")
    public NiceLimitProperty niceLimitProperty() {
        return new NiceLimitProperty();
    }
}
