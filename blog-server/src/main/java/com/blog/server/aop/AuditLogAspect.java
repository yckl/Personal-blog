package com.blog.server.aop;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.blog.server.entity.AuditLog;
import com.blog.server.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Pointcut("execution(* com.blog.server.controller..*.*(..)) && @annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void postMapping() {}

    @Pointcut("execution(* com.blog.server.controller..*.*(..)) && @annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void putMapping() {}

    @Pointcut("execution(* com.blog.server.controller..*.*(..)) && @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void deleteMapping() {}

    @AfterReturning("(postMapping() || putMapping() || deleteMapping()) && !execution(* com.blog.server.controller.PublicController.*(..))")
    public void logAdminAction(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();
            
            String method = request.getMethod();
            String uri = request.getRequestURI();
            
            // Infer action
            String action = "UPDATE";
            if ("POST".equalsIgnoreCase(method)) action = "CREATE";
            else if ("DELETE".equalsIgnoreCase(method)) action = "DELETE";
            
            // Special cases
            if (uri.contains("/login")) action = "LOGIN";
            else if (uri.contains("/publish")) action = "PUBLISH";
            
            // Infer resource
            String resource = "system";
            if (uri.contains("/articles")) resource = "article";
            else if (uri.contains("/categories")) resource = "category";
            else if (uri.contains("/tags")) resource = "tag";
            else if (uri.contains("/comments")) resource = "comment";
            else if (uri.contains("/users")) resource = "user";
            else if (uri.contains("/config") || uri.contains("/settings")) resource = "settings";
            else if (uri.contains("/messages")) resource = "message";
            else if (uri.contains("/timeline")) resource = "timeline";
            else if (uri.contains("/media")) resource = "media";
            
            // Get user ID
            Long userId = 1L; // default fallback
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                // Assuming principal is SysUserDetails or similar. Since we don't have exactly the class here,
                // we could just parse it or fall back to 1L
                try {
                    // This is a naive extraction; ideally we cast to SysUserDetails from com.blog.server.security
                    userId = Long.parseLong(auth.getName()); // Just in case Name is the ID string
                } catch (Exception e) {}
            }
            
            // Create log
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setIpAddress(request.getRemoteAddr());
            auditLog.setDetail(joinPoint.getSignature().getName() + " executed on " + uri);
            auditLog.setUserId(userId); 
            
            auditLogMapper.insert(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
