package com.blog.server.aspect;

import com.blog.server.annotation.RateLimit;
import com.blog.server.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    @SuppressWarnings("null")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return point.proceed();
        
        HttpServletRequest request = attributes.getRequest();
        String ip = getIpAddress(request);
        String methodName = point.getSignature().getName();
        String customKey = rateLimit.key().isEmpty() ? methodName : rateLimit.key();
        
        String redisKey = "rate_limit:" + customKey + ":" + ip;
        
        // Fixed window approximation via incr + expire
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.windowSeconds()));
        }
        
        if (count != null && count > rateLimit.maxRequests()) {
            throw new BusinessException(429, "Too Many Requests. System is under heavy load, please slow down.");
        }
        
        return point.proceed();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
