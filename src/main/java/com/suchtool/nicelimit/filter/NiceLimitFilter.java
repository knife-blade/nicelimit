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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws ServletException, IOException {
        if (Boolean.TRUE.equals(newProperty.getEnabled())) {
            try {
                boolean limitRequired = processDoFilter(servletRequest, servletResponse, filterChain);
                if (limitRequired) {
                    return;
                }
            } catch (Exception e) {
                log.error("nicelimit error", e);
            }

            // 调用filter链中的下一个filter
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * @return 是否限流
     */
    private boolean processDoFilter(ServletRequest servletRequest,
                                    ServletResponse servletResponse,
                                    FilterChain filterChain) throws IOException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String url = httpServletRequest.getRequestURI();

            boolean limited = limitRequired(url);

            if (limited) {
                if (servletResponse instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                    httpServletResponse.setStatus(newProperty.getLimitedStatusCode());
                    httpServletResponse.setContentType(newProperty.getLimitedContentType());
                    httpServletResponse.getWriter().write(newProperty.getLimitedMessage());
                    return true;
                } else {
                    throw new RuntimeException(newProperty.getLimitedMessage());
                }
            }
        }

        return false;
    }

    /**
     * @return 是否要限流
     */
    private boolean limitRequired(String url) {
        // 如果是禁止的URL，直接限流
        if (!CollectionUtils.isEmpty(newProperty.getForbidUrl())
            && newProperty.getForbidUrl().contains(url)) {
            return true;
        }

        // 如果没有限流配置，则不限流
        NiceLimitDetailProperty niceLimitDetailProperty = detailPropertyMap.get(url);
        if (niceLimitDetailProperty == null) {
            return false;
        }

        RRateLimiter rateLimiter = rateLimiterMap.get(url);
        if (rateLimiter == null) {
            rateLimiter = doCreateRateLimiter(niceLimitDetailProperty);
        }

        if (rateLimiter != null) {
            return rateLimiter.tryAcquire();
        } else {
            // 正常不会到这里，为了保险，在这里不限流
            return false;
        }
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
