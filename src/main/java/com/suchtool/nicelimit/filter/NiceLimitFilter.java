package com.suchtool.nicelimit.filter;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Slf4j
public class NiceLimitFilter implements Filter {

    private final NiceLimitHandler niceLimitHandler;

    public NiceLimitFilter(NiceLimitHandler niceLimitHandler) {
        this.niceLimitHandler = niceLimitHandler;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) {
        try {
            niceLimitHandler.doFilter(servletRequest, servletResponse, filterChain);
        } catch (Exception e) {
            log.error("nicelimit filter error", e);
        }
    }
}
