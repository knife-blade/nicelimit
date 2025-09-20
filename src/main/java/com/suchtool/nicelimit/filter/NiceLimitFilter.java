package com.suchtool.nicelimit.filter;

import com.suchtool.nicelimit.property.NiceLimitDetailProperty;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import com.suchtool.nicetool.util.base.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class NiceLimitFilter implements Filter, ApplicationListener<EnvironmentChangeEvent> {

    private NiceLimitProperty oldProperty;

    private final NiceLimitProperty newProperty;

    private final RedissonClient redissonClient;

    /**
     * key：url，value：NiceLimitDetailProperty
     */
    private Map<String, NiceLimitDetailProperty> detailPropertyMap;

    /**
     * key：url，value：RRateLimiter
     */
    private final Map<String, RRateLimiter> rateLimiterMap = new HashMap<>();

    public NiceLimitFilter(NiceLimitProperty newProperty,
                           RedissonClient redissonClient) {
        this.newProperty = newProperty;
        this.redissonClient = redissonClient;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) {
        if (Boolean.TRUE.equals(newProperty.getEnabled())) {
            try {
                processDoFilter(servletRequest, servletResponse, filterChain);
            } catch (Exception e) {
                log.error("nicelimit error", e);
            }
        }
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        try {
            Set<String> keys = event.getKeys();
            for (String key : keys) {
                if (key.startsWith("suchtool.nicelimit")) {
                    checkAndUpdateConfig();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("nicelimit event listener error", e);
        }
    }

    private void processDoFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse,
                                 FilterChain filterChain) {
        // todo 填充
    }

    private void checkAndUpdateConfig() {
        if (requireUpdateLocal()) {
            RBucket<String> configBucket = redissonClient.getBucket(newProperty.getConfigKey());
            String remotePropertyJson = configBucket.get();
            NiceLimitProperty remoteProperty = null;
            boolean requireUpdateRemote = false;
            if (!StringUtils.hasText(remotePropertyJson)) {
                requireUpdateRemote = true;
            } else {
                remoteProperty = JsonUtil.toObject(remotePropertyJson, NiceLimitProperty.class);
                requireUpdateRemote = !newProperty.getVersion().equals(remoteProperty.getVersion());
            }

            String newConfigJsonString = JsonUtil.toJsonString(newProperty);

            // 更新本地配置
            updateLocalConfig(newConfigJsonString);

            // 更新redis的配置
            if (requireUpdateRemote) {
                RLock lock = redissonClient.getLock(newProperty.getUpdateLockKey());
                boolean locked = false;
                locked = lock.tryLock();
                if (locked) {
                    try {
                        deleteOldRateLimiter(remoteProperty);
                        createNewRateLimiter();
                        configBucket.set(newConfigJsonString);
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }

    /**
     * @return 是否需要更新本地
     */
    private boolean requireUpdateLocal() {
        if (oldProperty == null) {
            return true;
        } else {
            return !newProperty.getVersion().equals(oldProperty.getVersion());
        }
    }

    private void updateLocalConfig(String newConfigJsonString) {
        oldProperty = JsonUtil.toObject(newConfigJsonString, NiceLimitProperty.class);
        List<NiceLimitDetailProperty> detailList = newProperty.getDetail();
        if (!CollectionUtils.isEmpty(detailList)) {
            detailPropertyMap = detailList.stream()
                    .collect(Collectors.toMap(NiceLimitDetailProperty::getUrl, Function.identity()));
        }
    }

    private void deleteOldRateLimiter(NiceLimitProperty remoteProperty) {
        if (remoteProperty == null) {
            return;
        }

        List<NiceLimitDetailProperty> detailList = remoteProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            return;
        }

        for (NiceLimitDetailProperty detailProperty : detailList) {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(
                    buildRateLimiterKey(remoteProperty, detailProperty.getUrl()));
            rateLimiter.delete();
        }
    }

    private void createNewRateLimiter() {
        List<NiceLimitDetailProperty> detailList = newProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            return;
        }

        for (NiceLimitDetailProperty detailProperty : detailList) {
            RRateLimiter rRateLimiter = doCreateRateLimiter(detailProperty);
            if (rRateLimiter != null) {
                rateLimiterMap.put(detailProperty.getUrl(), rRateLimiter);
            }
        }
    }

    private String buildRateLimiterKey(NiceLimitProperty property,
                                       String url) {
        return property.getLimiterKeyPrefix() + ":" + url;
    }

    private RRateLimiter doCreateRateLimiter(NiceLimitDetailProperty detailProperty) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(
                buildRateLimiterKey(newProperty, detailProperty.getUrl()));
        boolean success = rateLimiter.trySetRate(
                detailProperty.getRateType(),
                detailProperty.getRate(),
                detailProperty.getRateInterval());
        if (success) {
            return rateLimiter;
        } else {
            return null;
        }
    }
}
