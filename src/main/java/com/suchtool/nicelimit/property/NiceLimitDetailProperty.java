package com.suchtool.nicelimit.property;

import lombok.Data;
import org.redisson.api.RateType;

import java.time.Duration;

@Data
public class NiceLimitDetailProperty {
    /**
     * URL（不支持通配符，为了极致的效率）
     */
    private String url;

    /**
     * 速度类型
     */
    private RateType rateType;

    /**
     * 速度间隔（单位时间）
     */
    private Duration rateInterval;

    /**
     * 速度（数量）
     */
    private Long rate;

    /**
     * 被限流的状态码
     */
    private Integer limitedStatusCode;

    /**
     * 被限流的内容类型
     */
    private String limitedContentType;

    /**
     * 被限流的提示信息
     */
    private String limitedMessage;
}
