package com.suchtool.nicelimit.property;

import lombok.Data;

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
    private String version;

    /**
     * 被限流的提示信息
     */
    private String limitedMessage;

    /**
     * 被限流的异常
     */
    private Class<? extends RuntimeException> limitedException;

    /**
     * key的前缀
     */
    private String keyPrefix = "rateLimiter";

    /**
     * 详情
     */
    private List<NiceLimitDetailProperty> detail;

}
