package com.blog.server.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter per IP.
 * Limits: 30 requests/minute for auth endpoints, 120 requests/minute for other API endpoints.
 * Stale entries are cleaned up every 5 minutes to prevent memory leaks.
 */
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, RateInfo> rateLimits = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;

    @Override
    public void init(FilterConfig filterConfig) {
        // Schedule cleanup of stale entries every 5 minutes
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleEntries, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(httpRequest);
        int maxRequests = path.contains("/auth/") ? 30 : 120;
        String key = ip + ":" + (path.contains("/auth/") ? "auth" : "api");

        RateInfo info = rateLimits.computeIfAbsent(key, k -> new RateInfo());
        long now = System.currentTimeMillis();

        // Reset window every minute
        if (now - info.windowStart > 60_000) {
            info.count.set(0);
            info.windowStart = now;
        }

        if (info.count.incrementAndGet() > maxRequests) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        rateLimits.entrySet().removeIf(entry -> now - entry.getValue().windowStart > 120_000);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) return xForwarded.split(",")[0].trim();
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isEmpty()) return xReal;
        return request.getRemoteAddr();
    }

    private static class RateInfo {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
