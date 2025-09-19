package com.suchtool.nicelimit.filter;

import com.suchtool.nicelimit.property.NiceLimitDetailProperty;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class RateLimiterFilter {
    private NiceLimitProperty oldProperty;

    @Autowired
    private NiceLimitProperty currentProperty;

    private void doCheckRateLimiter() {
        if (configChanged()) {
            deleteOldRateLimiter();
            createNewRateLimiter();
            oldProperty = currentProperty;
        }
    }

    /**
     * @return 配置是否改变
     */
    private boolean configChanged() {
        return oldProperty == null
                || StringUtils.isEmpty(oldProperty.getVersion())
                || !oldProperty.getVersion().equals(currentProperty.getVersion());
    }

    private void deleteOldRateLimiter() {

    }

    private void createNewRateLimiter() {
        List<NiceLimitDetailProperty> detailList = currentProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            return;
        }

        for (NiceLimitDetailProperty detailProperty : detailList) {

        }
    }


}
