package com.eneng.suporte.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditInterceptor.class);
    private static final String START_ATTR = "req.start.ns";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        log.debug("-> {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        Long start = (Long) request.getAttribute(START_ATTR);
        long durationMs = start == null ? -1 : (System.nanoTime() - start) / 1_000_000;
        if (ex != null) {
            log.warn("<- {} {} status={} {}ms erro={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), durationMs, ex.getClass().getSimpleName());
        } else {
            log.debug("<- {} {} status={} {}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), durationMs);
        }
    }
}
