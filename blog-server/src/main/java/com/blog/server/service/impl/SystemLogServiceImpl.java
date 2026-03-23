package com.blog.server.service.impl;

import com.blog.server.entity.SystemLog;
import com.blog.server.mapper.SystemLogMapper;
import com.blog.server.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogMapper systemLogMapper;

    @Async
    @Override
    public void logLogin(Long userId, String ip, boolean success, String errorMsg) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setOperation("LOGIN");
        log.setMethod("POST");
        log.setIpAddress(ip);
        log.setStatus(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        systemLogMapper.insert(log);
    }
}
