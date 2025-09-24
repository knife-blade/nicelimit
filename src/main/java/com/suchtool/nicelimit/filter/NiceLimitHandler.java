package com.suchtool.nicelimit.filter;

import com.suchtool.nicelimit.property.NiceLimitDetailProperty;
import com.suchtool.nicelimit.property.NiceLimitProperty;
import com.suchtool.nicetool.util.base.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class NiceLimitHandler {
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

    public NiceLimitHandler(NiceLimitProperty newProperty,
                            RedissonClient redissonClient) {
        this.newProperty = newProperty;
        this.redissonClient = redissonClient;
    }

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

            boolean limitRequired = limitRequired(url);

            if (newProperty.getDebug()) {
                log.info("nicelimit limit required: {}", limitRequired);
            }

            if (limitRequired) {

                Integer limitedStatusCode = newProperty.getLimitedStatusCode();
                String limitedContentType = newProperty.getLimitedContentType();
                String limitedMessage = newProperty.getLimitedMessage();

                NiceLimitDetailProperty detailProperty = detailPropertyMap.get(url);
                if (detailProperty != null) {
                    if (detailProperty.getLimitedStatusCode() != null) {
                        limitedStatusCode = detailProperty.getLimitedStatusCode();
                    }

                    if (StringUtils.hasText(detailProperty.getLimitedContentType())) {
                        limitedContentType = detailProperty.getLimitedContentType();
                    }

                    if (StringUtils.hasText(detailProperty.getLimitedMessage())) {
                        limitedMessage = detailProperty.getLimitedMessage();
                    }
                }

                if (servletResponse instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                    httpServletResponse.setStatus(limitedStatusCode);
                    httpServletResponse.setContentType(limitedContentType);
                    httpServletResponse.getWriter().write(limitedMessage);

                    return true;
                } else {
                    throw new RuntimeException(limitedMessage);
                }
            }
        }

        return false;
    }

    /**
     * @return 是否要限流
     */
    private boolean limitRequired(String url) {
        if (newProperty.getDebug()) {
            log.info("nicelimit check limit required start");
        }

        // 如果是禁止的URL，直接限流
        if (!CollectionUtils.isEmpty(newProperty.getForbidUrl())
                && newProperty.getForbidUrl().contains(url)) {
            if (newProperty.getDebug()) {
                log.info("nicelimit limit is required: url{} is in forbid url", url);
            }
            return true;
        }

        NiceLimitDetailProperty niceLimitDetailProperty = null;
        // 如果没有限流配置，则不限流
        if (!CollectionUtils.isEmpty(detailPropertyMap)) {
            niceLimitDetailProperty = detailPropertyMap.get(url);
            if (niceLimitDetailProperty == null) {
                if (newProperty.getDebug()) {
                    log.info("nicelimit limit is not required: url({} is not in detail", url);
                }
                return false;
            }
        }

        if (niceLimitDetailProperty == null) {
            return false;
        }

        RRateLimiter rateLimiter = rateLimiterMap.get(url);
        if (rateLimiter == null) {
            log.info("nicelimit rate limiter is null, recreate start.url:{}", url);
            rateLimiter = doCreateRateLimiter(niceLimitDetailProperty);
        }

        if (rateLimiter != null) {
            return !rateLimiter.tryAcquire();
        } else {
            // 正常不会到这里，为了保险，在这里不限流
            log.error("nicelimit rate limiter is null, even though recreate");
            return false;
        }
    }

    public void doCheckAndUpdateConfig() {
        if (requireUpdateLocal()) {
            if (newProperty.getDebug()) {
                log.info("nicelimit update local start");
            }
            String newConfigJsonString = JsonUtil.toJsonString(newProperty);
            // 更新本地配置
            updateLocalConfig(newConfigJsonString);

            boolean requireUpdateRemote = false;

            RBucket<String> configBucket = redissonClient.getBucket(newProperty.getConfigKey());
            String remotePropertyJson = configBucket.get();

            if (newProperty.getDebug()) {
                log.info("nicelimit fetch remote config result: {}", remotePropertyJson);
            }

            NiceLimitProperty remoteProperty = null;
            if (!StringUtils.hasText(remotePropertyJson)) {
                if (newProperty.getDebug()) {
                    log.info("nicelimit remote config is blank, update remote is required");
                }
                requireUpdateRemote = true;
            } else {
                remoteProperty = JsonUtil.toObject(remotePropertyJson, NiceLimitProperty.class);

                String newPropertyJsonString = JsonUtil.toJsonString(newProperty);
                String remotePropertyJsonString = JsonUtil.toJsonString(remoteProperty);
                requireUpdateRemote = !DigestUtils.md5DigestAsHex(newPropertyJsonString.getBytes())
                        .equals(DigestUtils.md5DigestAsHex(remotePropertyJsonString.getBytes()));
                if (newProperty.getDebug()) {
                    log.info("nicelimit remote config is different from new config, update remote is required");
                }
            }

            if (newProperty.getDebug()) {
                log.info("nicelimit requireUpdateRemote: {}", requireUpdateRemote);
            }

            // 更新redis的配置
            if (requireUpdateRemote) {
                RLock lock = redissonClient.getLock(newProperty.getUpdateLockKey());
                boolean locked = lock.tryLock();
                if (locked) {
                    try {
                        deleteOldRateLimiter(remoteProperty);
                        configBucket.set(newConfigJsonString);

                        if (newProperty.getDebug()) {
                            log.info("nicelimit update remote config as: {}", newProperty);
                        }
                    } catch (Exception e) {
                        log.error("nicelimit update remote error", e);
                    } finally {
                        lock.unlock();
                    }
                }
            }

            createAndRecordReadRateLimiter();
        }
    }

    /**
     * @return 是否需要更新本地
     */
    private boolean requireUpdateLocal() {
        if (oldProperty == null) {
            return true;
        } else {
            String newPropertyJsonString = JsonUtil.toJsonString(newProperty);
            String oldPropertyJsonString = JsonUtil.toJsonString(oldProperty);

            return !DigestUtils.md5DigestAsHex(newPropertyJsonString.getBytes())
                    .equals(DigestUtils.md5DigestAsHex(oldPropertyJsonString.getBytes()));
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
        if (newProperty.getDebug()) {
            log.info("nicelimit delete old rate limiter start");
        }

        if (remoteProperty == null) {
            if (newProperty.getDebug()) {
                log.info("nicelimit remote property is null, do not delete old rate limiter");
            }
            return;
        }

        List<NiceLimitDetailProperty> detailList = remoteProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            if (newProperty.getDebug()) {
                log.info("nicelimit remote property deteil is empty, do not delete old rate limiter");
            }
            return;
        }

        for (NiceLimitDetailProperty detailProperty : detailList) {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(
                    buildRateLimiterKey(remoteProperty, detailProperty.getUrl()));
            rateLimiter.delete();
            if (newProperty.getDebug()) {
                log.info("nicelimit delete old rate limiter successfully, detail property: {}",
                        JsonUtil.toJsonString(detailProperty)
                );
            }
        }
    }

    private void createAndRecordReadRateLimiter() {
        if (newProperty.getDebug()) {
            log.info("nicelimit create new rate limiter start");
        }

        List<NiceLimitDetailProperty> detailList = newProperty.getDetail();
        if (CollectionUtils.isEmpty(detailList)) {
            if (newProperty.getDebug()) {
                log.info("nicelimit detail property is empty, do not create new rate limiter");
            }
            return;
        }

        rateLimiterMap.clear();

        for (NiceLimitDetailProperty detailProperty : detailList) {
            doCreateRateLimiter(detailProperty);
            if (newProperty.getDebug()) {
                log.info("nicelimit create new rate limiter successfully, detail property: {}",
                        JsonUtil.toJsonString(detailProperty));
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
        // 返回true表示新建，false表示已存在
        boolean createNew = rateLimiter.trySetRate(
                detailProperty.getRateType(),
                detailProperty.getRate(),
                detailProperty.getRateInterval().getSeconds(),
                RateIntervalUnit.SECONDS
                );
        rateLimiterMap.put(detailProperty.getUrl(), rateLimiter);
        return rateLimiter;
    }
}
