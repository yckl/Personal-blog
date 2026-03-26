package com.blog.server.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.annotation.LogOperation;
import com.blog.server.dto.request.LoginRequest;
import com.blog.server.dto.response.LoginResponse;
import com.blog.server.entity.SysUser;
import com.blog.server.mapper.SysUserMapper;
import com.blog.server.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogAspect {

    private final SystemLogService systemLogService;
    private final SysUserMapper sysUserMapper;

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            if ("LOGIN".equals(logOperation.type())) {
                handleLoginLog(joinPoint, result, error);
            }
            long end = System.currentTimeMillis();
            log.info("Operation [{}] completed in {} ms", logOperation.type(), (end - start));
        }
    }

    private void handleLoginLog(ProceedingJoinPoint joinPoint, Object result, Throwable error) {
        CompletableFuture.runAsync(() -> {
            try {
                Object[] args = joinPoint.getArgs();
                String ip = "0.0.0.0";
                LoginRequest loginReq = null;

                for (Object arg : args) {
                    if (arg instanceof LoginRequest) loginReq = (LoginRequest) arg;
                    if (arg instanceof String && ip.equals("0.0.0.0")) {
                        ip = (String) arg; // IP arg
                    }
                }

                Long userId = null;
                boolean success = (error == null);
                String msg = success ? "Login successful" : (error != null ? error.getMessage() : "Unknown error");

                if (success && result instanceof LoginResponse) {
                    userId = ((LoginResponse) result).getUserId();
                } else if (!success && loginReq != null) {
                    SysUser u = sysUserMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, loginReq.getUsername())
                    );
                    if (u != null) {
                        userId = u.getId();
                    }
                }

                systemLogService.logLogin(userId, ip, success, msg);
            } catch (Exception e) {
                log.error("Failed to execute async login log", e);
            }
        });
    }
}
