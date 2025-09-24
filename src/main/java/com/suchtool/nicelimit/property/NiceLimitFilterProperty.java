package com.suchtool.nicelimit.property;

import lombok.Data;
import org.redisson.api.RateType;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Data
public class NiceLimitFilterProperty {
    /**
     * 过滤器匹配模式（支持通配符，比如：/*）
     */
    private List<String> filterPattern = Collections.singletonList("/*");

    /**
     * 过滤器名字
     */
    private String filterName = "niceLimitFilter";

    /**
     * 过滤器顺序
     */
    private Integer filterOrder;
}
