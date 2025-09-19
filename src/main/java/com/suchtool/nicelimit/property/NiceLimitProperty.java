package com.suchtool.nicelimit.property;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class NiceLimitProperty {
    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 版本，每次修改配置必须更改
     */
    private String version = "1";

    /**
     * 被限流的提示信息
     */
    private String limitedMessage = "哎呀，访问量好大，请稍后再试试吧~";

    /**
     * 被限流的异常
     */
    private Class<? extends RuntimeException> limitedException;

    /**
     * 配置的key
     */
    private String configKey = "niceLimit:config";

    /**
     * 限流器的key前缀
     */
    private String limiterKeyPrefix = "niceLimit:limiter";

    /**
     * 白名单
     */
    private List<String> whiteUrl;

    /**
     * 详情
     */
    private List<NiceLimitDetailProperty> detail;

    /**
     * 过滤器匹配模式
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
