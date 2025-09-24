package com.suchtool.nicelimit.dto;

import lombok.Data;

@Data
public class NiceLimitLimitedDTO {
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
