package com.suchtool.nicelimit.property;

import lombok.Data;

@Data
public class NiceLimitResponseCommonProperty {
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
