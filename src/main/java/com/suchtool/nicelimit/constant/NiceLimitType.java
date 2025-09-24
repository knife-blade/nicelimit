package com.suchtool.nicelimit.constant;

import lombok.Getter;

@Getter
public enum NiceLimitType {
    SERVLET("SERVLET"),
    ;

    private final String description;

    NiceLimitType(String description) {
        this.description = description;
    }
}
