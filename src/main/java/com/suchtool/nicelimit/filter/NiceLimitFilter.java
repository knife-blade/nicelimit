package com.suchtool.nicelimit.filter;

import com.suchtool.nicelimit.property.NiceLimitDetailProperty;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import com.suchtool.nicetool.util.base.JsonUtil;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import java.util.List;

public class NiceLimitFilter implements Filter {

    private NiceLimitProperty oldProperty;

    private final NiceLimitProperty newProperty;

    private final RedissonClient redissonClient;

    public NiceLimitFilter(NiceLimitProperty newProperty,
                           RedissonClient redissonClient) {
        this.newProperty = newProperty;
        this.redissonClient = redissonClient;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)  {
        doLimit();
    }

    private void doLimit() {
        if (checkAndInitConfig()) {
            deleteOldRateLimiter();
            createNewRateLimiter();
            oldProperty = newProperty;
        }
    }

    /**
     * @return 配置是否改变
     */
    private boolean checkAndInitConfig() {
        boolean configChanged = false;
        if (oldProperty == null) {
            RBucket<String> bucket = redissonClient.getBucket(newProperty.getConfigKey());
            String configJson = bucket.get();
            if (!StringUtils.hasText(configJson)) {
                configJson = JsonUtil.toJsonString(newProperty);
                bucket.set(configJson);
                oldProperty = JsonUtil.toObject(configJson, NiceLimitProperty.class);
                configChanged = true;
            } else {
                oldProperty = JsonUtil.toObject(configJson, NiceLimitProperty.class);
                configChanged = !newProperty.getVersion().equals(oldProperty.getVersion());
            }
        } else {
            return !newProperty.getVersion().equals(oldProperty.getVersion());
        }

        return configChanged;
    }

    private void deleteOldRateLimiter() {

    }

    private void createNewRateLimiter() {
        List<NiceLimitDetailProperty> detailList = newProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            return;
        }

        for (NiceLimitDetailProperty detailProperty : detailList) {
            redissonClient.getRateLimiter(newProperty + ":" + detailProperty.getUrl());
        }
    }
}
