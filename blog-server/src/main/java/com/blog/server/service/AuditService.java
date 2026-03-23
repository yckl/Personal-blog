package com.blog.server.service;

import com.blog.server.entity.AuditLog;
import com.blog.server.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * I3: Audit logging service for tracking admin operations.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    @Async
    public void log(Long userId, String action, String resource, Long resourceId, String detail) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResource(resource);
        log.setResourceId(resourceId);
        log.setDetail(detail != null && detail.length() > 2000 ? detail.substring(0, 2000) : detail);
        log.setIpAddress(getCurrentIp());
        auditLogMapper.insert(log);
    }

    public void log(String action, String resource, Long resourceId, String detail) {
        log(null, action, resource, resourceId, detail);
    }

    private String getCurrentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwarded = request.getHeader("X-Forwarded-For");
                if (xForwarded != null && !xForwarded.isEmpty()) return xForwarded.split(",")[0].trim();
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
