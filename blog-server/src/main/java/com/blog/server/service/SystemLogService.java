package com.blog.server.service;

public interface SystemLogService {

    /**
     * Log a login attempt to the system_log table.
     */
    void logLogin(Long userId, String ip, boolean success, String errorMsg);
}
