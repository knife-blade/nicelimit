package com.suchtool.nicelimit.filter;

import com.suchtool.nicelimit.dto.NiceLimitLimitedDTO;
import com.suchtool.nicelimit.handler.NiceLimitHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class NiceLimitFilterJavax implements Filter, Ordered {

    private final NiceLimitHandler niceLimitHandler;

    private final Integer order;

    public NiceLimitFilterJavax(NiceLimitHandler niceLimitHandler,
                                Integer order) {
        this.niceLimitHandler = niceLimitHandler;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws ServletException, IOException {
        try {
            boolean limited = process(servletRequest, servletResponse, filterChain);
            // 如果被限流，直接返回。（process里已经处理了响应）
            if (limited) {
                return;
            }
        } catch (Exception e) {
            log.error("nicelimit filter error", e);
        }

        // 调用filter链中的下一个filter
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean process(ServletRequest servletRequest,
                            ServletResponse servletResponse,
                            FilterChain filterChain) throws IOException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String url = httpServletRequest.getRequestURI();

            NiceLimitLimitedDTO niceLimitLimitedDTO = niceLimitHandler.checkLimit(url);

            if (niceLimitLimitedDTO != null) {
                if (servletResponse instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                    httpServletResponse.setStatus(niceLimitLimitedDTO.getLimitedStatusCode());
                    httpServletResponse.setContentType(niceLimitLimitedDTO.getLimitedContentType());
                    httpServletResponse.getWriter().write(niceLimitLimitedDTO.getLimitedMessage());

                    return true;
                } else {
                    throw new RuntimeException(niceLimitLimitedDTO.getLimitedMessage());
                }
            }
        }

        return false;
    }
}
