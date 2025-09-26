package com.suchtool.nicelimit.property;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NiceLimitForbidProperty extends NiceLimitResponseCommonProperty{
    /**
     * URL（不支持通配符，为了极致的效率）
     */
    private String url;
}
